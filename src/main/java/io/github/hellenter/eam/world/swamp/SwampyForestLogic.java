package io.github.hellenter.eam.world.swamp;

import org.jspecify.annotations.Nullable;

/**
 * Where the swampy forest lies and how its ground is shaped. The world is cut into {@link #CELL}-block cells; a cell
 * whose centre has temperate, inland, low-lying climate holds one swamp, a blob about {@code 2 * RADIUS} blocks across
 * with a wobbly outline, placed at a random spot in the cell. The swamp only spreads over ground vanilla keeps fairly
 * low (its own surface estimate from continentalness, erosion and weirdness), so it never cuts into hills or mountains:
 * its rim runs along their feet, where vanilla's ground is already about as low as the swamp plain. Inside it the real humidity chooses the part of the swamp:
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

		/** Vanilla's rough surface height at a column, from its terrain spline: what the land would be without the swamp. */
		float relief(int x, int z);
	}

	/** Side of a cell, in blocks; each cell holds at most one swamp. */
	static final int CELL = 2048;
	/** Radius of a swamp in blocks, before the edge wobble: about 1000 blocks across. */
	static final int RADIUS = 520;
	/** How far a swamp's centre may sit from its cell's centre, so the swamps do not form a grid. */
	static final int JITTER = 360;
	/** The radius grows and shrinks by up to this share with the humidity under the edge, so the outline is ragged. */
	static final float EDGE_WOBBLE = 0.15F;
	/** Share of the radius over which the ground blends from vanilla to the swamp plain; the biome starts halfway. */
	static final float EDGE_WIDTH = 0.3F;
	/** The outline is pushed in and out by a broad noise by up to this share of the radius, so swamps are not round. */
	static final float SHAPE_WARP = 0.3F;
	static final float SHAPE_SCALE = 380.0F;
	/** Where vanilla's land is at most this high the swamp may lie fully; above the second height it fades out. */
	static final float LOWLAND_FROM = 74.0F;
	static final float LOWLAND_TO = 86.0F;
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
	/** Lakes are this many blocks deep at most (2 over most of their area, 1 at the rim). */
	static final float LAKE_DEPTH = 2.0F;
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
		return region(climate, x, z, 0, LOWLAND_FROM, LOWLAND_TO);
	}

	/**
	 * Whether a column lies in the band of ordinary forest around a swamp: {@link #RIM_WIDTH} blocks past the swamp's
	 * outline, and up the feet of the hills to where vanilla's land reaches {@link #RIM_LOWLAND_TO}, so the swamp meets
	 * forest instead of bare mountain biomes. The swamp itself (weight at least a half) is not part of the rim.
	 */
	public static boolean inRim(Climate climate, int x, int z, float weight) {
		if (inSwamp(weight)) {
			return false;
		}
		return weight > 0.0F || region(climate, x, z, RIM_WIDTH, RIM_LOWLAND_FROM, RIM_LOWLAND_TO) >= 0.5F;
	}

	/** Blocks of ordinary forest between the swamp's outline and the vanilla land around it, on level ground. */
	static final int RIM_WIDTH = 160;
	/** The rim forest climbs hills while vanilla's land there is at most this high, fading out by the second height. */
	static final float RIM_LOWLAND_FROM = 100.0F;
	static final float RIM_LOWLAND_TO = 124.0F;

	/** Which vanilla forest the rim has at a column: mostly plain forest, with birch and dark forest patches. */
	public static RimForest rimForest(int x, int z) {
		float noise = valueNoise(x / RIM_PATCH_SCALE, z / RIM_PATCH_SCALE, 7);
		if (noise > 0.3F) {
			return RimForest.BIRCH;
		}
		return noise < -0.4F ? RimForest.DARK : RimForest.FOREST;
	}

	static final float RIM_PATCH_SCALE = 96.0F;

	public enum RimForest {
		FOREST, BIRCH, DARK
	}

	/**
	 * The middle of the swamp in a cell, where Baba Yaga's hut stands: the column of full swamp (weight at least
	 * {@link #HUT_WEIGHT}) nearest the middle of the swamp's shape, searched on a {@link #HUT_STEP}-block grid out to
	 * {@link #HUT_SEARCH} blocks. Returns {x, z}, or {@code null} when the cell has no swamp or no such column near its middle.
	 */
	public static int @Nullable [] hutSpot(Climate climate, int cellX, int cellZ) {
		int[] centre = centre(climate, cellX, cellZ);
		// The outline is pushed about by a broad noise; the shape's middle sits about that far the other way.
		int middleX = centre[0] - Math.round(RADIUS * SHAPE_WARP * valueNoise(centre[0] / SHAPE_SCALE, centre[1] / SHAPE_SCALE, 5));
		int middleZ = centre[1] - Math.round(RADIUS * SHAPE_WARP * valueNoise(centre[0] / SHAPE_SCALE, centre[1] / SHAPE_SCALE, 6));
		int steps = HUT_SEARCH / HUT_STEP;
		for (int ring = 0; ring <= steps; ring++) {
			int[] best = null;
			long bestDistance = Long.MAX_VALUE;
			for (int i = -ring; i <= ring; i++) {
				for (int j = -ring; j <= ring; j++) {
					if (Math.max(Math.abs(i), Math.abs(j)) != ring) continue;
					int x = middleX + i * HUT_STEP;
					int z = middleZ + j * HUT_STEP;
					long distance = (long) i * i + (long) j * j;
					if (distance >= bestDistance || Math.floorDiv(x, CELL) != cellX || Math.floorDiv(z, CELL) != cellZ) continue;
					if (weight(climate, x, z) >= HUT_WEIGHT) {
						best = new int[] {x, z};
						bestDistance = distance;
					}
				}
			}
			if (best != null) {
				return best;
			}
		}
		return null;
	}

	/** The hut stands where the swamp is full: none of the terrain blending toward the rim. */
	static final float HUT_WEIGHT = 0.95F;
	static final int HUT_STEP = 16;
	static final int HUT_SEARCH = 320;

	/** The point a cell's swamp is laid around, before its outline is warped. */
	private static int[] centre(Climate climate, int cellX, int cellZ) {
		// The world's own temperature at the cell corner seeds the swamp's spot, so it moves with the world seed.
		long hash = mix(cellX * 341873128712L + cellZ * 132897987541L + Float.floatToIntBits(climate.temperature(cellX * CELL, cellZ * CELL)));
		int centreX = cellX * CELL + CELL / 2 + (int) ((hash & 0xFFFF) / 65535.0F * 2 * JITTER) - JITTER;
		int centreZ = cellZ * CELL + CELL / 2 + (int) (((hash >>> 16) & 0xFFFF) / 65535.0F * 2 * JITTER) - JITTER;
		return new int[] {centreX, centreZ};
	}

	/** Side of the cells swamps are laid out in, in blocks. */
	public static int cellSize() {
		return CELL;
	}

	/**
	 * The swamp's shape, 0 to 1, grown outward by {@code extra} blocks and limited to land vanilla keeps below
	 * {@code lowFrom}..{@code lowTo}. The cell's centre always has to suit a swamp, so a rim never comes without one.
	 */
	private static float region(Climate climate, int x, int z, int extra, float lowFrom, float lowTo) {
		int cellX = Math.floorDiv(x, CELL);
		int cellZ = Math.floorDiv(z, CELL);
		int[] centre = centre(climate, cellX, cellZ);
		int centreX = centre[0];
		int centreZ = centre[1];
		float dx = x - centreX + RADIUS * SHAPE_WARP * valueNoise(x / SHAPE_SCALE, z / SHAPE_SCALE, 5);
		float dz = z - centreZ + RADIUS * SHAPE_WARP * valueNoise(x / SHAPE_SCALE, z / SHAPE_SCALE, 6);
		float reach = RADIUS * (1.0F + EDGE_WOBBLE) * (1.0F + EDGE_WIDTH / 2) + extra;
		float distanceSquared = dx * dx + dz * dz;
		if (distanceSquared >= reach * reach) {
			return 0.0F;
		}
		float temperature = climate.temperature(centreX, centreZ);
		if (temperature < MIN_TEMPERATURE || temperature > MAX_TEMPERATURE || climate.continents(centreX, centreZ) < MIN_CONTINENTALNESS
				|| lowland(climate, centreX, centreZ, LOWLAND_FROM, LOWLAND_TO) < 0.5F) {
			return 0.0F;
		}
		float radius = RADIUS * (1.0F + EDGE_WOBBLE * Math.clamp(climate.humidity(x, z) * 2.0F, -1.0F, 1.0F));
		float distance = ((float) Math.sqrt(distanceSquared) - extra) / radius;
		float shape = ramp(1.0F + EDGE_WIDTH / 2 - distance, EDGE_WIDTH);
		if (shape <= 0.0F) {
			return 0.0F;
		}
		return shape * lowland(climate, x, z, lowFrom, lowTo);
	}

	/**
	 * How low vanilla's land is at a column, 0 (hills, mountains, sea) to 1 (lowland inland). The swamp fades out where
	 * this drops, so it stops at the foot of the hills and the shore instead of planing them off.
	 */
	static float lowland(Climate climate, int x, int z, float from, float to) {
		// Read on a coarse grid and blended between its points: vanilla's height estimate can climb 20 blocks within a
		// dozen, and a swamp that let go that fast would leave a wall where it ends.
		int gx = Math.floorDiv(x, LOWLAND_GRID);
		int gz = Math.floorDiv(z, LOWLAND_GRID);
		float fx = smooth((x - gx * LOWLAND_GRID) / (float) LOWLAND_GRID);
		float fz = smooth((z - gz * LOWLAND_GRID) / (float) LOWLAND_GRID);
		float a = lowlandAt(climate, gx, gz, from, to);
		float b = lowlandAt(climate, gx + 1, gz, from, to);
		float c = lowlandAt(climate, gx, gz + 1, from, to);
		float d = lowlandAt(climate, gx + 1, gz + 1, from, to);
		float top = a + (b - a) * fx;
		return top + (c + (d - c) * fx - top) * fz;
	}

	/** Spacing of the grid {@link #lowland} is read on: the shortest distance over which the swamp fades out. */
	static final int LOWLAND_GRID = 128;

	/** Low land that is not sea: the swamp keeps vanilla's shore and fades out over the hills. */
	private static float lowlandAt(Climate climate, int gx, int gz, float from, float to) {
		int x = gx * LOWLAND_GRID;
		int z = gz * LOWLAND_GRID;
		return ramp(to - climate.relief(x, z), to - from)
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
		float s = Math.clamp((waterLine - noise) / 0.08F, 0.0F, 1.0F);
		return SEA_LEVEL - 0.5F - (float) Math.sqrt(s) * LAKE_DEPTH;
	}

	/** Solid ground under the plain's surface, so no cave opens into the swamp floor or a lake bed. */
	static final float CRUST = 10.0F;
	/** Density change per block across the plain's surface; vanilla's terrain is about as steep near the ground. */
	private static final float GROUND_GRADIENT = 0.25F;

	/**
	 * How far above and below the plain the ground may still reach at a partial {@link #weight}, in blocks: 0 inside the
	 * swamp, growing smoothly toward the rim and past the world's height at weight 0. Blending heights instead of densities
	 * keeps the rim a slope: a hill outside is cut down to a rising ceiling rather than sheared off into a wall.
	 */
	static float edgeSpread(float weight) {
		return EDGE_RISE * (1.0F - weight) / (weight + 0.1F);
	}

	/** Blocks the ceiling over the plain rises per unit of lost weight, near the swamp. */
	static final float EDGE_RISE = 40.0F;

	/**
	 * The final density at one block: vanilla's density kept between a ceiling and a floor that close in on the plain's
	 * {@code height} as the column's {@link #weight} grows. Under the floor's {@link #CRUST} vanilla's caves stay.
	 */
	public static float groundDensity(float vanilla, float weight, float height, int y) {
		if (weight <= 0.0F) {
			return vanilla;
		}
		float spread = edgeSpread(weight);
		float ceiling = Math.clamp((height + spread - y) * GROUND_GRADIENT, -1.0F, 1.0F);
		float floor = height - spread;
		float solid = y > floor - CRUST ? Math.clamp((floor - y) * GROUND_GRADIENT, -1.0F, 1.0F) : -1.0F;
		return Math.min(ceiling, Math.max(vanilla, solid));
	}

	/** Noise level below which the ground is a lake: higher (more lakes) where it is wetter. */
	static float lakeLevel(float humidity) {
		return Math.clamp(-0.42F + 0.5F * humidity, -0.6F, -0.08F);
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

	/**
	 * Whether a lake gets duckweed at all: every small pond does (little open water around the spot), a big lake only
	 * where the broad {@code noise} (-1 to 1) is high, so most open lakes stay clear.
	 *
	 * @param openWater share of the water around the spot, out to about a dozen blocks, 0 to 1
	 */
	public static boolean duckweedLake(float openWater, float noise) {
		return openWater < SMALL_POND_WATER || noise > 0.45F;
	}

	/** A spot with less of its surroundings under water than this is in a small pond. */
	static final float SMALL_POND_WATER = 0.3F;

	/** 0 below 0, 1 from {@code width} up, smooth in between. */
	static float ramp(float distance, float width) {
		float t = Math.clamp(distance / width, 0.0F, 1.0F);
		return t * t * (3.0F - 2.0F * t);
	}
}
