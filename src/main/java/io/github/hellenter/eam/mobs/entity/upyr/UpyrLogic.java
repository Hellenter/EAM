package io.github.hellenter.eam.mobs.entity.upyr;

/** Pure upyr and ward rules, kept apart from the entity so they can be unit tested. */
public final class UpyrLogic {
	private UpyrLogic() {
	}

	/** Damage of a bloodsucker's blow: cut by the garlic factor when the victim smells of garlic. */
	public static float wardedDamage(float damage, boolean garlic, float garlicFactor) {
		return garlic ? damage * garlicFactor : damage;
	}

	/**
	 * Health an upyr drinks back from a bite: the health its victim lost, times the heal factor. A victim warded by garlic
	 * gives nothing, since the upyr will not drink from them.
	 */
	public static float drunk(float victimHealthBefore, float victimHealthAfter, float healFactor, boolean garlic) {
		if (garlic) {
			return 0.0F;
		}
		return Math.max(0.0F, victimHealthBefore - victimHealthAfter) * healFactor;
	}

	/** Whether an upyr appears at this check: rolled against the chance while few upyrs are around. */
	public static boolean spawnDue(int upyrsNearby, int maxNearby, float roll, float chance) {
		return upyrsNearby < maxNearby && roll < chance;
	}

	/** Distance of a new upyr from the player for a uniform roll in [0, 1). */
	public static double spawnDistance(double min, double max, float roll) {
		double low = Math.min(min, max);
		return low + (Math.max(min, max) - low) * roll;
	}
}
