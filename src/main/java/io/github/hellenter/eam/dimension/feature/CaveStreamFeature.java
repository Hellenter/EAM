package io.github.hellenter.eam.dimension.feature;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.hellenter.eam.world.structures.village.VillageArea;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A stream or a small river sunk into a cave floor: from where it starts it winds across the floor, never uphill, a
 * step down at a time where the floor drops, so the water spills over each step in a little fall. The channel is cut
 * only into floor blocks with open air above, so it follows the floor and never tunnels into a wall; its bed is lined
 * with {@code bed} where the rock under it is not solid, and where the floor beside it is lower a bank of the floor's own
 * block keeps the water in. It starts on the floor (in the air just above it) and keeps
 * within 16 blocks of its chunk.
 *
 * @param width  half the channel's width: 0 is one block wide, 1 three blocks
 * @param depth  depth in the middle of the channel; the edges are one block shallower
 * @param length how many blocks it runs
 * @param bed    what lines the bed
 */
public record CaveStreamFeature(IntProvider width, IntProvider depth, IntProvider length, BlockState bed) implements Feature {
	public static final MapCodec<CaveStreamFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			IntProviders.codec(0, 3).fieldOf("width").forGetter(CaveStreamFeature::width),
			IntProviders.codec(1, 3).fieldOf("depth").forGetter(CaveStreamFeature::depth),
			IntProviders.codec(4, 48).fieldOf("length").forGetter(CaveStreamFeature::length),
			BlockState.CODEC.fieldOf("bed").forGetter(CaveStreamFeature::bed)
	).apply(i, CaveStreamFeature::new));

	/** How far a stream may reach out of its chunk, its widest channel and bank included. */
	private static final int REACH = 16 - 3 - 1;
	/** How far a step down may drop, and how many times a stream may turn away from a wall before it ends. */
	private static final int MAX_DROP = 3;
	private static final int TURNS = 4;
	/** How far beside a duergar village no stream runs higher than its streets. */
	private static final int VILLAGE_MARGIN = 16;

	@Override
	public MapCodec<CaveStreamFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
		if (!level.getBlockState(origin).isAir() || !isFloor(level, origin.below())) {
			return false;
		}
		int minX = (origin.getX() & ~15) - REACH;
		int maxX = (origin.getX() & ~15) + 15 + REACH;
		int minZ = (origin.getZ() & ~15) - REACH;
		int maxZ = (origin.getZ() & ~15) + 15 + REACH;
		int width = this.width.sample(random);
		int depth = this.depth.sample(random);
		int length = this.length.sample(random);

		// Trace the course: points on the floor, each the surface block the water takes.
		List<BlockPos> course = new ArrayList<>();
		double x = origin.getX() + 0.5;
		double z = origin.getZ() + 0.5;
		int surface = origin.getY() - 1;
		float angle = random.nextFloat() * Mth.TWO_PI;
		int turns = 0;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		course.add(new BlockPos(origin.getX(), surface, origin.getZ()));
		for (int step = 0; step < length * 3 && course.size() < length; step++) {
			angle += (random.nextFloat() - 0.5F) * 0.7F;
			double nx = x + Mth.cos(angle);
			double nz = z + Mth.sin(angle);
			int bx = Mth.floor(nx);
			int bz = Mth.floor(nz);
			if (bx - width < minX || bx + width > maxX || bz - width < minZ || bz + width > maxZ) {
				break;
			}
			int floor = floorBelow(level, pos, bx, surface, bz);
			if (floor == Integer.MIN_VALUE) {
				// A wall or a drop too deep: turn aside, or end here.
				if (++turns > TURNS) {
					break;
				}
				angle += (random.nextBoolean() ? 1 : -1) * Mth.HALF_PI;
				continue;
			}
			x = nx;
			z = nz;
			surface = floor;
			BlockPos last = course.getLast();
			if (last.getX() != bx || last.getZ() != bz) {
				course.add(new BlockPos(bx, surface, bz));
			}
		}
		if (course.size() < 4) {
			return false;
		}
		// Every column of the channel, at its surface height.
		Map<Long, Integer> channel = new HashMap<>();
		for (BlockPos point : course) {
			for (int dx = -width; dx <= width; dx++) {
				for (int dz = -width; dz <= width; dz++) {
					if (Math.sqrt(dx * dx + dz * dz) <= width + 0.5) {
						channel.merge(ChunkPos.pack(point.getX() + dx, point.getZ() + dz), point.getY(), Math::max);
					}
				}
			}
		}
		// A stream may run into a neighbouring chunk that was decorated already: never through a duergar village, and
		// never on a ledge above one, where it would pour over the edge into the streets.
		VillageArea villages = VillageArea.around(level, origin, 2);
		VillageArea overlooked = VillageArea.around(level, origin, VILLAGE_MARGIN);
		if (!overlooked.isEmpty()) {
			for (Map.Entry<Long, Integer> column : channel.entrySet()) {
				BlockPos at = new BlockPos(ChunkPos.getX(column.getKey()), column.getValue(), ChunkPos.getZ(column.getKey()));
				if (villages.touches(at) || overlooked.overlooks(at, 2)) {
					return false;
				}
			}
		}
		for (BlockPos point : course) {
			cut(level, point, width, depth, channel, pos);
		}
		return true;
	}

	/**
	 * The surface block the water takes at a column: the same height as {@code surface} or up to {@link #MAX_DROP} lower,
	 * a floor block with air above, or {@link Integer#MIN_VALUE} when there is none (a wall, a deep drop).
	 */
	private static int floorBelow(WorldGenLevel level, BlockPos.MutableBlockPos pos, int x, int surface, int z) {
		if (!level.getBlockState(pos.set(x, surface + 1, z)).isAir()) {
			return Integer.MIN_VALUE;
		}
		for (int y = surface; y >= surface - MAX_DROP; y--) {
			if (isFloor(level, pos.set(x, y, z))) {
				return y;
			}
			if (!level.getBlockState(pos).isAir()) {
				return Integer.MIN_VALUE;
			}
		}
		return Integer.MIN_VALUE;
	}

	private static boolean isFloor(WorldGenLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return state.isSolid() && state.getFluidState().isEmpty() && !state.is(BlockTags.FEATURES_CANNOT_REPLACE)
				&& !(state.getBlock() instanceof FallingBlock);
	}

	/** Sinks the channel round one point of the course: water down from its surface block, in floor blocks only. */
	private void cut(WorldGenLevel level, BlockPos point, int width, int depth, Map<Long, Integer> channel, BlockPos.MutableBlockPos pos) {
		for (int dx = -width; dx <= width; dx++) {
			for (int dz = -width; dz <= width; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz);
				if (d > width + 0.5) {
					continue;
				}
				int x = point.getX() + dx;
				int z = point.getZ() + dz;
				int top = point.getY();
				// Only a floor block at the course's height, open above: never into a bank or a wall.
				BlockState above = level.getBlockState(pos.set(x, top + 1, z));
				if (!isFloor(level, pos.set(x, top, z)) || above.isSolid() || !above.getFluidState().isEmpty() && !above.is(Blocks.WATER)) {
					continue;
				}
				if (!above.isAir() && !above.is(Blocks.WATER)) {
					level.setBlock(pos.set(x, top + 1, z), Blocks.AIR.defaultBlockState(), 2);
				}
				int deep = d > width - 0.5 && width > 0 ? Math.max(1, depth - 1) : depth;
				BlockState bank = level.getBlockState(pos.set(x, top, z));
				for (int y = top; y > top - deep; y--) {
					BlockState there = level.getBlockState(pos.set(x, y, z));
					if (there.is(BlockTags.FEATURES_CANNOT_REPLACE)) {
						break;
					}
					level.setBlock(pos, Blocks.WATER.defaultBlockState(), 2);
				}
				// A bed that holds: line it where the rock under it is not solid.
				BlockState under = level.getBlockState(pos.set(x, top - deep, z));
				if (!under.isSolid() && !under.is(Blocks.WATER) && !under.is(BlockTags.FEATURES_CANNOT_REPLACE)
						|| under.getBlock() instanceof FallingBlock) {
					level.setBlock(pos, this.bed, 2);
				}
				// Banks: open air beside the water gets a block of the floor round it, so the water stays in its bed, except
				// where the channel itself goes on lower down: there it spills over the step in a little fall.
				for (Direction side : Direction.Plane.HORIZONTAL) {
					pos.set(x, top, z).move(side);
					if (!level.getBlockState(pos).isAir()) {
						continue;
					}
					Integer next = channel.get(ChunkPos.pack(pos.getX(), pos.getZ()));
					if (next != null && next < top) {
						level.scheduleTick(pos.set(x, top, z), Fluids.WATER, 1);
					} else if (next == null) {
						level.setBlock(pos, bank, 2);
					}
				}
			}
		}
	}
}
