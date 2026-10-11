package io.github.hellenter.eam.mobs.entity.bat;

/** Pure vampire bat rules, kept apart from the entity so they can be unit tested. */
public final class VampireBatLogic {
	private VampireBatLogic() {
	}

	/** Whether a player gets a flock at this check: rolled against the chance while few bats are around. */
	public static boolean flockDue(int batsNearby, int maxNearby, float roll, float chance) {
		return batsNearby < maxNearby && roll < chance;
	}

	/** Flock size from a uniform roll in [0, 1): {@code min} to {@code max} bats, both included. */
	public static int flockSize(int min, int max, float roll) {
		int low = Math.min(min, max);
		int high = Math.max(min, max);
		return low + Math.min((int) (roll * (high - low + 1)), high - low);
	}

	/** Distance of a flock from the player for a uniform roll in [0, 1). */
	public static double flockDistance(double min, double max, float roll) {
		double low = Math.min(min, max);
		return low + (Math.max(min, max) - low) * roll;
	}

	/**
	 * Velocity along one axis for the next tick: it eases toward {@code speed} in the direction of the target, as
	 * vanilla bats do, which gives their jerky, fluttering flight.
	 */
	public static double steer(double velocity, double toTarget, double speed, double ease) {
		return velocity + (Math.signum(toTarget) * speed - velocity) * ease;
	}
}
