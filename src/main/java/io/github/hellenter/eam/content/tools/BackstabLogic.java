package io.github.hellenter.eam.content.tools;

/** Backstab with a knife: a sneaking attacker behind the target deals extra damage. Pure logic. */
public final class BackstabLogic {
	/** Extra damage as a share of the hit: +50%. */
	public static final float BONUS = 0.5F;
	/** The attacker must be within this many degrees of straight behind the target (a 90° cone). */
	public static final double MAX_ANGLE_FROM_BEHIND = 45.0;

	private BackstabLogic() {
	}

	/**
	 * @param targetYaw target's body yaw in degrees (Minecraft convention: 0 faces +Z, 90 faces -X)
	 * @param dx        attacker x minus target x
	 * @param dz        attacker z minus target z
	 */
	public static boolean isBehind(float targetYaw, double dx, double dz) {
		double length = Math.sqrt(dx * dx + dz * dz);
		if (length < 1.0E-4) {
			return false;
		}
		double yaw = Math.toRadians(targetYaw);
		double facingX = -Math.sin(yaw);
		double facingZ = Math.cos(yaw);
		// Cosine of the angle between where the target faces and where the attacker stands; -1 is straight behind.
		double cos = (facingX * dx + facingZ * dz) / length;
		return cos <= -Math.cos(Math.toRadians(MAX_ANGLE_FROM_BEHIND));
	}

	public static float bonusDamage(float damage, boolean sneaking, boolean behind) {
		return sneaking && behind ? damage * BONUS : 0.0F;
	}
}
