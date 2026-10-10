package io.github.hellenter.eam.world.swamp;

import org.jspecify.annotations.Nullable;

/**
 * Where the swampy forest lies and how its ground is shaped. The world is cut into {@link #CELL}-block cells; a cell
 * whose centre has temperate, inland climate holds one swamp, a round region about {@code 2 * RADIUS} blocks across
 * with a ragged edge, placed at a random spot in the cell. Inside it the real humidity chooses the part of the swamp:
 * hummocks where it is drier, lakes where it is wettest, forest in between. The ground there is not vanilla's: it is a
 * low plain a few blocks above the water ({@link #surfaceHeight}) with lakes 2 to 3 blocks deep, blended into the
 * vanilla terrain over the region's rim ({@link #weight}).
 */
public final class SwampyForestLogic {
	/** The climate a swamp reads: the overworld's own temperature, humidity and continentalness noises at a block. */
	public interface Climate {
		float temperature(int x, int z);

		float humidity(int x, int z);

		float continents(int x, int z);
	}

	/** Side of a cell, in blocks; each cell holds at most one swamp. */
	static final int CELL = 2048;
	/** Radius of a swamp in blocks, before the edge wobble: about 1000 blocks across. */
	static final int RADIUS = 500;
	/** How far a swamp's centre may sit from its cell's centre, so the swamps do not form a grid. */
	static final int JITTER = 360;
	/** The radius grows and shrinks by up to this share with the humidity under the edge, so the outline is ragged. */
	static final float EDGE_WOBBLE = 0.15F;
	/** Share of the radius over which the ground blends from vanilla to the swamp plain; the biome starts halfway. */
	static final float EDGE_WIDTH = 0.18F;
	/** The swamp's centre must be neither snowy nor desert hot (vanilla's temperature bands 1 to 3)... */
	static final float MIN_TEMPERATURE = -0.45F;
	static final float MAX_TEMPERATURE = 0.55F;
	/** ...and well inland (vanilla's mid-inland band), so the sea does not eat most of the swamp. */
	static final float MIN_CONTINENTALNESS = 0.03F;
	/** Real continentalness of the coast band: the swamp keeps the vanilla shore below it and is fully flat above. */
	static final float COAST_FROM = -0.19F;
	static final float COAST_TO = -0.11F;
	/** Real humidity below this gives hummocks with low trees... */
	static final float HUMMOCKS_BELOW = -0.2F;
	/** ...above this, open lakes; the forest takes what is left in between. */
	static final float LAKES_ABOVE = 0.25F;

	/** Sea level: water fills everything below it, so the water's surface is the block under it. */
	static final int SEA_LEVEL = 63;
	/** The lowest dry ground is the first block above the water; the highest is this many blocks higher. */
	static final float LAND_RISE = 5.0F;
	/** Lakes are this many blocks deep at most (2 to 3 over most of their area, shallower at the rim). */
	static final float LAKE_DEPTH = 3.0F;
	/** Ground noise sizes: broad lakes and low rises, and smaller bumps on top. */
	static final float BROAD_SCALE = 44.0F;
	static final float SMALL_SCALE = 13.0F;

	public enum Part {
		FOREST, LAKES, HUMMOCKS
	}

	private SwampyForestLogic() {
	}

	/**
	 * How strongly a column belongs to a swamp, 0 (vanilla) to 1 (swamp plain). The swamp biomes cover what is at least
	 * half; the terrain blends in over the outer part.
	 */
	public static float weight(Climate climate, int x, int z) {
		int cellX = Math.floorDiv(x, CELL);
		int cellZ = Math.floorDiv(z, CELL);
		// The world's own temperature at the cell corner seeds the swamp's spot, so it moves with the world seed.
		long hash = mix(cellX * 341873128712L + cellZ * 132897987541L + Float.floatToIntBits(climate.temperature(cellX * CELL, cellZ * CELL)));
		int centreX = cellX * CELL + CELL / 2 + (int) ((hash & 0xFFFF) / 65535.0F * 2 * JITTER) - JITTER;
		int centreZ = cellZ * CELL + CELL / 2 + (int) (((hash >>> 16) & 0xFFFF) / 65535.0F * 2 * JITTER) - JITTER;
		float dx = x - centreX;
		float dz = z - centreZ;
		float reach = RADIUS * (1.0F + EDGE_WOBBLE) * (1.0F + EDGE_WIDTH / 2);
		float distanceSquared = dx * dx + dz * dz;
		if (distanceSquared >= reach * reach) {
			return 0.0F;
		}
		float temperature = climate.temperature(centreX, centreZ);
		if (temperature < MIN_TEMPERATURE || temperature > MAX_TEMPERATURE || climate.continents(centreX, centreZ) < MIN_CONTINENTALNESS) {
			return 0.0F;
		}
		float radius = RADIUS * (1.0F + EDGE_WOBBLE * Math.clamp(climate.humidity(x, z) * 2.0F, -1.0F, 1.0F));
		float distance = (float) Math.sqrt(distanceSquared) / radius;
		return ramp(1.0F + EDGE_WIDTH / 2 - distance, EDGE_WIDTH)
				* ramp(climate.continents(x, z) - COAST_FROM, COAST_TO - COAST_FROM);
	}

	/** Whether a column with this {@link #weight} gets a swamp biome. */
	public static boolean inSwamp(float weight) {
		return weight >= 0.5F;
	}

	/** The part of the swamp a spot gets, from the real humidity there. */
	public static Part part(float humidity) {
		if (humidity < HUMMOCKS_BELOW) {
			return Part.HUMMOCKS;
		}
		return humidity > LAKES_ABOVE ? Part.LAKES : Part.FOREST;
	}

	/** The swamp part of a column, or {@code null} outside every swamp. */
	public static @Nullable Part choose(Climate climate, int x, int z) {
		return inSwamp(weight(climate, x, z)) ? part(climate.humidity(x, z)) : null;
	}

	/**
	 * Height of the swamp plain at a column: the ground is solid below this y. Dry land runs from the first block above
	 * the water to {@link #LAND_RISE} blocks higher, mostly low; where the noise dips below a humidity-dependent level
	 * there is a lake up to {@link #LAKE_DEPTH} deep. The wetter the spot, the more of it is lake.
	 */
	public static float surfaceHeight(int x, int z, float humidity) {
		float noise = valueNoise(x / BROAD_SCALE, z / BROAD_SCALE, 0) * 0.72F + valueNoise(x / SMALL_SCALE, z / SMALL_SCALE, 1) * 0.28F;
		float waterLine = lakeLevel(humidity);
		if (noise >= waterLine) {
			float t = (noise - waterLine) / (1.0F - waterLine);
			return SEA_LEVEL + 0.5F + (float) Math.pow(t, 1.4) * LAND_RISE;
		}
		float s = Math.clamp((waterLine - noise) / 0.12F, 0.0F, 1.0F);
		return SEA_LEVEL - 0.5F - (float) Math.sqrt(s) * LAKE_DEPTH;
	}

	/** Solid ground under the plain's surface, so no cave opens into the swamp floor or a lake bed. */
	static final float CRUST = 10.0F;
	/** Density change per block across the plain's surface; vanilla's terrain is about as steep near the ground. */
	private static final float GROUND_GRADIENT = 0.25F;

	/**
	 * The final density at one block: vanilla's density there blended by the column's {@link #weight} toward the plain,
	 * which is solid below {@code height}, air above it, and vanilla again under its {@link #CRUST}.
	 */
	public static float groundDensity(float vanilla, float weight, float height, int y) {
		if (weight <= 0.0F) {
			return vanilla;
		}
		float plain = Math.clamp((height - y) * GROUND_GRADIENT, -1.0F, 1.0F);
		float crust = y > height - CRUST ? 1.0F : -1.0F;
		float swamp = Math.min(plain, Math.max(vanilla, crust));
		return vanilla + (swamp - vanilla) * weight;
	}

	/** Noise level below which the ground is a lake: higher (more lakes) where it is wetter. */
	static float lakeLevel(float humidity) {
		return Math.clamp(-0.28F + 0.6F * humidity, -0.5F, 0.1F);
	}

	/** Smooth value noise, -1 to 1, on a unit lattice. */
	static float valueNoise(float x, float z, int salt) {
		int x0 = (int) Math.floor(x);
		int z0 = (int) Math.floor(z);
		float fx = smooth(x - x0);
		float fz = smooth(z - z0);
		float a = lattice(x0, z0, salt);
		float b = lattice(x0 + 1, z0, salt);
		float c = lattice(x0, z0 + 1, salt);
		float d = lattice(x0 + 1, z0 + 1, salt);
		float top = a + (b - a) * fx;
		float bottom = c + (d - c) * fx;
		return top + (bottom - top) * fz;
	}

	private static float smooth(float t) {
		return t * t * (3.0F - 2.0F * t);
	}

	private static float lattice(int x, int z, int salt) {
		long hash = mix(x * 0x9E3779B97F4A7C15L + z * 0xC2B2AE3D27D4EB4FL + salt * 0x165667B19E3779F9L);
		return (hash >>> 40) / (float) (1L << 23) - 1.0F;
	}

	private static long mix(long value) {
		long h = value * 0x9E3779B97F4A7C15L;
		h = (h ^ (h >>> 32)) * 0xD6E8FEB86659FD93L;
		return h ^ (h >>> 32);
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
}
