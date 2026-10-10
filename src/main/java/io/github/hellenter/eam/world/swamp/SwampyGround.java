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
 * The overworld's final density with the swamp plain cut into it. Where a column is fully swamp the ground ends at the
 * {@link SwampyHeight} (air above it, solid for {@link SwampyForestLogic#CRUST} blocks below it) and vanilla's density, caves included,
 * takes over under that crust; over the region's rim the two blend by {@link SwampyFlatness}.
 */
public record SwampyGround(DensityFunction vanilla, DensityFunction weight, DensityFunction height) implements DensityFunction {
	public static final MapCodec<SwampyGround> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			DensityFunction.CODEC.fieldOf("vanilla").forGetter(SwampyGround::vanilla),
			DensityFunction.CODEC.fieldOf("weight").forGetter(SwampyGround::weight),
			DensityFunction.CODEC.fieldOf("height").forGetter(SwampyGround::height))
			.apply(i, SwampyGround::new));

	@Override
	public DensitySampler compileSampler(DensityFunction.CompileContext context) {
		DensitySampler vanilla = this.vanilla.compileSampler(context);
		DensitySampler weight = this.weight.compileSampler(context);
		DensitySampler height = this.height.compileSampler(context);
		return new DensitySampler() {
			@Override
			public float sampleValue(SamplerContext samplerContext, int blockX, int blockY, int blockZ) {
				float value = vanilla.sampleValue(samplerContext, blockX, blockY, blockZ);
				float w = weight.sampleValue(samplerContext, blockX, 0, blockZ);
				return w <= 0.0F ? value : SwampyForestLogic.groundDensity(value, w, height.sampleValue(samplerContext, blockX, 0, blockZ), blockY);
			}

			@Override
			public void sampleVolume(SamplerContext samplerContext, DensityBuffer outputBuffer, DensityVolume volume) {
				// Vanilla fills the buffer the fast way; the plain is then cut in column by column.
				vanilla.sampleVolume(samplerContext, outputBuffer, volume);
				for (int z = 0; z < volume.sizeZ(); z++) {
					int blockZ = volume.blockZ(z);
					for (int x = 0; x < volume.sizeX(); x++) {
						int blockX = volume.blockX(x);
						float w = weight.sampleValue(samplerContext, blockX, 0, blockZ);
						if (w <= 0.0F) {
							continue;
						}
						float h = height.sampleValue(samplerContext, blockX, 0, blockZ);
						for (int y = 0; y < volume.sizeY(); y++) {
							int index = volume.indexUnchecked(x, y, z);
							outputBuffer.set(index, SwampyForestLogic.groundDensity(outputBuffer.get(index), w, h, volume.blockY(y)));
						}
					}
				}
			}
		};
	}

	@Override
	public DensityFunction rewriteChildren(DfRewriteRule rule) {
		DensityFunction vanilla = rule.rewrite(this.vanilla);
		DensityFunction weight = rule.rewrite(this.weight);
		DensityFunction height = rule.rewrite(this.height);
		return vanilla == this.vanilla && weight == this.weight && height == this.height ? this : new SwampyGround(vanilla, weight, height);
	}

	@Override
	public Interval range() {
		Interval vanilla = this.vanilla.range();
		return Interval.of(Math.min(vanilla.min(), -1.0F), Math.max(vanilla.max(), 1.0F));
	}

	@Override
	public @DensityFunction.Axes int domainAxes() {
		return AXIS_X | AXIS_Y | AXIS_Z;
	}

	@Override
	public MapCodec<SwampyGround> codec() {
		return CODEC;
	}
}
