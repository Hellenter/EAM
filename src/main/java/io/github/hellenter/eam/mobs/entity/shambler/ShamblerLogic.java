package io.github.hellenter.eam.mobs.entity.shambler;

/** The shambler's rules, kept free of game objects so they can be unit tested. */
public final class ShamblerLogic {
	private ShamblerLogic() {
	}

	/** A disguised shambler stands up once a player is within {@code wakeDistance} (squared distance in). */
	public static boolean wakes(double distanceSqr, double wakeDistance) {
		return distanceSqr <= wakeDistance * wakeDistance;
	}

	/**
	 * Whether an awake shambler settles down as a mushroom again: it has had nothing to fight for {@code idleTicks}, is not
	 * burning, and the nearest player (squared distance, or infinity for none) is well beyond the distance that wakes it.
	 */
	public static boolean settles(int idleTicks, int disguiseDelay, boolean burning, double nearestPlayerSqr, double wakeDistance) {
		double clear = wakeDistance * 2.0;
		return !burning && idleTicks >= disguiseDelay && nearestPlayerSqr > clear * clear;
	}

	/** A plain shambler takes more from every blow while it burns; a smoldering one never minds fire. */
	public static float damageTaken(float damage, boolean burning, boolean smoldering, float burningDamage) {
		return burning && !smoldering ? damage * burningDamage : damage;
	}

	/** A plain shambler that catches fire burns long: fire left is raised to at least {@code burnTicks}. */
	public static int fireTicks(int current, int burnTicks) {
		return current > 0 ? Math.max(current, burnTicks) : current;
	}
}
