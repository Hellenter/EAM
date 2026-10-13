package io.github.hellenter.eam.dimension.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * A cave lake dug into the floor, at the lowest floor of its column between {@code minY} and {@code maxY}; then, with
 * {@code cascadeChance} each, up to {@code cascades} more lakes on higher floors beside it, each spilling over its rim
 * so water runs down towards the lake below. Every lake has walls and a bed at least two blocks thick of solid rock, no
 * falling blocks, so it never leaks or drops sand and gravel when it loads. It holds lava wherever its biome is one of
 * {@code lavaBiomes}, otherwise water, which {@link UnderworldFluidsFeature} freezes in the cold biomes. The lake picks
 * its fluid itself because it may reach into a neighbouring chunk that was decorated already.
 *
 * @param radius      lake radius
 * @param depth       depth at the middle
 * @param islandChance chance that a lake of radius 4 or more keeps an island of rock in its middle
 */
public record UnderworldLakeFeature(IntProvider radius, IntProvider depth, float islandChance, float cascadeChance, int cascades,
		int minY, int maxY, TagKey<Biome> lavaBiomes) implements Feature {
	public static final MapCodec<UnderworldLakeFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			IntProviders.codec(2, UnderworldLakeFeature.MAX_RADIUS).fieldOf("radius").forGetter(UnderworldLakeFeature::radius),
			IntProviders.codec(1, 6).fieldOf("depth").forGetter(UnderworldLakeFeature::depth),
			Codec.floatRange(0.0F, 1.0F).fieldOf("island_chance").forGetter(UnderworldLakeFeature::islandChance),
			Codec.floatRange(0.0F, 1.0F).fieldOf("cascade_chance").forGetter(UnderworldLakeFeature::cascadeChance),
			Codec.intRange(0, 4).fieldOf("cascades").forGetter(UnderworldLakeFeature::cascades),
			Codec.INT.fieldOf("min_y").forGetter(UnderworldLakeFeature::minY),
			Codec.INT.fieldOf("max_y").forGetter(UnderworldLakeFeature::maxY),
			TagKey.codec(Registries.BIOME).fieldOf("lava_biomes").forGetter(UnderworldLakeFeature::lavaBiomes)
	).apply(i, UnderworldLakeFeature::new));

	/** How thick the rock round a lake is made. */
	private static final int WALL = 2;
	/**
	 * The widest lake. A feature may change blocks up to 16 out of its chunk, and a lake reaches radius + 1 + {@link #WALL}
	 * from its centre, which lies inside the chunk; cascade lakes beside it shrink their reach to fit.
	 */
	private static final int MAX_RADIUS = 10;
	/** A lake is given up when more than this share of its surface has no ground under it. */
	private static final float MAX_OVERHANG = 0.25F;
	/** How far beside the lake below a cascade lake may stand, centre to centre. */
	private static final int CASCADE_OFFSET = 8;
	/** How much higher than the lake below a cascade lake must be, and at most. */
	private static final int CASCADE_MIN_RISE = 4;
	private static final int CASCADE_MAX_RISE = 40;

	@Override
	public MapCodec<UnderworldLakeFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
		BlockPos floor = lowestFloor(level, origin.getX(), origin.getZ());
		if (floor == null) {
			return false;
		}
		int minX = origin.getX() & ~15;
		int minZ = origin.getZ() & ~15;
		Lake below = lake(level, random, floor, this.radius.sample(random));
		if (below == null) {
			return false;
		}
		for (int i = 0; i < this.cascades && random.nextFloat() < this.cascadeChance; i++) {
			Lake above = null;
			int radius = this.radius.sample(random);
			// How far outside the chunk the cascade lake's centre may be, so it stays inside the area a feature may change.
			int reach = Math.max(0, 16 - radius - 1 - WALL);
			for (int tries = 0; tries < 6 && above == null; tries++) {
				float angle = random.nextFloat() * Mth.TWO_PI;
				int distance = below.radius + 1 + random.nextInt(Math.max(1, CASCADE_OFFSET - below.radius));
				int x = Mth.clamp(below.centre.getX() + Math.round(Mth.cos(angle) * distance), minX - reach, minX + 15 + reach);
				int z = Mth.clamp(below.centre.getZ() + Math.round(Mth.sin(angle) * distance), minZ - reach, minZ + 15 + reach);
				BlockPos higher = floorAbove(level, x, below.centre.getY() + CASCADE_MIN_RISE, below.centre.getY() + CASCADE_MAX_RISE, z);
				if (higher != null) {
					above = lake(level, random, higher, radius);
				}
			}
			if (above == null) {
				break;
			}
			spill(level, above, below, minX, minZ);
			below = above;
		}
		return true;
	}

	private record Lake(BlockPos centre, int radius, BlockState fluid) {
	}

	/** Lava in the hot biomes, water elsewhere. */
	private BlockState fluidAt(WorldGenLevel level, BlockPos pos) {
		return level.getBiome(pos).is(this.lavaBiomes) ? Blocks.LAVA.defaultBlockState() : Blocks.WATER.defaultBlockState();
	}

	private static boolean isFluid(BlockState state) {
		return state.is(Blocks.WATER) || state.is(Blocks.LAVA);
	}

	/** The top block of the lowest ground with air above it in this column, or null. */
	private @Nullable BlockPos lowestFloor(WorldGenLevel level, int x, int z) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, this.minY, z);
		boolean solidBelow = false;
		for (int y = this.minY; y <= this.maxY; y++) {
			pos.setY(y);
			BlockState state = level.getBlockState(pos);
			if (state.isAir()) {
				if (solidBelow) {
					return pos.below().immutable();
				}
			} else {
				solidBelow = state.isSolid();
			}
		}
		return null;
	}

	/** The top block of ground with air above it, found going up from {@code fromY}, or null. */
	private @Nullable BlockPos floorAbove(WorldGenLevel level, int x, int fromY, int toY, int z) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, fromY, z);
		boolean solidBelow = false;
		for (int y = fromY; y <= Math.min(toY, this.maxY); y++) {
			pos.setY(y);
			BlockState state = level.getBlockState(pos);
			if (state.isAir()) {
				if (solidBelow) {
					return pos.below().immutable();
				}
			} else {
				solidBelow = state.isSolid();
			}
		}
		return null;
	}

	/** Digs a lake whose water surface is the ground at {@code surface}, or digs nothing and returns null. */
	private @Nullable Lake lake(WorldGenLevel level, RandomSource random, BlockPos surface, int radius) {
		int depth = this.depth.sample(random);
		boolean island = radius >= 4 && random.nextFloat() < this.islandChance;
		float phase = random.nextFloat() * Mth.TWO_PI;
		int lobes = 2 + random.nextInt(3);

		Set<BlockPos> water = new HashSet<>();
		int surfaceCells = 0;
		int overhang = 0;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz);
				double edge = radius * (0.8 + 0.2 * Math.sin(Math.atan2(dz, dx) * lobes + phase)) + 0.5;
				if (d > edge || island && d < radius * 0.3) {
					continue;
				}
				int cellDepth = Math.max(1, (int) Math.round(depth * (1.0 - (d / edge) * (d / edge)) + 0.4));
				surfaceCells++;
				pos.set(surface.getX() + dx, surface.getY(), surface.getZ() + dz);
				if (!level.getBlockState(pos).isSolid() && !isFluid(level.getBlockState(pos))) {
					overhang++;
				}
				for (int y = 0; y < cellDepth; y++) {
					water.add(new BlockPos(surface.getX() + dx, surface.getY() - y, surface.getZ() + dz));
				}
			}
		}
		if (surfaceCells == 0 || overhang > surfaceCells * MAX_OVERHANG) {
			return null;
		}
		for (BlockPos cell : water) {
			if (level.getBlockState(cell).is(BlockTags.FEATURES_CANNOT_REPLACE)) {
				return null;
			}
		}

		BlockState fluid = fluidAt(level, surface);
		boolean lava = fluid.is(Blocks.LAVA);
		// Walls and bed: whatever round the water is not solid rock becomes stone (basalt round lava), two blocks out.
		for (BlockPos cell : water) {
			for (Direction direction : Direction.values()) {
				if (direction == Direction.UP) {
					continue;
				}
				for (int step = 1; step <= WALL; step++) {
					BlockPos near = cell.relative(direction, step);
					if (water.contains(near)) {
						break;
					}
					BlockState state = level.getBlockState(near);
					if (state.is(BlockTags.FEATURES_CANNOT_REPLACE) || isFluid(state)) {
						continue;
					}
					if (!state.isSolid() || state.getBlock() instanceof FallingBlock) {
						level.setBlock(near, lava ? Blocks.BASALT.defaultBlockState()
								: step == 1 && direction == Direction.DOWN && random.nextInt(3) == 0
								? Blocks.CLAY.defaultBlockState() : Blocks.STONE.defaultBlockState(), 2);
					}
				}
			}
		}
		for (BlockPos cell : water) {
			// Each block takes its own biome's fluid, so a lake never brings water into the lava caves.
			level.setBlock(cell, lava ? fluid : fluidAt(level, cell), 2);
			// Plants and loose blocks resting on the old floor go with it.
			BlockPos above = cell.above();
			if (!water.contains(above)) {
				BlockState over = level.getBlockState(above);
				if (!over.isAir() && (!over.isSolid() || over.getBlock() instanceof FallingBlock)) {
					level.setBlock(above, Blocks.AIR.defaultBlockState(), 2);
				}
			}
		}
		return new Lake(surface, radius, fluid);
	}

	/**
	 * Opens the rim of {@code from} on the side facing {@code to}, so its water runs over the edge and down. The channel
	 * stops where it would read or write outside the chunks around {@code minX}, {@code minZ} that a feature may touch.
	 */
	private static void spill(WorldGenLevel level, Lake from, Lake to, int minX, int minZ) {
		int dx = to.centre.getX() - from.centre.getX();
		int dz = to.centre.getZ() - from.centre.getZ();
		double length = Math.sqrt(dx * dx + dz * dz);
		if (length < 1.0) {
			return;
		}
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		boolean inWater = false;
		int channel = 0;
		for (int step = 1; step <= from.radius + 2 * WALL + 4; step++) {
			pos.set(from.centre.getX() + Math.round(dx / length * step), from.centre.getY(), from.centre.getZ() + Math.round(dz / length * step));
			// The channel and the blocks beside it must stay in the chunk and its direct neighbours.
			if (pos.getX() <= minX - 16 || pos.getX() >= minX + 31 || pos.getZ() <= minZ - 16 || pos.getZ() >= minZ + 31) {
				return;
			}
			BlockState state = level.getBlockState(pos);
			if (isFluid(state)) {
				inWater = true;
				continue;
			}
			if (!inWater || state.is(BlockTags.FEATURES_CANNOT_REPLACE) || channel > WALL) {
				return;
			}
			// Cut a channel through the rim, one source at a time, until it reaches open air to pour into.
			if (!state.isAir()) {
				level.setBlock(pos, from.fluid, 2);
				BlockPos above = pos.above();
				BlockState over = level.getBlockState(above);
				if (!over.isAir() && !over.is(BlockTags.FEATURES_CANNOT_REPLACE)) {
					level.setBlock(above, Blocks.AIR.defaultBlockState(), 2);
				}
			}
			channel++;
			if (state.isAir() || opensOut(level, pos)) {
				if (state.isAir()) {
					level.setBlock(pos, from.fluid, 2);
				}
				level.scheduleTick(pos.immutable(), from.fluid.getFluidState().getType(), 1);
				return;
			}
		}
	}

	private static boolean opensOut(WorldGenLevel level, BlockPos pos) {
		for (Direction direction : Direction.values()) {
			if (direction != Direction.UP && level.getBlockState(pos.relative(direction)).isAir()) {
				return true;
			}
		}
		return false;
	}
}
