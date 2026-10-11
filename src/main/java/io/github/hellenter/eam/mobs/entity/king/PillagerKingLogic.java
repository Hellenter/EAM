package io.github.hellenter.eam.mobs.entity.king;

import org.jspecify.annotations.Nullable;

import java.util.function.DoubleSupplier;

/** The Pillager King's choices, free of game state so they can be unit tested. */
public final class PillagerKingLogic {
	public enum Attack {
		/** One cut with the right axe. */
		SLASH,
		/** Four cuts, parryable at the start. */
		COMBO,
		/** Rush, kick and a leap with both axes; a shield only softens it, a hit while he is in the air parries it. */
		ONSLAUGHT,
		/** A long spin of six weak cuts. */
		TORNADO,
		/** The left axe thrown; it knocks out a shield it hits. */
		THROW,
		/** The left axe thrown round the target into its back. */
		BOOMERANG,
		/** A run at the target after a red warning: three cuts and a kick. */
		CHARGE
	}

	/** What he does about a hit that is about to come. */
	public enum Reach {
		/** The target will be in reach. */
		HITS,
		/** The target will be out of reach, but a dash closes the gap. */
		DASH,
		/** The target will be out of reach and the dash is cooling down or too short. */
		MISSES
	}

	private PillagerKingLogic() {
	}

	/**
	 * The attack to start, or {@code null} to keep closing in.
	 *
	 * @param distance     flat distance to the target
	 * @param blocking     the target holds up a shield
	 * @param outOfReach   ticks the target has stayed out of his reach
	 * @param cooldowns    ticks left for each {@link Attack}, by ordinal
	 * @param rolls        random numbers in [0, 1)
	 */
	public static @Nullable Attack choose(double distance, boolean blocking, int outOfReach, int[] cooldowns, PillagerKingSettings settings,
			DoubleSupplier rolls) {
		PillagerKingSettings.AxeThrow axe = settings.axeThrow();
		boolean throwReady = cooldowns[Attack.THROW.ordinal()] <= 0 && distance <= axe.maxRange();
		if (distance > settings.reach()) {
			PillagerKingSettings.Charge charge = settings.charge();
			if (cooldowns[Attack.CHARGE.ordinal()] <= 0 && distance >= charge.from() && distance <= charge.to()
					&& rolls.getAsDouble() < charge.chance()) {
				return Attack.CHARGE;
			}
			PillagerKingSettings.Boomerang boomerang = settings.boomerang();
			if (cooldowns[Attack.BOOMERANG.ordinal()] <= 0 && distance >= boomerang.minRange() && distance <= boomerang.maxRange()
					&& (blocking || outOfReach >= axe.after()) && rolls.getAsDouble() < boomerang.chance()) {
				return Attack.BOOMERANG;
			}
			return throwReady && distance >= axe.minRange() && (blocking || outOfReach >= axe.after()) ? Attack.THROW : null;
		}
		// A raised shield is what the throw is for: it knocks it out for the attack that follows.
		if (blocking && throwReady && rolls.getAsDouble() < axe.chanceVsShield()) {
			return Attack.THROW;
		}
		return chooseMelee(blocking, cooldowns, settings, rolls);
	}

	/** One of his axe attacks, for a target in reach. */
	public static Attack chooseMelee(boolean blocking, int[] cooldowns, PillagerKingSettings settings, DoubleSupplier rolls) {
		PillagerKingSettings.Onslaught onslaught = settings.onslaught();
		if (cooldowns[Attack.ONSLAUGHT.ordinal()] <= 0
				&& rolls.getAsDouble() < (blocking ? onslaught.chanceVsShield() : onslaught.chance())) {
			return Attack.ONSLAUGHT;
		}
		if (cooldowns[Attack.TORNADO.ordinal()] <= 0 && rolls.getAsDouble() < settings.tornado().chance()) {
			return Attack.TORNADO;
		}
		if (cooldowns[Attack.COMBO.ordinal()] <= 0 && rolls.getAsDouble() < settings.combo().chance()) {
			return Attack.COMBO;
		}
		return Attack.SLASH;
	}

	/** Whether a hit lands, needs a dash, or misses. */
	public static Reach reach(double predictedDistance, double reach, int dashCooldown, PillagerKingSettings.Dash dash) {
		if (predictedDistance <= reach) {
			return Reach.HITS;
		}
		return dashCooldown <= 0 && predictedDistance <= reach + dash.range() ? Reach.DASH : Reach.MISSES;
	}

	/** Whether he dashes in at a target this far, out of his reach, when he is ready to attack. */
	public static boolean dashIn(double distance, int dashCooldown, PillagerKingSettings.Dash dash) {
		return dashCooldown <= 0 && distance >= dash.from() && distance <= dash.to();
	}

	/** Whether he runs: the target has been out of his reach for long enough. */
	public static boolean runs(int outOfReach, PillagerKingSettings.Run run) {
		return outOfReach >= run.after();
	}

	/**
	 * The movement speed modifier (a share of the attribute) that makes him {@code bonus} faster on the ground. A mob's
	 * ground speed grows with the square of its speed attribute: it is both the push and the length of the push.
	 */
	public static double speedModifier(double bonus) {
		return Math.sqrt(1.0 + bonus) - 1.0;
	}

	/**
	 * Whether his totem saves him from this blow.
	 *
	 * @param health      his health after the blow
	 * @param used        the totem already broke
	 * @param unstoppable the damage kills through invulnerability ({@code /kill}, the void)
	 */
	public static boolean totemSaves(float health, boolean used, boolean unstoppable) {
		return health <= 0.0F && !used && !unstoppable;
	}

	/** Whether a hit taken on tick {@code t} of an action parries him: at the start of a combo, or while he leaps in the onslaught. */
	public static boolean parries(KingAction action, int t) {
		return parries(action, t, KingAction.COMBO_PARRY_END);
	}

	/**
	 * Whether a hit taken on tick {@code t} of an action parries him, with the combo's window ending at tick {@code end};
	 * the onslaught's window is cut short by as much.
	 */
	public static boolean parries(KingAction action, int t, int end) {
		int cut = KingAction.COMBO_PARRY_END - end;
		return action == KingAction.COMBO && t < end
				|| action == KingAction.ONSLAUGHT && t >= KingAction.ONSLAUGHT_PARRY_START && t < KingAction.ONSLAUGHT_PARRY_END - cut;
	}

	/** The blow of the onslaught on a target that may hold up a shield against it: the shield only takes a share off. */
	public static float onslaughtDamage(float damage, boolean shielded, float shieldReduction) {
		return shielded ? damage * (1.0F - shieldReduction) : damage;
	}

	/**
	 * Horizontal speed for a leap that lands {@code distance} blocks away after {@code ticks} ticks in the air, with the
	 * air slowing a mob to 0.91 of its speed each tick.
	 */
	public static double leapSpeed(double distance, int ticks) {
		double carried = (1.0 - Math.pow(0.91, ticks)) / 0.09;
		return Math.max(0.0, distance) / carried;
	}
}
