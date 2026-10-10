package io.github.hellenter.eam.world.swamp;

import io.github.hellenter.eam.content.EamBlocks;
import io.github.hellenter.eam.content.swamp.BerryShrub;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * A big swamp shrub: a woody post one or two blocks tall with a crown of passable leaves around and over its top,
 * about three blocks across. Some shrubs bear wolfberries.
 *
 * @param berryChance chance that a shrub bears berries
 */
public record BigShrubFeature(float berryChance) implements Feature {
	public static final MapCodec<BigShrubFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.floatRange(0.0F, 1.0F).fieldOf("berry_chance").forGetter(BigShrubFeature::berryChance)
	).apply(i, BigShrubFeature::new));

	@Override
	public MapCodec<BigShrubFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
		if (!level.getBlockState(origin.below()).is(BlockTags.DIRT) || !free(level, origin)) {
			return false;
		}
		int height = random.nextFloat() < 0.3F ? 2 : 1;
		for (int y = 0; y < height; y++) {
			if (!free(level, origin.above(y))) {
				return false;
			}
		}
		for (int y = 0; y < height; y++) {
			level.setBlock(origin.above(y), EamBlocks.BIG_SHRUB_STEM.defaultBlockState(), 2);
		}
		boolean berries = random.nextFloat() < this.berryChance;
		BlockPos top = origin.above(height - 1);
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				boolean corner = dx != 0 && dz != 0;
				if ((dx != 0 || dz != 0) && (!corner || random.nextFloat() < 0.6F)) {
					leaves(level, random, top.offset(dx, 0, dz), berries);
				}
				if (!corner || random.nextFloat() < 0.4F) {
					leaves(level, random, top.offset(dx, 1, dz), berries);
				}
				// A two-block shrub's crown reaches down around the post too.
				if (height == 2 && !corner && (dx != 0 || dz != 0) && random.nextBoolean()) {
					leaves(level, random, origin.offset(dx, 0, dz), berries);
				}
			}
		}
		if (random.nextFloat() < 0.5F) {
			leaves(level, random, top.offset(random.nextBoolean() ? 2 : -2, 0, random.nextInt(3) - 1), berries);
		}
		return true;
	}

	private static void leaves(WorldGenLevel level, RandomSource random, BlockPos pos, boolean berries) {
		if (!free(level, pos)) {
			return;
		}
		BlockState state = EamBlocks.BIG_SHRUB_LEAVES.defaultBlockState();
		if (berries) {
			state = state.setValue(BerryShrub.BERRIES, random.nextFloat() < 0.6F ? BerryShrub.RIPE : BerryShrub.PICKED);
		}
		level.setBlock(pos, state, 2);
	}

	/** Air or a replaceable plant, without water. */
	private static boolean free(WorldGenLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return (state.isAir() || state.canBeReplaced()) && state.getFluidState().isEmpty();
	}
}
