package io.github.hellenter.eam.world.swamp;

import org.jspecify.annotations.Nullable;

/**
 * Where the swampy forest lies. Vanilla picks overworld biomes from six climate noises; the swampy forest instead looks
 * at the same climate map zoomed in {@link #REGION_SCALE} times, so its regions come out about ten times the area of an
 * ordinary biome. Inside a region the real humidity under each spot chooses the part of the swamp: hummocks where it is
 * drier, lakes where it is wettest, forest in between.
 */
public final class SwampyForestLogic {
	/** The region map is the climate map stretched this many times along each axis: 9 times the area. */
	public static final int REGION_SCALE = 3;
	/** Vanilla's forest temperature band (temperature index 2), so the swamp sits among temperate biomes. */
	static final float MIN_TEMPERATURE = -0.15F;
	static final float MAX_TEMPERATURE = 0.2F;
	/** Only the wet half of that band (vanilla humidity index 3 and 4: forests, dark forests, jungles). */
	static final float MIN_HUMIDITY = 0.1F;
	/** Inland on the stretched map too, so a region is not centred on an ocean and left with a thin strip of coast. */
	static final float MIN_CONTINENTALNESS = -0.11F;
	/** Real humidity below this gives hummocks with low trees... */
	static final float HUMMOCKS_BELOW = -0.2F;
	/** ...above this, open lakes; the forest takes what is left in between. */
	static final float LAKES_ABOVE = 0.25F;

	/** How far inside a region (in climate units on the stretched map) the terrain becomes fully flat. */
	static final float FLAT_MARGIN = 0.04F;
	/** Real continentalness of the coast band: the swamp keeps the vanilla shore below it and is fully flat above. */
	static final float COAST_FROM = -0.19F;
	static final float COAST_TO = -0.11F;
	/**
	 * Terrain offset (vanilla's base height: 0 is about y 64, 0.0078 a block) that the swamp flattens toward. Inside a
	 * region the vanilla offset is clamped to +-{@link #OFFSET_LIMIT} and squeezed to {@link #OFFSET_SQUEEZE} of its
	 * distance from this level: hills shrink to a block or two, rivers to shallow water.
	 */
	static final float FLAT_OFFSET = -0.01F;
	static final float OFFSET_LIMIT = 0.3F;
	static final float OFFSET_SQUEEZE = 0.15F;
	/** Vanilla's terrain factor: the higher, the less the 3D noise bends the ground. Flat plains are about 6. */
	static final float MIN_FACTOR = 12.0F;

	public enum Part {
		FOREST, LAKES, HUMMOCKS
	}

	private SwampyForestLogic() {
	}

	/** The block coordinate on the stretched climate map that decides whether {@code block} is in a swampy forest region. */
	public static int regionCoordinate(int block) {
		return Math.floorDiv(block, REGION_SCALE);
	}

	/** Whether the stretched climate at a spot (temperature, humidity, continentalness) makes it swampy forest. */
	public static boolean inRegion(float temperature, float humidity, float continentalness) {
		return temperature >= MIN_TEMPERATURE && temperature <= MAX_TEMPERATURE && humidity >= MIN_HUMIDITY
				&& continentalness >= MIN_CONTINENTALNESS;
	}

	/** The part of the swamp a spot in a region gets, from the real (not stretched) humidity there. */
	public static Part part(float humidity) {
		if (humidity < HUMMOCKS_BELOW) {
			return Part.HUMMOCKS;
		}
		return humidity > LAKES_ABOVE ? Part.LAKES : Part.FOREST;
	}

	/**
	 * How strongly the terrain at a spot is flattened, 0 (vanilla) to 1 (swamp flat): 0 on the region border, growing
	 * to 1 within {@link #FLAT_MARGIN} of every climate limit, and 0 again toward real coasts so oceans stay oceans.
	 */
	public static float flatness(float regionTemperature, float regionHumidity, float regionContinentalness, float continentalness) {
		return ramp(regionTemperature - MIN_TEMPERATURE, FLAT_MARGIN) * ramp(MAX_TEMPERATURE - regionTemperature, FLAT_MARGIN)
				* ramp(regionHumidity - MIN_HUMIDITY, FLAT_MARGIN) * ramp(regionContinentalness - MIN_CONTINENTALNESS, FLAT_MARGIN)
				* ramp(continentalness - COAST_FROM, COAST_TO - COAST_FROM);
	}

	/** The flat swamp's terrain offset for a vanilla offset. */
	public static float flatOffset(float offset) {
		float clamped = Math.clamp(offset, -OFFSET_LIMIT, OFFSET_LIMIT);
		return FLAT_OFFSET + (clamped - FLAT_OFFSET) * OFFSET_SQUEEZE;
	}

	/**
	 * Duckweed coverage stage (0 none, 1 to 4 for about 20, 50, 80 and 100%) for a water spot {@code shoreDistance}
	 * blocks from the nearest bank (1 = touching it). A solid carpet runs along the banks and thins out toward open
	 * water over {@code reach} blocks; past that only a few patches where the noise is high.
	 *
	 * @param noise  smooth noise at the spot, -1 to 1, so mats come in patches
	 * @param jitter random -0.5 to 0.5, so stage borders are ragged
	 */
	public static int duckweedCoverage(int shoreDistance, int reach, float noise, float jitter) {
		if (shoreDistance <= 1) {
			return 4;
		}
		if (shoreDistance > reach) {
			return noise > 0.45F ? Math.clamp(Math.round(1.0F + (noise - 0.45F) * 4.0F + jitter), 0, 2) : 0;
		}
		float fromShore = (shoreDistance - 1) / (float) Math.max(1, reach - 1);
		int stage = Math.round(4.0F - 3.0F * fromShore + noise * 1.2F + jitter);
		return Math.clamp(stage, shoreDistance == 2 ? 3 : 0, 4);
	}

	/** 0 below 0, 1 from {@code width} up, smooth in between. */
	static float ramp(float distance, float width) {
		float t = Math.clamp(distance / width, 0.0F, 1.0F);
		return t * t * (3.0F - 2.0F * t);
	}

	/** {@link #part} if the stretched climate puts the spot in a region, else {@code null}. */
	public static @Nullable Part choose(float regionTemperature, float regionHumidity, float regionContinentalness, float humidity) {
		return inRegion(regionTemperature, regionHumidity, regionContinentalness) ? part(humidity) : null;
	}
}
