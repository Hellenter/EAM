package io.github.hellenter.eam.mobs.entity.cavalry;

import io.github.hellenter.eam.mobs.entity.knight.SkeletonKnightSettings;

import org.jspecify.annotations.Nullable;

/** The heavy cavalrymen's choices and speeds, free of game state so they can be unit tested. */
public final class LostCavalryLogic {
	/** A walking player's movement speed attribute. */
	static final double PLAYER_WALK = 0.1;
	/** A skeleton's movement speed attribute; the Lost walk as fast. */
	static final double SKELETON_WALK = 0.25;

	/** The axe moves on foot. */
	public enum FootAttack {
		/** One plain cut: an axe knocks a raised shield out of the guard. */
		SLASH,
		/** Two quick cuts, right then left, each weaker than a plain cut. */
		DOUBLE,
		/** Axe raised overhead, then a cut that goes through a shield and stuns it. */
		HEAVY,
		/** A crouch with the axe drawn far back, then a full turn hitting everyone around, round a shield. */
		SWEEP
	}

	/** What a mounted cavalryman does next while he rides round the player. */
	public enum Mounted {
		/** Keeps riding round at a distance. */
		RIDE,
		/** Lowers the lance and gallops at the target. */
		CHARGE,
		/** Throws his lance. */
		THROW
	}

	private LostCavalryLogic() {
	}

	/**
	 * The movement speed attribute that moves a mob {@code multiple} times as fast as a walking player. A mob moves its
	 * speed squared each tick where a player moves his speed, so ground speed grows with the square of the attribute.
	 */
	public static double speedOfPlayerWalk(double multiple) {
		return Math.sqrt(PLAYER_WALK * multiple);
	}

	/** The movement speed attribute that moves a mob {@code multiple} times as fast as a walking skeleton. */
	public static double speedOfSkeletonWalk(double multiple) {
		return SKELETON_WALK * Math.sqrt(multiple);
	}

	/** The speed modifier for navigation that moves a mob {@code multiple} times as fast as its own attribute does. */
	public static double modifierFor(double multiple) {
		return Math.sqrt(multiple);
	}

	/**
	 * The axe move to start, or {@code null} while the target is out of reach. Against a raised or ready shield he
	 * mostly cuts plainly to knock it away, sometimes strikes heavily through it or sweeps round it; against a target
	 * without a shield he cuts twice in quick succession.
	 *
	 * @param shieldReady whether the target has a shield it can raise (held, and not knocked out)
	 */
	public static @Nullable FootAttack chooseFoot(double distance, boolean shieldReady, int heavyCooldown, int doubleCooldown,
			int sweepCooldown, SkeletonKnightSettings axe, LostCavalrySettings.Moves moves, double heavyRoll, double moveRoll) {
		if (distance > axe.reach()) {
			return null;
		}
		if (shieldReady) {
			if (heavyCooldown <= 0 && heavyRoll < axe.heavyChanceVsShield()) {
				return FootAttack.HEAVY;
			}
			if (sweepCooldown <= 0 && moveRoll < moves.sweepChance()) {
				return FootAttack.SWEEP;
			}
			return FootAttack.SLASH;
		}
		if (doubleCooldown <= 0 && moveRoll < moves.doubleChance()) {
			return FootAttack.DOUBLE;
		}
		if (heavyCooldown <= 0 && heavyRoll < axe.heavyChance()) {
			return FootAttack.HEAVY;
		}
		return FootAttack.SLASH;
	}

	/** Whether he starts running: he has chased out of reach for long enough and the last run is over. */
	public static boolean shouldSprint(int chaseTicks, int sprintCooldown, LostCavalrySettings.Speed speed) {
		return sprintCooldown <= 0 && chaseTicks >= speed.sprintAfter() && speed.sprintTicks() > 0;
	}

	/**
	 * What he does while riding round the player.
	 *
	 * @param distance      flat distance to the target
	 * @param reachable     whether the horse can get to the target (a path, not much higher or lower)
	 * @param chargeCooldown ticks before he may charge
	 * @param throwRoll     random number in [0, 1), or 1 when this is not the once-a-second throw check
	 */
	public static Mounted chooseMounted(double distance, boolean reachable, int chargeCooldown, int throwCooldown, int sparesLeft,
			LostCavalrySettings.Lance lance, LostCavalrySettings.Throwing throwing, double throwRoll) {
		if (reachable && chargeCooldown <= 0 && distance >= lance.minDistance() && distance <= lance.maxDistance()) {
			return Mounted.CHARGE;
		}
		boolean canThrow = sparesLeft > 0 && throwCooldown <= 0 && distance >= throwing.minRange() && distance <= throwing.maxRange();
		// A player the horse cannot get to gets the lance thrown at once; otherwise only now and then between charges.
		if (canThrow && (!reachable ? throwRoll < 1.0 : throwRoll < throwing.chance())) {
			return Mounted.THROW;
		}
		return Mounted.RIDE;
	}

	/** Ticks before his next charge: shorter while he covers an unhorsed partner. */
	public static int chargeCooldown(LostCavalrySettings.Lance lance, boolean covering) {
		return covering ? (int) Math.round(lance.cooldown() * lance.coverMultiplier()) : lance.cooldown();
	}

	/** How far from the player he rides between charges; closer while he covers an unhorsed partner. */
	public static double keepDistance(LostCavalrySettings.Lance lance, boolean covering, double roll) {
		double distance = lance.keepMin() + (lance.keepMax() - lance.keepMin()) * roll;
		return covering ? Math.max(lance.minDistance(), distance * lance.coverMultiplier() * 1.4) : distance;
	}

	/** Damage of a lance strike: a share of it through a shield that covers the target. */
	public static float lanceDamage(LostCavalrySettings.Lance lance, boolean blocked) {
		return blocked ? lance.damage() * lance.blockedShare() : lance.damage();
	}
}
