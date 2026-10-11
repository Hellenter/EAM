package io.github.hellenter.eam.mobs.entity.knight;

import org.jspecify.annotations.Nullable;

/** The skeleton knight's choices, free of game state so they can be unit tested. */
public final class SkeletonKnightLogic {
	public enum Attack {
		/** One cut. */
		SLASH,
		/** A spear jab with a longer reach. */
		POKE,
		/** Three quick cuts and a hop back. */
		COMBO,
		/** Sword raised overhead, then a cut that goes through a shield and knocks it out. */
		HEAVY
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

	private SkeletonKnightLogic() {
	}

	/**
	 * The attack to start, or {@code null} while the target is out of reach.
	 *
	 * @param heavyRoll random number in [0, 1) for the heavy strike
	 * @param comboRoll random number in [0, 1) for the combo
	 */
	public static @Nullable Attack choose(double distance, boolean targetBlocking, int comboCooldown, int heavyCooldown,
			SkeletonKnightSettings settings, double heavyRoll, double comboRoll) {
		if (distance > settings.reach()) {
			return null;
		}
		double heavyChance = targetBlocking ? settings.heavyChanceVsShield() : settings.heavyChance();
		if (heavyCooldown <= 0 && heavyRoll < heavyChance) {
			return Attack.HEAVY;
		}
		if (comboCooldown <= 0 && comboRoll < settings.comboChance()) {
			return Attack.COMBO;
		}
		return Attack.SLASH;
	}

	/**
	 * Whether a hit lands, needs a dash, or misses.
	 *
	 * @param predictedDistance flat distance to where the target will be on the hit tick
	 * @param reach             the reach of that hit
	 * @param dashCooldown      ticks left before he can dash again
	 */
	public static Reach reach(double predictedDistance, double reach, int dashCooldown, SkeletonKnightSettings.Dash dash) {
		if (predictedDistance <= reach) {
			return Reach.HITS;
		}
		return dashCooldown <= 0 && predictedDistance <= reach + dash.range() ? Reach.DASH : Reach.MISSES;
	}

	/** Ticks his shield stays down after an axe that disables a player's shield for {@code seconds}. */
	public static int shieldDisableTicks(float seconds, float multiplier) {
		return Math.max(0, Math.round(seconds * multiplier * 20.0F));
	}
}
