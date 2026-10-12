package io.github.hellenter.eam.mobs.entity.crossbowman;

import org.jspecify.annotations.Nullable;

/** Pure rules of the elite crossbowman: which shot he picks, how he moves and how high he aims. */
public final class EliteCrossbowmanLogic {
	/** Gravity of an arrow, blocks per tick squared. */
	public static final double ARROW_GRAVITY = 0.05;
	/** Gravity of a thrown item such as the net. */
	public static final double THROWN_GRAVITY = 0.03;
	/** Share of its speed an arrow or a thrown item keeps each tick in the air. */
	public static final double DRAG = 0.99;
	private static final double MAX_ANGLE = Math.PI / 4.0;
	private static final int MAX_FLIGHT_TICKS = 200;

	private EliteCrossbowmanLogic() {
	}

	public enum Attack {
		NORMAL,
		BURST,
		CHARGED,
		NET
	}

	public enum Move {
		RETREAT,
		APPROACH,
		HOLD
	}

	/**
	 * Picks the next shot by weight among those off cooldown: the net only at a player within its range. {@code roll}
	 * is a random number in [0, 1). Returns null when nothing has weight.
	 */
	public static @Nullable Attack choose(float roll, int[] cooldowns, EliteCrossbowmanSettings settings, double distance, boolean player) {
		int[] weights = new int[Attack.values().length];
		int total = 0;
		for (Attack attack : Attack.values()) {
			int weight = cooldowns[attack.ordinal()] > 0 ? 0 : weight(attack, settings);
			if (attack == Attack.NET && (!player || distance > settings.net().maxRange())) {
				weight = 0;
			}
			weights[attack.ordinal()] = weight;
			total += weight;
		}
		if (total <= 0) {
			return null;
		}
		float pick = roll * total;
		for (Attack attack : Attack.values()) {
			pick -= weights[attack.ordinal()];
			if (pick < 0.0F) {
				return attack;
			}
		}
		return Attack.NORMAL;
	}

	static int weight(Attack attack, EliteCrossbowmanSettings settings) {
		return switch (attack) {
			case NORMAL -> settings.normal().weight();
			case BURST -> settings.burst().weight();
			case CHARGED -> settings.charged().weight();
			case NET -> settings.net().weight();
		};
	}

	public static int loadTicks(Attack attack, EliteCrossbowmanSettings settings) {
		return switch (attack) {
			case NORMAL -> settings.normal().loadTicks();
			case BURST -> settings.burst().loadTicks();
			case CHARGED -> settings.charged().loadTicks();
			case NET -> settings.net().loadTicks();
		};
	}

	/** Ticks he holds his aim before the first shot: the charged shot glows red for its whole windup. */
	public static int aimTicks(Attack attack, EliteCrossbowmanSettings settings) {
		return attack == Attack.CHARGED ? Math.max(settings.aimTicks(), settings.charged().windupTicks()) : settings.aimTicks();
	}

	public static int cooldown(Attack attack, EliteCrossbowmanSettings settings) {
		return switch (attack) {
			case NORMAL -> 0;
			case BURST -> settings.burst().cooldown();
			case CHARGED -> settings.charged().cooldown();
			case NET -> settings.net().cooldown();
		};
	}

	/** Keeps the target between his minimum and maximum range, and in sight. */
	public static Move move(double distance, boolean sees, EliteCrossbowmanSettings settings) {
		if (!sees || distance > settings.maxRange()) {
			return Move.APPROACH;
		}
		return distance < settings.minRange() ? Move.RETREAT : Move.HOLD;
	}

	/**
	 * How high above the line to the target to aim, in blocks over {@code flat} blocks of flat distance, so a
	 * projectile of this speed and gravity comes down on a point {@code rise} blocks above the muzzle. It flies the shot
	 * tick by tick like vanilla does (move, slow down by {@link #DRAG}, fall) and searches the flattest angle that
	 * reaches the point; out of reach, it aims at 45 degrees.
	 */
	public static double aimHeight(double flat, double rise, double speed, double gravity) {
		if (flat < 1.0E-3) {
			return rise;
		}
		double low = -MAX_ANGLE;
		double high = MAX_ANGLE;
		if (heightAt(flat, high, speed, gravity) < rise) {
			return flat * Math.tan(high);
		}
		for (int i = 0; i < 30; i++) {
			double mid = (low + high) * 0.5;
			if (heightAt(flat, mid, speed, gravity) < rise) {
				low = mid;
			} else {
				high = mid;
			}
		}
		return flat * Math.tan((low + high) * 0.5);
	}

	/** Height of the shot when it has flown {@code flat} blocks forward, or far below if it never gets there. */
	static double heightAt(double flat, double angle, double speed, double gravity) {
		double vx = speed * Math.cos(angle);
		double vy = speed * Math.sin(angle);
		double x = 0.0;
		double y = 0.0;
		for (int tick = 0; tick < MAX_FLIGHT_TICKS; tick++) {
			double nx = x + vx;
			double ny = y + vy;
			if (nx >= flat) {
				return y + (ny - y) * (flat - x) / (nx - x);
			}
			x = nx;
			y = ny;
			vx *= DRAG;
			vy = vy * DRAG - gravity;
		}
		return Double.NEGATIVE_INFINITY;
	}

	/**
	 * The arrow's base damage for a hit of this damage at this speed: vanilla multiplies the base damage by the arrow's
	 * speed on hit.
	 */
	public static double baseDamage(float damage, float speed) {
		return damage / speed;
	}

	/** The red bolt's damage, cut when it goes through a raised shield that faces it. */
	public static float redBoltDamage(float damage, boolean shielded, float shieldMultiplier) {
		return shielded ? damage * shieldMultiplier : damage;
	}
}
