package io.github.hellenter.eam.mobs.entity.duergar;

/** Pure rules for the duergar, unit tested. */
public final class DuergarLogic {
	/** Trade experience a duergar needs to reach the next level, as a villager: index = his current level. */
	private static final int[] NEXT_LEVEL_XP = {0, 10, 70, 150, 250};

	private DuergarLogic() {
	}

	/** Whether a duergar at {@code level} with {@code xp} trade experience moves up a level. */
	public static boolean levelsUp(int level, int maxLevel, int xp) {
		return level >= 1 && level < Math.min(maxLevel, NEXT_LEVEL_XP.length) && xp >= NEXT_LEVEL_XP[level];
	}

	/** Renegades spawn only where no light at all falls: no torch, lava or glowing mushroom near, no sky. */
	public static boolean darkEnough(int blockLight, int skyLight) {
		return blockLight == 0 && skyLight == 0;
	}

	/** How many followers a renegade leader brings, from {@code min} to {@code max}, for a roll in [0, 1). */
	public static int followerCount(int min, int max, double roll) {
		int low = Math.min(min, max);
		int high = Math.max(min, max);
		return Math.min(high, low + (int) (roll * (high - low + 1)));
	}

	/**
	 * Bomb damage at {@code distance} blocks from the burst: {@code damage} at the centre, falling off in a straight line
	 * to nothing at {@code radius}.
	 */
	public static float blastDamage(float damage, double distance, double radius) {
		if (radius <= 0.0 || distance >= radius) {
			return 0.0F;
		}
		return (float) (damage * (1.0 - Math.max(0.0, distance) / radius));
	}

	/** How many ticks a lob over {@code distance} blocks flies: farther throws fly longer, within [min, max]. */
	public static int flightTicks(double distance, double ticksPerBlock, int min, int max) {
		return Math.clamp(Math.round(distance * ticksPerBlock), min, max);
	}

	/**
	 * The start speed along one axis that carries a thrown body {@code distance} blocks in {@code ticks} ticks, under
	 * {@code gravity} and a drag of {@code drag} per tick. A projectile slows before it moves ({@code dragFirst});
	 * a living body moves, then slows. Pass gravity 0 for a horizontal axis.
	 */
	public static double launchSpeed(double distance, int ticks, double gravity, double drag, boolean dragFirst) {
		double still = travel(0.0, ticks, gravity, drag, dragFirst);
		double unit = travel(1.0, ticks, gravity, drag, dragFirst) - still;
		return unit <= 1.0E-9 ? 0.0 : (distance - still) / unit;
	}

	/** How far a body starting at {@code speed} goes in {@code ticks} ticks along one axis. */
	static double travel(double speed, int ticks, double gravity, double drag, boolean dragFirst) {
		double v = speed;
		double moved = 0.0;
		for (int i = 0; i < ticks; i++) {
			if (dragFirst) {
				v = (v - gravity) * drag;
				moved += v;
			} else {
				moved += v;
				v = (v - gravity) * drag;
			}
		}
		return moved;
	}
}
