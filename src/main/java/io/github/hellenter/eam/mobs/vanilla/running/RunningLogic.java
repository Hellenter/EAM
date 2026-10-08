package io.github.hellenter.eam.mobs.vanilla.running;

/**
 * How much faster a hostile mob moves while it chases a target.
 *
 * <p>A mob's walk input and its acceleration both scale with its movement speed attribute, so on the ground it covers
 * ground in proportion to {@code speed²}. A player's input is fixed and only the speed attribute (0.1 walking) scales
 * the acceleration. A mob with speed {@code s} therefore moves at {@code s² / 0.1} of a walking player's pace: a
 * zombie (0.23) at 53%, a spider (0.3) at 90%.
 */
public final class RunningLogic {
	/** A player's base movement speed attribute while walking. */
	public static final double PLAYER_WALK_SPEED = 0.1;

	private RunningLogic() {
	}

	/** Fraction of a walking player's ground pace a mob with this movement speed attribute reaches. */
	public static double paceVsPlayerWalk(double mobSpeed) {
		return mobSpeed * mobSpeed / PLAYER_WALK_SPEED;
	}

	/**
	 * Multiplier for the mob's movement speed attribute that brings its chase pace up to {@code chasePace} (a fraction
	 * of a walking player's pace). Mobs that are already that fast are left alone, never slowed down.
	 */
	public static double speedMultiplier(double mobSpeed, double chasePace) {
		if (mobSpeed <= 0.0 || chasePace <= 0.0) {
			return 1.0;
		}
		double wanted = Math.sqrt(chasePace * PLAYER_WALK_SPEED);
		return Math.max(1.0, wanted / mobSpeed);
	}
}
