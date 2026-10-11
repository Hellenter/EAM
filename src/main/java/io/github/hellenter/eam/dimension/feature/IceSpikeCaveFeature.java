package io.github.hellenter.eam.dimension.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * A big ice stalactite hanging from a cave ceiling: a cone of packed ice round a core of blue ice, with a clear ice
 * tip. Under some of them a shorter stalagmite rises from the floor. The feature starts in the air below the ceiling.
 *
 * @param length          stalactite length
 * @param radius          radius at the ceiling
 * @param stalagmiteChance chance for a stalagmite under it
 * @param search          how far up to look for the ceiling and down for the floor
 */
public record IceSpikeCaveFeature(IntProvider length, IntProvider radius, float stalagmiteChance, int search) implements Feature {
	public static final MapCodec<IceSpikeCaveFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			IntProviders.codec(2, 48).fieldOf("length").forGetter(IceSpikeCaveFeature::length),
			IntProviders.codec(1, 6).fieldOf("radius").forGetter(IceSpikeCaveFeature::radius),
			Codec.floatRange(0.0F, 1.0F).fieldOf("stalagmite_chance").forGetter(IceSpikeCaveFeature::stalagmiteChance),
			Codec.intRange(4, 96).fieldOf("search").forGetter(IceSpikeCaveFeature::search)
	).apply(i, IceSpikeCaveFeature::new));

	@Override
	public MapCodec<IceSpikeCaveFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
		if (!level.getBlockState(origin).isAir()) {
			return false;
		}
		BlockPos.MutableBlockPos pos = origin.mutable();
		int up = 0;
		while (up < this.search && level.getBlockState(pos.move(0, 1, 0)).isAir()) {
			up++;
		}
		if (up >= this.search || !level.getBlockState(pos).isSolid()) {
			return false;
		}
		int ceiling = pos.getY();
		pos.set(origin);
		int down = 0;
		while (down < this.search && level.getBlockState(pos.move(0, -1, 0)).isAir()) {
			down++;
		}
		boolean floorFound = down < this.search && level.getBlockState(pos).isSolid();
		int floor = pos.getY();
		int gap = ceiling - floor - 1;
		if (gap < 4) {
			return false;
		}
		int radius = this.radius.sample(random);
		// A stalactite never reaches further than two thirds of the way to the floor.
		int length = Math.min(this.length.sample(random), gap * 2 / 3);
		if (length < 2) {
			return false;
		}
		cone(level, new BlockPos(origin.getX(), ceiling, origin.getZ()), -1, length, radius);
		if (floorFound && random.nextFloat() < this.stalagmiteChance) {
			int rise = Math.min(Math.max(2, length / 2), gap - length - 1);
			if (rise >= 2) {
				cone(level, new BlockPos(origin.getX(), floor, origin.getZ()), 1, rise, Math.max(1, radius - 1));
			}
		}
		return true;
	}

	/** A cone from {@code base} (a ceiling or floor block) growing {@code length} blocks in direction {@code dy}. */
	private static void cone(WorldGenLevel level, BlockPos base, int dy, int length, int radius) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		// One layer into the rock, so the cone sits in it and not on it.
		for (int step = -1; step < length; step++) {
			double t = Math.max(0.0, step) / (double) length;
			double r = (radius + 0.45) * Math.pow(1.0 - t, 0.85);
			int ri = (int) Math.ceil(r);
			for (int dx = -ri; dx <= ri; dx++) {
				for (int dz = -ri; dz <= ri; dz++) {
					double d = Math.sqrt(dx * dx + dz * dz);
					if (d > r) {
						continue;
					}
					pos.set(base.getX() + dx, base.getY() + dy * (step + 1), base.getZ() + dz);
					BlockState there = level.getBlockState(pos);
					boolean fill = !there.is(BlockTags.FEATURES_CANNOT_REPLACE)
						&& (step < 0 ? there.isSolid() || there.isAir() : there.isAir() || there.canBeReplaced() && there.getFluidState().isEmpty());
					if (!fill) {
						continue;
					}
					BlockState ice;
					if (step >= length - 2) {
						ice = Blocks.ICE.defaultBlockState();
					} else if (d < r * 0.45 && radius >= 2) {
						ice = Blocks.BLUE_ICE.defaultBlockState();
					} else {
						ice = Blocks.PACKED_ICE.defaultBlockState();
					}
					level.setBlock(pos, ice, 2);
				}
			}
		}
	}
}
