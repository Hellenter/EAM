package io.github.hellenter.eam.mobs.entity.ghosthorse;

import net.minecraft.util.Mth;

/** Pure ghost horse rules, unit tested: its speed and jump, its health over time and what its rider deals and takes. */
public final class GhostHorseLogic {
	/** The fastest a bred horse can be: {@code AbstractHorse.generateSpeed} with every roll at its top. */
	public static final double HORSE_MAX_SPEED = (0.45 + 0.3 * 3) * 0.25;
	/** The highest a bred horse can jump: {@code AbstractHorse.generateJumpStrength} with every roll at its top. */
	public static final double HORSE_MAX_JUMP_STRENGTH = 0.4 + 0.2 * 3;
	private static final double GRAVITY = 0.08;
	private static final double AIR_DRAG = 0.98;

	private GhostHorseLogic() {
	}

	/** Its speed attribute: the fastest horse's, {@code bonus} more (0.2 is a fifth faster). A ridden horse moves linearly with it. */
	public static double speed(double bonus) {
		return HORSE_MAX_SPEED * (1.0 + bonus);
	}

	/** Its jump strength: the one that clears {@code bonus} more height than the best jumping horse. */
	public static double jumpStrength(double bonus) {
		double target = jumpHeight(HORSE_MAX_JUMP_STRENGTH) * (1.0 + bonus);
		double low = 0.0;
		double high = 4.0;
		for (int i = 0; i < 60; i++) {
			double mid = (low + high) / 2.0;
			if (jumpHeight(mid) < target) {
				low = mid;
			} else {
				high = mid;
			}
		}
		return (low + high) / 2.0;
	}

	/** How many blocks a full jump of {@code strength} rises: the take-off speed, slowed by gravity and air each tick. */
	public static double jumpHeight(double strength) {
		double height = 0.0;
		double velocity = strength;
		while (velocity > 0.0) {
			height += velocity;
			velocity = (velocity - GRAVITY) * AIR_DRAG;
		}
		return height;
	}

	/**
	 * Its health one second on: below {@code base} it heals {@code regen}; above it, the overflow from its master's blows
	 * runs off by {@code decay} back down to {@code base}.
	 */
	public static float secondLater(float health, float base, float regen, float decay) {
		if (health < base) {
			return Math.min(base, health + regen);
		}
		if (health > base) {
			return Math.max(base, health - decay);
		}
		return health;
	}

	/** Whether it heals again: {@code delay} ticks have passed since it was last hurt. */
	public static boolean healsAfter(int ticksSinceHurt, int delay) {
		return ticksSinceHurt >= delay;
	}

	/** Its health after its master dealt {@code damage}: a share of it heals the horse, past its base health up to {@code cap}. */
	public static float afterMasterHit(float health, float cap, float damage, float share) {
		if (damage <= 0.0F) {
			return health;
		}
		return Math.max(health, Math.min(cap, health + damage * share));
	}

	/** The health it is summoned with: what it had when called back, healed for the seconds it rested in the horn. */
	public static float rested(float stored, float base, float regen, long ticksInHorn) {
		float healed = Math.max(0L, ticksInHorn) / 20.0F * regen;
		return Math.min(base, Math.min(stored, base) + healed);
	}

	/** The fall damage it takes: what the fall would deal, at most {@code cap}. */
	public static int fallDamage(int damage, float cap) {
		return Math.min(damage, Mth.floor(cap));
	}

	/** Damage {@code amount} scaled by a rider's {@code factor}, taken (0.8) or dealt (1.15). */
	public static float scaled(float amount, float factor) {
		return amount * factor;
	}
}
