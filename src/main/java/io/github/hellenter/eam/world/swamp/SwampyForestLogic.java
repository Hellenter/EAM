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

	/** {@link #part} if the stretched climate puts the spot in a region, else {@code null}. */
	public static @Nullable Part choose(float regionTemperature, float regionHumidity, float regionContinentalness, float humidity) {
		return inRegion(regionTemperature, regionHumidity, regionContinentalness) ? part(humidity) : null;
	}
}
