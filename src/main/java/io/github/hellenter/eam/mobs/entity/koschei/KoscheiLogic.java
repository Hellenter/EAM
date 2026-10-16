package io.github.hellenter.eam.mobs.entity.koschei;

import org.jspecify.annotations.Nullable;

import java.util.function.DoubleSupplier;

/** Koschei's choices, free of game state so they can be unit tested. */
public final class KoscheiLogic {
	public enum Attack {
		STRIKE,
		DOUBLE,
		ORANGE,
		RED,
		OVERHEAD,
		LUNGE,
		KICK,
		FORCE,
		BOLT,
		/** Not a blow: the target stood too close for his lunge too often, so he makes room (the burst of force or a hop back). */
		MAKE_ROOM
	}

	/**
	 * What he does next.
	 *
	 * @param attack       the attack, or null to keep closing in
	 * @param lungeFailed  he wanted to lunge but the target stood too close
	 */
	public record Choice(@Nullable Attack attack, boolean lungeFailed) {
		static final Choice WALK = new Choice(null, false);
	}

	private KoscheiLogic() {
	}

	/**
	 * The attack to start against a target this far, or none to keep walking in.
	 *
	 * @param distance    flat distance to the target
	 * @param shieldReady the target's shield is not knocked out
	 * @param shieldTicks ticks the target has held its shield up toward him; 0 when it holds none up
	 * @param lungeFails  times in a row the target stood too close for a lunge he wanted
	 * @param cooldowns   ticks left for each {@link Attack}, by ordinal
	 * @param last        his last attack, picked less often again
	 * @param rolls       random numbers in [0, 1)
	 */
	public static Choice choose(double distance, boolean shieldReady, int shieldTicks, int lungeFails, int[] cooldowns, @Nullable Attack last,
			KoscheiSettings.PhaseOne settings, DoubleSupplier rolls) {
		KoscheiSettings.Bolt bolt = settings.bolt();
		// The bolt is his rare pool: never from close by.
		if (cooldowns[Attack.BOLT.ordinal()] <= 0 && distance >= bolt.minRange() && distance <= bolt.maxRange()
				&& rolls.getAsDouble() < bolt.chance()) {
			return new Choice(Attack.BOLT, false);
		}
		KoscheiSettings.Lunge lunge = settings.lunge();
		boolean lungeReady = cooldowns[Attack.LUNGE.ordinal()] <= 0;
		if (distance > settings.reach()) {
			if (lungeReady && distance >= lunge.minRange() && distance <= lunge.maxRange() && rolls.getAsDouble() < lunge.chance()) {
				return new Choice(Attack.LUNGE, false);
			}
			return Choice.WALK;
		}
		KoscheiSettings.Kick kick = settings.kick();
		boolean kickReady = cooldowns[Attack.KICK.ordinal()] <= 0 && distance <= kick.reach();
		if (kickReady && shieldTicks >= kick.shieldTicks()) {
			// Up close behind a raised shield: he kicks it aside.
			return new Choice(Attack.KICK, false);
		}
		if (lungeFails > lunge.maxFails()) {
			return new Choice(Attack.MAKE_ROOM, false);
		}
		if (cooldowns[Attack.FORCE.ordinal()] <= 0 && rolls.getAsDouble() < settings.force().chance()) {
			return new Choice(Attack.FORCE, false);
		}
		// A lunge he would take here fails: the target is inside its range. He cuts instead.
		boolean failed = lungeReady && distance < lunge.minRange() && rolls.getAsDouble() < lunge.chance();
		return new Choice(melee(shieldReady, kickReady, cooldowns, last, settings, rolls), failed);
	}

	/** One of his blows for a target in reach, picked by weight; the kick only for a target close enough for it. */
	public static Attack melee(boolean shieldReady, boolean kickReady, int[] cooldowns, @Nullable Attack last, KoscheiSettings.PhaseOne settings,
			DoubleSupplier rolls) {
		Attack[] blows = {Attack.STRIKE, Attack.DOUBLE, Attack.ORANGE, Attack.RED, Attack.OVERHEAD, Attack.KICK};
		float[] weights = {
				settings.strike().weight(),
				settings.doubleStrike().weight(),
				shieldReady ? settings.orange().weight() : 0.0F,
				settings.red().weight(),
				settings.overhead().weight(),
				kickReady ? settings.kick().weight() : 0.0F
		};
		float total = 0.0F;
		for (int i = 0; i < blows.length; i++) {
			if (cooldowns[blows[i].ordinal()] > 0) {
				weights[i] = 0.0F;
			} else if (blows[i] == last) {
				weights[i] *= 0.25F;
			}
			total += weights[i];
		}
		if (total <= 0.0F) {
			return Attack.STRIKE;
		}
		double pick = rolls.getAsDouble() * total;
		for (int i = 0; i < blows.length; i++) {
			pick -= weights[i];
			if (pick < 0.0) {
				return blows[i];
			}
		}
		return Attack.STRIKE;
	}

	/** Whether a hit on tick {@code t} of an action parries him, with a parry window of {@code window} ticks. */
	public static boolean parries(KoscheiAction action, int t, int window) {
		int start = action.parryStart(window);
		return start >= 0 && t >= start && t < action.parryEnd();
	}

	/** Damage to him after his resistance: physical damage loses {@code resist} of it, the rest goes through. */
	public static float resisted(float damage, boolean physical, float resist) {
		return physical ? damage * (1.0F - resist) : damage;
	}

	/** A blow on a target that may hold up a shield against it: the shield only takes a share off. */
	public static float shielded(float damage, boolean shield, float reduction) {
		return shield ? damage * (1.0F - reduction) : damage;
	}

	/** Damage of one pulse of the bolt. */
	public static float boltPulse(float perSecond, int pulse) {
		return perSecond * pulse / 20.0F;
	}

	/**
	 * Horizontal speed that carries a thrown target {@code distance} blocks in {@code ticks} ticks in the air, with the
	 * air slowing it to 0.91 of its speed each tick.
	 */
	public static double pushSpeed(double distance, int ticks) {
		double carried = (1.0 - Math.pow(0.91, ticks)) / 0.09;
		return Math.max(0.0, distance) / carried;
	}

	/**
	 * Angles off straight behind the target to try for a teleport, in degrees: a random one within {@code maxAngle} to
	 * either side first, then the mirror of it, then straight behind, then wider ones all round.
	 */
	public static float[] teleportAngles(float maxAngle, DoubleSupplier rolls) {
		float first = (float) ((rolls.getAsDouble() * 2.0 - 1.0) * maxAngle);
		return new float[] {first, -first, 0.0F, maxAngle, -maxAngle, 110.0F, -110.0F, 180.0F};
	}

	/** Whether {@code taken} health lost lately, out of {@code maxHealth}, is more than {@code share} of it: time to leap back. */
	public static boolean burstLeap(float taken, float maxHealth, float share) {
		return taken > maxHealth * share;
	}

	/** Damage of his blow to this target: a player takes it as it is, anything else {@code mobMultiplier} times. */
	public static float damageTo(float damage, boolean player, float mobMultiplier) {
		return player ? damage : damage * mobMultiplier;
	}

	/** He never lands a hop in closer than this to the target. */
	public static final double HOP_IN_GAP = 2.0;

	/**
	 * How far a hop in at a target {@code distance} away goes: a random length between {@code min} and {@code max}, cut so it
	 * lands {@link #HOP_IN_GAP} short of the target; 0 when that leaves less than {@code min}.
	 *
	 * @param roll uniform random in [0, 1)
	 */
	public static double hopInLength(double distance, double min, double max, double roll) {
		double low = Math.min(min, max);
		double length = Math.min(low + roll * (Math.max(min, max) - low), distance - HOP_IN_GAP);
		return length >= low ? length : 0.0;
	}
}
