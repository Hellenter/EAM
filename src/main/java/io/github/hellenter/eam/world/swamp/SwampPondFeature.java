package io.github.hellenter.eam.world.swamp;

import io.github.hellenter.eam.Eam;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.material.Fluids;

/**
 * A shallow bog pond dug into flat ground: a few overlapping ellipses, 1 block deep at the rim and up to
 * {@code maxDepth} in the middle, with a mud or clay bottom and muddy banks. Columns that would let the water run out
 * (a lower neighbour, a cave under the bottom) are left dry, so the pond never spills. Some inner columns can stay as
 * grassy hummocks, raised one block above the water.
 *
 * @param radius     radius of each ellipse
 * @param maxDepth   depth in the middle, 1 to 3
 * @param hummocks   chance that an inner column stays a raised hummock
 * @param bottom     block under the water
 * @param bank       block on the dry rim around the pond, placed on half of the rim
 */
public record SwampPondFeature(IntProvider radius, int maxDepth, float hummocks, Holder<BlockStateProvider> bottom,
		Holder<BlockStateProvider> bank) implements Feature {
	public static final MapCodec<SwampPondFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			IntProviders.codec(1, 8).fieldOf("radius").forGetter(SwampPondFeature::radius),
			Codec.intRange(1, 3).fieldOf("max_depth").forGetter(SwampPondFeature::maxDepth),
			Codec.floatRange(0.0F, 1.0F).optionalFieldOf("hummocks", 0.0F).forGetter(SwampPondFeature::hummocks),
			BlockStateProvider.CODEC.fieldOf("bottom").forGetter(SwampPondFeature::bottom),
			BlockStateProvider.CODEC.fieldOf("bank").forGetter(SwampPondFeature::bank)
	).apply(i, SwampPondFeature::new));

	/** Ground a pond may be dug into: grass, dirt, mud, moss, podzol. */
	public static final TagKey<Block> DIGGABLE = TagKey.create(Registries.BLOCK, Eam.id("swamp_pond_diggable"));
	private static final int ELLIPSES = 3;
	private static final int PASSES = 6;

	public SwampPondFeature(IntProvider radius, int maxDepth, float hummocks, BlockStateProvider bottom, BlockStateProvider bank) {
		this(radius, maxDepth, hummocks, Holder.direct(bottom), Holder.direct(bank));
	}

	@Override
	public MapCodec<SwampPondFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
		int surface = origin.getY() - 1;
		if (!level.getBlockState(origin.below()).is(DIGGABLE)) {
			return false;
		}
		Long2IntOpenHashMap depths = shape(random, origin);
		LongSet hummocks = new LongOpenHashSet();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		// Drop columns whose ground is not at the pond's level, then hummocks, then anything that would leak.
		for (long column : new LongArrayList(depths.keySet())) {
			int x = ChunkPos.getX(column);
			int z = ChunkPos.getZ(column);
			if (!level.getBlockState(pos.set(x, surface, z)).is(DIGGABLE) || isGround(level.getBlockState(pos.set(x, surface + 1, z)))) {
				depths.remove(column);
			} else if (depths.get(column) > 1 && random.nextFloat() < this.hummocks) {
				depths.remove(column);
				hummocks.add(column);
			}
		}
		for (int pass = 0; pass < PASSES; pass++) {
			boolean changed = false;
			for (long column : new LongArrayList(depths.keySet())) {
				if (leaks(level, depths, column, surface, pos)) {
					depths.remove(column);
					changed = true;
				}
			}
			if (!changed) {
				break;
			}
		}
		if (depths.size() < 4) {
			return false;
		}
		depths.forEach((column, depth) -> dig(level, random, ChunkPos.getX(column), ChunkPos.getZ(column), surface, depth, pos));
		for (long column : hummocks) {
			raiseHummock(level, ChunkPos.getX(column), ChunkPos.getZ(column), surface, pos);
		}
		muddyBanks(level, random, depths, surface, pos);
		return true;
	}

	/** Columns of a few overlapping ellipses, each with its depth: deepest near the middle, 1 block at the rim. */
	private Long2IntOpenHashMap shape(RandomSource random, BlockPos origin) {
		Long2IntOpenHashMap depths = new Long2IntOpenHashMap();
		for (int e = 0; e < ELLIPSES; e++) {
			int cx = origin.getX() + (e == 0 ? 0 : random.nextInt(7) - 3);
			int cz = origin.getZ() + (e == 0 ? 0 : random.nextInt(7) - 3);
			float rx = this.radius.sample(random) + random.nextFloat() * 0.5F;
			float rz = this.radius.sample(random) + random.nextFloat() * 0.5F;
			for (int dx = (int) -rx; dx <= rx; dx++) {
				for (int dz = (int) -rz; dz <= rz; dz++) {
					float distance = (dx * dx) / (rx * rx) + (dz * dz) / (rz * rz);
					if (distance > 1.0F) {
						continue;
					}
					int depth = Math.clamp(Math.round((1.0F - (float) Math.sqrt(distance)) * this.maxDepth + random.nextFloat() - 0.3F), 1, this.maxDepth);
					depths.mergeInt(ChunkPos.pack(cx + dx, cz + dz), depth, Math::max);
				}
			}
		}
		return depths;
	}

	/** Water would run out sideways into a lower neighbour, or down into a cave below the bottom. */
	private static boolean leaks(WorldGenLevel level, Long2IntOpenHashMap depths, long column, int surface, BlockPos.MutableBlockPos pos) {
		int x = ChunkPos.getX(column);
		int z = ChunkPos.getZ(column);
		int depth = depths.get(column);
		if (!holdsWater(level.getBlockState(pos.set(x, surface - depth, z)))) {
			return true;
		}
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			int nx = x + direction.getStepX();
			int nz = z + direction.getStepZ();
			int neighbourDepth = depths.getOrDefault(ChunkPos.pack(nx, nz), 0);
			for (int y = surface - depth + 1; y <= surface - neighbourDepth; y++) {
				if (!holdsWater(level.getBlockState(pos.set(nx, y, nz)))) {
					return true;
				}
			}
		}
		return false;
	}

	private void dig(WorldGenLevel level, RandomSource random, int x, int z, int surface, int depth, BlockPos.MutableBlockPos pos) {
		// Grass and flowers on top would float on the water.
		for (int y = surface + 1; y <= surface + 2; y++) {
			BlockState above = level.getBlockState(pos.set(x, y, z));
			if (above.isAir() || !above.canBeReplaced() || !above.getFluidState().isEmpty()) {
				break;
			}
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
		}
		for (int y = surface - depth + 1; y <= surface; y++) {
			level.setBlock(pos.set(x, y, z), Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
		}
		pos.set(x, surface - depth, z);
		if (level.getBlockState(pos).is(DIGGABLE)) {
			level.setBlock(pos, this.bottom.value().getState(level, random, pos), Block.UPDATE_CLIENTS);
		}
	}

	/** A grassy tussock sticking one block out of the water. */
	private static void raiseHummock(WorldGenLevel level, int x, int z, int surface, BlockPos.MutableBlockPos pos) {
		BlockState above = level.getBlockState(pos.set(x, surface + 1, z));
		if (above.isAir() || above.canBeReplaced() && above.getFluidState().isEmpty()) {
			level.setBlock(pos.set(x, surface, z), Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
			level.setBlock(pos.set(x, surface + 1, z), Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	private void muddyBanks(WorldGenLevel level, RandomSource random, Long2IntOpenHashMap depths, int surface, BlockPos.MutableBlockPos pos) {
		for (long column : depths.keySet()) {
			for (Direction direction : Direction.Plane.HORIZONTAL) {
				int nx = ChunkPos.getX(column) + direction.getStepX();
				int nz = ChunkPos.getZ(column) + direction.getStepZ();
				if (!depths.containsKey(ChunkPos.pack(nx, nz)) && random.nextBoolean()
						&& level.getBlockState(pos.set(nx, surface, nz)).is(BlockTags.GRASS_BLOCKS)) {
					level.setBlock(pos, this.bank.value().getState(level, random, pos), Block.UPDATE_CLIENTS);
				}
			}
		}
	}

	private static boolean isGround(BlockState state) {
		return !state.isAir() && !state.canBeReplaced() && state.getFluidState().isEmpty();
	}

	/** Solid ground or water keeps the pond's water in. */
	private static boolean holdsWater(BlockState state) {
		return state.getFluidState().is(Fluids.WATER) || state.isSolid() && !state.canBeReplaced();
	}
}
