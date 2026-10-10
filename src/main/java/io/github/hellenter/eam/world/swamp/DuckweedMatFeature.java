package io.github.hellenter.eam.world.swamp;

import io.github.hellenter.eam.content.EamBlocks;
import io.github.hellenter.eam.content.swamp.DuckweedBlock;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/**
 * Covers the still water of one chunk with duckweed: a solid mat along every bank that thins out stage by stage
 * toward open water ({@link SwampyForestLogic#duckweedCoverage}). Placed once per chunk at the chunk's corner.
 *
 * @param reach how many blocks out from the bank the mat reaches, 2 to 8
 */
public record DuckweedMatFeature(int reach) implements Feature {
	public static final MapCodec<DuckweedMatFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.intRange(2, 8).fieldOf("reach").forGetter(DuckweedMatFeature::reach)
	).apply(i, DuckweedMatFeature::new));
	/** Mats come in patches a dozen blocks across. */
	private static final double NOISE_SCALE = 0.09;

	@Override
	public MapCodec<DuckweedMatFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
		int minX = origin.getX() & ~15;
		int minZ = origin.getZ() & ~15;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		boolean placed = false;
		for (int x = minX; x < minX + 16; x++) {
			for (int z = minZ; z < minZ + 16; z++) {
				int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
				pos.set(x, y, z);
				if (!level.getBlockState(pos).isAir() || !isStillWater(level.getFluidState(pos.setY(y - 1)))
						|| !level.getBiome(pos).is(SwampyForestBiomes.IS_SWAMPY_FOREST)) {
					continue;
				}
				float noise = Biome.BIOME_INFO_NOISE.get(x * NOISE_SCALE, z * NOISE_SCALE);
				int stage = SwampyForestLogic.duckweedCoverage(shoreDistance(level, x, y - 1, z), this.reach, noise, random.nextFloat() - 0.5F);
				if (stage > 0) {
					level.setBlock(pos.setY(y), EamBlocks.DUCKWEED.defaultBlockState().setValue(DuckweedBlock.COVERAGE, stage), 2);
					placed = true;
				}
			}
		}
		return placed;
	}

	/** Distance (in square rings) from a water surface block to the nearest block of that level that is not water. */
	private int shoreDistance(WorldGenLevel level, int x, int y, int z) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int ring = 1; ring <= this.reach; ring++) {
			for (int dx = -ring; dx <= ring; dx++) {
				for (int dz = -ring; dz <= ring; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
						continue;
					}
					BlockState state = level.getBlockState(pos.set(x + dx, y, z + dz));
					if (!state.is(Blocks.WATER) && !state.getFluidState().is(Fluids.WATER)) {
						return ring;
					}
				}
			}
		}
		return this.reach + 1;
	}

	private static boolean isStillWater(FluidState fluid) {
		return fluid.is(Fluids.WATER) && fluid.isSource();
	}
}
