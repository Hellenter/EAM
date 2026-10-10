package io.github.hellenter.eam.world.swamp;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.Interval;
import net.minecraft.world.level.levelgen.densityfunction.DensityBuffer;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.DensityVolume;
import net.minecraft.world.level.levelgen.densityfunction.DfRewriteRule;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;

/** A 2D density function: the swamp plain's ground height at a column ({@link SwampyForestLogic#surfaceHeight}), as a y. */
public record SwampyHeight(DensityFunction vegetation) implements DensityFunction {
	public static final MapCodec<SwampyHeight> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			DensityFunction.CODEC.fieldOf("vegetation").forGetter(SwampyHeight::vegetation))
			.apply(i, SwampyHeight::new));

	@Override
	public DensitySampler compileSampler(DensityFunction.CompileContext context) {
		DensitySampler vegetation = this.vegetation.compileSampler(context);
		return new DensitySampler() {
			@Override
			public float sampleValue(SamplerContext samplerContext, int blockX, int blockY, int blockZ) {
				return SwampyForestLogic.surfaceHeight(blockX, blockZ, vegetation.sampleValue(samplerContext, blockX, 0, blockZ));
			}

			@Override
			public void sampleVolume(SamplerContext samplerContext, DensityBuffer outputBuffer, DensityVolume volume) {
				DensitySampler.sampleVolumeNaive(samplerContext, outputBuffer, volume, this);
			}
		};
	}

	@Override
	public DensityFunction rewriteChildren(DfRewriteRule rule) {
		DensityFunction vegetation = rule.rewrite(this.vegetation);
		return vegetation == this.vegetation ? this : new SwampyHeight(vegetation);
	}

	@Override
	public Interval range() {
		return Interval.of(SwampyForestLogic.SEA_LEVEL - 0.5F - SwampyForestLogic.LAKE_DEPTH,
				SwampyForestLogic.SEA_LEVEL + 0.5F + SwampyForestLogic.LAND_RISE);
	}

	@Override
	public @DensityFunction.Axes int domainAxes() {
		return AXIS_X | AXIS_Z;
	}

	@Override
	public MapCodec<SwampyHeight> codec() {
		return CODEC;
	}
}
