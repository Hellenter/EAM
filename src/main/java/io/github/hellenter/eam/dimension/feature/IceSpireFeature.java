package io.github.hellenter.eam.dimension.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * An ice spike rising from a cave floor, like those of the ice spikes biome: a tapering spire of packed ice with blue
 * ice through its core. A {@code giantChance} of them are giants; a spire as tall as the cave becomes a column that
 * widens again into the ceiling. It starts on the floor (in the air just above it).
 *
 * @param height      spire height
 * @param radius      radius at the base
 * @param giantHeight height of a giant spire
 * @param giantRadius base radius of a giant
 */
public record IceSpireFeature(IntProvider height, IntProvider radius, IntProvider giantHeight, IntProvider giantRadius, float giantChance)
		implements Feature {
	public static final MapCodec<IceSpireFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			IntProviders.codec(3, 64).fieldOf("height").forGetter(IceSpireFeature::height),
			IntProviders.codec(1, 6).fieldOf("radius").forGetter(IceSpireFeature::radius),
			IntProviders.codec(8, 128).fieldOf("giant_height").forGetter(IceSpireFeature::giantHeight),
			IntProviders.codec(2, 8).fieldOf("giant_radius").forGetter(IceSpireFeature::giantRadius),
			Codec.floatRange(0.0F, 1.0F).fieldOf("giant_chance").forGetter(IceSpireFeature::giantChance)
	).apply(i, IceSpireFeature::new));

	@Override
	public MapCodec<IceSpireFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
		if (!level.getBlockState(origin).isAir() || !level.getBlockState(origin.below()).isFaceSturdy(level, origin.below(), Direction.UP)) {
			return false;
		}
		boolean giant = random.nextFloat() < this.giantChance;
		int height = (giant ? this.giantHeight : this.height).sample(random);
		double radius = (giant ? this.giantRadius : this.radius).sample(random) + random.nextDouble() * 0.6;
		int clearance = 0;
		while (clearance <= height && level.getBlockState(origin.above(clearance)).isAir() && !level.isOutsideBuildHeight(origin.above(clearance))) {
			clearance++;
		}
		// Reaching the ceiling: a column, thin in the middle and widening into the ceiling.
		boolean column = clearance <= height;
		if (column) {
			height = clearance;
		}
		if (height < 3) {
			return false;
		}
		double leanX = (random.nextDouble() - 0.5) * 0.08;
		double leanZ = (random.nextDouble() - 0.5) * 0.08;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		// Two blocks down into the floor, so it never floats on a slope.
		for (int y = -2; y < height; y++) {
			double t = Math.max(0.0, y) / height;
			double r = column ? radius * (0.45 + 0.55 * Math.pow(Math.abs(2.0 * t - 1.0), 1.6)) : radius * Math.pow(1.0 - t, 1.3) + 0.35;
			double cx = origin.getX() + 0.5 + leanX * y;
			double cz = origin.getZ() + 0.5 + leanZ * y;
			int reach = (int) Math.ceil(r) + 1;
			for (int dx = -reach; dx <= reach; dx++) {
				for (int dz = -reach; dz <= reach; dz++) {
					int x = origin.getX() + dx;
					int z = origin.getZ() + dz;
					double ox = x + 0.5 - cx;
					double oz = z + 0.5 - cz;
					double d = Math.sqrt(ox * ox + oz * oz);
					if (d > r) {
						continue;
					}
					pos.set(x, origin.getY() + y, z);
					BlockState there = level.getBlockState(pos);
					if (!there.isAir() && !there.canBeReplaced() && !there.is(Blocks.SNOW_BLOCK) && !there.is(Blocks.POWDER_SNOW)) {
						continue;
					}
					boolean core = d < r * 0.4 && random.nextFloat() < 0.7F;
					level.setBlock(pos, core ? Blocks.BLUE_ICE.defaultBlockState() : Blocks.PACKED_ICE.defaultBlockState(), 2);
				}
			}
		}
		return true;
	}
}
