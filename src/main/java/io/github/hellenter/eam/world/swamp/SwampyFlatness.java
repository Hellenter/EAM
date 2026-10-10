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

/**
 * A 2D density function: how strongly a column belongs to a swamp ({@link SwampyForestLogic#weight}), 0 to 1. It reads
 * the overworld's own climate functions, like the biome choice does, so the flat ground lines up with the swamp biomes.
 */
public record SwampyFlatness(DensityFunction temperature, DensityFunction vegetation, DensityFunction continents) implements DensityFunction {
	public static final MapCodec<SwampyFlatness> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			DensityFunction.CODEC.fieldOf("temperature").forGetter(SwampyFlatness::temperature),
			DensityFunction.CODEC.fieldOf("vegetation").forGetter(SwampyFlatness::vegetation),
			DensityFunction.CODEC.fieldOf("continents").forGetter(SwampyFlatness::continents))
			.apply(i, SwampyFlatness::new));

	@Override
	public DensitySampler compileSampler(DensityFunction.CompileContext context) {
		return new Sampler(this.temperature.compileSampler(context), this.vegetation.compileSampler(context),
				this.continents.compileSampler(context));
	}

	@Override
	public DensityFunction rewriteChildren(DfRewriteRule rule) {
		DensityFunction temperature = rule.rewrite(this.temperature);
		DensityFunction vegetation = rule.rewrite(this.vegetation);
		DensityFunction continents = rule.rewrite(this.continents);
		return temperature == this.temperature && vegetation == this.vegetation && continents == this.continents
				? this : new SwampyFlatness(temperature, vegetation, continents);
	}

	@Override
	public Interval range() {
		return Interval.of(0.0F, 1.0F);
	}

	@Override
	public @DensityFunction.Axes int domainAxes() {
		return AXIS_X | AXIS_Z;
	}

	@Override
	public MapCodec<SwampyFlatness> codec() {
		return CODEC;
	}

	private record Sampler(DensitySampler temperature, DensitySampler vegetation, DensitySampler continents) implements DensitySampler {
		@Override
		public float sampleValue(SamplerContext context, int blockX, int blockY, int blockZ) {
			return SwampyForestLogic.weight(new SwampyForestLogic.Climate() {
				@Override
				public float temperature(int x, int z) {
					return Sampler.this.temperature.sampleValue(context, x, 0, z);
				}

				@Override
				public float humidity(int x, int z) {
					return Sampler.this.vegetation.sampleValue(context, x, 0, z);
				}

				@Override
				public float continents(int x, int z) {
					return Sampler.this.continents.sampleValue(context, x, 0, z);
				}
			}, blockX, blockZ);
		}

		@Override
		public void sampleVolume(SamplerContext context, DensityBuffer outputBuffer, DensityVolume volume) {
			DensitySampler.sampleVolumeNaive(context, outputBuffer, volume, this);
		}
	}
}
