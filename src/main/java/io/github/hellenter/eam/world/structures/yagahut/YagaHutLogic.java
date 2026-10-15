package io.github.hellenter.eam.world.structures.yagahut;

import java.util.Arrays;

/** Where Baba Yaga's hut stands and how the ground around it is levelled. */
public final class YagaHutLogic {
	/** Within this many blocks of the hut's middle the ground is level with its floor and every lake is drained. */
	public static final int FLAT_RADIUS = 32;
	/** Past the level ground the land slopes back to its own height over this many blocks. */
	public static final int BLEND = 8;
	/** Earth is filled at most this deep under the levelled ground. */
	public static final int FILL_DEPTH = 16;

	private YagaHutLogic() {
	}

	/**
	 * The y of the hut's ground layer: the middle of the ground heights sampled over its footprint, kept at least at
	 * {@code seaLevel} so the hut stands dry above the swamp's water (which fills up to the block under sea level).
	 */
	public static int groundLevel(int[] groundTops, int seaLevel) {
		int[] sorted = groundTops.clone();
		Arrays.sort(sorted);
		return Math.max(seaLevel, sorted[sorted.length / 2]);
	}

	/** Distance of a column from the hut's middle, in blocks. */
	public static double distance(int middleX, int middleZ, int x, int z) {
		double dx = x - middleX;
		double dz = z - middleZ;
		return Math.sqrt(dx * dx + dz * dz);
	}

	/** Whether a column is levelled at all: within the flat ground or the slope around it. */
	public static boolean inReach(double distance) {
		return distance <= FLAT_RADIUS + BLEND;
	}

	/**
	 * The y of the top ground block a column gets: the hut's ground level within {@link #FLAT_RADIUS}, then easing back to
	 * the column's own ground {@code ownTop} over {@link #BLEND} blocks, so hills are cut and dips filled into a slope.
	 */
	public static int targetTop(int ground, int ownTop, double distance) {
		if (distance <= FLAT_RADIUS) {
			return ground;
		}
		double t = Math.clamp((distance - FLAT_RADIUS) / BLEND, 0.0, 1.0);
		t = t * t * (3.0 - 2.0 * t);
		return (int) Math.round(ground + (ownTop - ground) * t);
	}

	/**
	 * Whether a column under water keeps its water: only on the slope, where the eased ground stays below the water's
	 * surface (the block under sea level), so lakes past the level ground keep their shore.
	 */
	public static boolean keepsWater(double distance, int target, int seaLevel) {
		return distance > FLAT_RADIUS && target < seaLevel;
	}
}
