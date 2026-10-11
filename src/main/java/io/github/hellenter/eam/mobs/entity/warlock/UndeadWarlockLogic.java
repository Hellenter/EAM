package io.github.hellenter.eam.mobs.entity.warlock;

/** Pure undead warlock rules, kept apart from the entity so they can be unit tested. */
public final class UndeadWarlockLogic {
	/** Ticks between two missiles of one cast. */
	public static final int MISSILE_INTERVAL = 4;
	/** The gust catches what is in front of him: the cosine of half its spread (about 140 degrees in all). */
	public static final double WIND_CONE = 0.35;
	/** When his Lost stand further than this from the player on average, he hastens them; closer, he strengthens them. */
	public static final double HASTE_DISTANCE = 6.0;

	private UndeadWarlockLogic() {
	}

	public enum Move {
		RETREAT, APPROACH, HOLD
	}

	/** Where he goes: away from a player too close, closer to one too far or out of sight, else he stays. */
	public static Move move(double distance, boolean sees, double minRange, double maxRange) {
		if (distance < minRange) {
			return Move.RETREAT;
		}
		return !sees || distance > maxRange ? Move.APPROACH : Move.HOLD;
	}

	/**
	 * The spell he casts now, or {@link WarlockSpell#NONE}. A player within reach of the gust gets it first; then, now
	 * and then, the bats; then the buff while some of his Lost lack it; then the missiles.
	 *
	 * @param ready      which spells are off cooldown, by {@link WarlockSpell} ordinal
	 * @param unbuffed   Lost near him without Speed or Strength
	 * @param batsNearby vampire bats already near him
	 * @param roll       a uniform roll in [0, 1) for the bats
	 */
	public static WarlockSpell choose(double distance, boolean sees, boolean[] ready, int unbuffed, int batsNearby, double roll,
			UndeadWarlockSettings settings) {
		if (ready[WarlockSpell.WIND.ordinal()] && sees && distance <= settings.wind().trigger()) {
			return WarlockSpell.WIND;
		}
		if (ready[WarlockSpell.BATS.ordinal()] && batsNearby < settings.bats().maxNearby() && roll < settings.bats().chance()) {
			return WarlockSpell.BATS;
		}
		if (ready[WarlockSpell.BUFF.ordinal()] && unbuffed > 0) {
			return WarlockSpell.BUFF;
		}
		if (ready[WarlockSpell.MISSILES.ordinal()] && sees && distance <= settings.missiles().range()) {
			return WarlockSpell.MISSILES;
		}
		return WarlockSpell.NONE;
	}

	/** Tick of the cast on which missile {@code index} (from 0) leaves his hand. */
	public static int missileTick(int index) {
		return WarlockSpell.MISSILES.release() + index * MISSILE_INTERVAL;
	}

	/** Index of the missile released on this tick of the cast, or -1. */
	public static int missileAt(int castTick, int count) {
		int since = castTick - WarlockSpell.MISSILES.release();
		if (since < 0 || since % MISSILE_INTERVAL != 0) {
			return -1;
		}
		int index = since / MISSILE_INTERVAL;
		return index < count ? index : -1;
	}

	/** Speed for the Lost far from the player, so they close in; Strength for those already fighting. */
	public static boolean haste(double averageDistance) {
		return averageDistance > HASTE_DISTANCE;
	}

	/** Bats in a flock for a uniform roll in [0, 1): {@code min} to {@code max}, both included. */
	public static int flockSize(int min, int max, double roll) {
		int low = Math.min(min, max);
		int high = Math.max(min, max);
		return low + Math.min((int) (roll * (high - low + 1)), high - low);
	}

	/** Whether something at ({@code dx}, {@code dz}) from him lies in the gust in front of him, facing ({@code lookX}, {@code lookZ}). */
	public static boolean inGust(double dx, double dz, double lookX, double lookZ) {
		double length = Math.sqrt(dx * dx + dz * dz);
		double look = Math.sqrt(lookX * lookX + lookZ * lookZ);
		if (length < 1.0E-4) {
			return true;
		}
		return look > 1.0E-4 && (dx * lookX + dz * lookZ) / (length * look) >= WIND_CONE;
	}

	/**
	 * The missile's next velocity: its heading turned part of the way toward the target, at full speed.
	 *
	 * @return {x, y, z}
	 */
	public static double[] steer(double vx, double vy, double vz, double tx, double ty, double tz, double speed, double turn) {
		double[] heading = normalize(vx, vy, vz);
		double[] toTarget = normalize(tx, ty, tz);
		double[] mixed = normalize(heading[0] + (toTarget[0] - heading[0]) * turn, heading[1] + (toTarget[1] - heading[1]) * turn,
				heading[2] + (toTarget[2] - heading[2]) * turn);
		if (mixed[0] == 0.0 && mixed[1] == 0.0 && mixed[2] == 0.0) {
			mixed = toTarget;
		}
		return new double[] {mixed[0] * speed, mixed[1] * speed, mixed[2] * speed};
	}

	private static double[] normalize(double x, double y, double z) {
		double length = Math.sqrt(x * x + y * y + z * z);
		return length < 1.0E-6 ? new double[] {0.0, 0.0, 0.0} : new double[] {x / length, y / length, z / length};
	}
}
