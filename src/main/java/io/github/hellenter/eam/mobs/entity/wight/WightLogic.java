package io.github.hellenter.eam.mobs.entity.wight;

/** The wight's decisions, free of the game so they can be unit tested. Angles are Minecraft yaws in degrees. */
public final class WightLogic {
	/** A point on the ground. */
	public record Point(double x, double z) {
	}

	/** What the wight does next while it stalks. */
	public enum Plan {
		/** Keep its distance, unseen. */
		STALK,
		/** A quick raid on a busy player: a couple of blows, then away. */
		RAID,
		/** The player is weak: go in from behind to finish them. */
		HUNT
	}

	private WightLogic() {
	}

	/** Natural wights appear only once the world is old enough. */
	public static boolean oldEnough(int day, int minDay) {
		return day >= minDay;
	}

	/** Whether a player this hurt gets hunted down. */
	public static boolean weak(float health, float maxHealth, double huntHealth) {
		return maxHealth > 0.0F && health < maxHealth * huntHealth;
	}

	/**
	 * What a stalking wight does next. A weak player is hunted whenever the pass cooldown allows; a busy one it can see
	 * is raided, now and then, once the raid cooldown is over; otherwise it keeps stalking.
	 *
	 * @param roll a random number in [0, 1), compared with the raid chance
	 */
	public static Plan plan(boolean weak, boolean busy, boolean sees, int raidCooldown, int huntCooldown, float roll, float raidChance) {
		if (weak) {
			return huntCooldown <= 0 && sees ? Plan.HUNT : Plan.STALK;
		}
		if (busy && sees && raidCooldown <= 0 && roll < raidChance) {
			return Plan.RAID;
		}
		return Plan.STALK;
	}

	/**
	 * A point {@code distance} away from a player at ({@code px}, {@code pz}) looking along {@code yaw}, behind them,
	 * turned {@code offset} degrees off straight behind.
	 */
	public static Point behind(double px, double pz, float yaw, double distance, float offset) {
		// Minecraft yaw 0 looks along +z; the look vector is (-sin, cos).
		double angle = Math.toRadians(yaw + 180.0F + offset);
		return new Point(px - Math.sin(angle) * distance, pz + Math.cos(angle) * distance);
	}

	/**
	 * Whether a point at ({@code x}, {@code z}) lies behind a player at ({@code px}, {@code pz}) looking along
	 * {@code yaw}: more than {@code halfAngle} degrees off their line of sight.
	 */
	public static boolean outOfSight(double px, double pz, float yaw, double x, double z, float halfAngle) {
		double dx = x - px;
		double dz = z - pz;
		double length = Math.sqrt(dx * dx + dz * dz);
		if (length < 1.0E-6) {
			return false;
		}
		double yawRad = Math.toRadians(yaw);
		double dot = (-Math.sin(yawRad) * dx + Math.cos(yawRad) * dz) / length;
		return dot < Math.cos(Math.toRadians(halfAngle));
	}

	/** Damage it takes: more while it burns. */
	public static float damageTaken(float damage, boolean burning, float burningMultiplier) {
		return burning ? damage * burningMultiplier : damage;
	}
}
