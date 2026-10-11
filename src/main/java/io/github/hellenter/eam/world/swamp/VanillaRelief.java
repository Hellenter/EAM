package io.github.hellenter.eam.world.swamp;

import net.minecraft.data.worldgen.TerrainProvider;
import net.minecraft.util.BoundedFloatFunction;
import net.minecraft.util.CubicSpline;
import net.minecraft.util.Interval;

/**
 * Vanilla's rough overworld surface height from its climate values alone: the terrain offset spline that shapes the
 * continents, evaluated without the 3D noise on top. Good to a few blocks on plains, so it tells hills and mountains from
 * lowland before any terrain is generated.
 */
public final class VanillaRelief {
	/** The spline reads continentalness, erosion and folded weirdness (peaks and valleys) from a three-float point. */
	private record Coordinate(int index) implements BoundedFloatFunction<float[]> {
		@Override
		public float apply(float[] point) {
			return point[this.index];
		}

		@Override
		public Interval range() {
			return Interval.of(-1.0F, 1.0F);
		}
	}

	private static final CubicSpline<Coordinate> OFFSET = TerrainProvider.overworldOffset(new Coordinate(0), new Coordinate(1),
			new Coordinate(2), false);

	private VanillaRelief() {
	}

	/** Surface y vanilla aims for: the offset spline is 0 at sea level and 1 per 128 blocks up. */
	public static float height(float continents, float erosion, float weirdness) {
		float offset = CubicSpline.sample(OFFSET, new float[] {continents, erosion, TerrainProvider.peaksAndValleys(weirdness)});
		return 63.5F + offset * 128.0F;
	}
}
