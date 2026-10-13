package io.github.hellenter.eam.mobs.entity.warg;

/** Pure rules of a warg pack's hunt: who the player is watching, where each warg circles, who strikes next. */
public final class WargLogic {
	private WargLogic() {
	}

	/**
	 * Degrees between where the player looks and the warg, seen from above: 0 straight ahead, 180 right behind.
	 *
	 * @param lookX the player's horizontal look direction
	 * @param toX   the direction from the player to the warg
	 */
	public static double offAngle(double lookX, double lookZ, double toX, double toZ) {
		double look = Math.sqrt(lookX * lookX + lookZ * lookZ);
		double to = Math.sqrt(toX * toX + toZ * toZ);
		if (look < 1.0E-6 || to < 1.0E-6) {
			// Looking straight up or down, or the warg is right underfoot: it counts as watched.
			return 0.0;
		}
		double cos = (lookX * toX + lookZ * toZ) / (look * to);
		return Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, cos))));
	}

	/** Whether a warg {@code offAngle} degrees off the player's look is one they are watching. */
	public static boolean watched(double offAngle, double seenAngle) {
		return offAngle < seenAngle;
	}

	/**
	 * The next striker: of the wargs ready to strike, the one furthest from where the player looks, as long as the
	 * player is not watching it.
	 *
	 * @param offAngles each warg's {@link #offAngle}, or a negative number for one not ready
	 * @return its index, or -1 when every ready warg is watched
	 */
	public static int pickStriker(double[] offAngles, double seenAngle) {
		int best = -1;
		for (int i = 0; i < offAngles.length; i++) {
			double angle = offAngles[i];
			if (angle >= 0.0 && !watched(angle, seenAngle) && (best < 0 || angle > offAngles[best])) {
				best = i;
			}
		}
		return best;
	}

	/** The angle, in radians, of the {@code index}-th of {@code count} places spread evenly around a ring turned by {@code ringAngle}. */
	public static double slotAngle(double ringAngle, int index, int count) {
		return ringAngle + Math.PI * 2.0 * index / Math.max(1, count);
	}

	/** How many wargs a leader brings, from {@code roll} in [0, 1). */
	public static int packSize(int min, int max, double roll) {
		int low = Math.min(min, max);
		int high = Math.max(min, max);
		return low + Math.min(high - low, (int) Math.floor(roll * (high - low + 1)));
	}
}
