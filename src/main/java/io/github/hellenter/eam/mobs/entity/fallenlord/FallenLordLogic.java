package io.github.hellenter.eam.mobs.entity.fallenlord;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure decisions of the Fallen Lord: which attack to use, when the shield blocks, when the swipe answers a flurry,
 * when he drinks, when he leaps back from a burst of damage, when he knocks a player's shield out, how he scales
 * with players.
 */
public final class FallenLordLogic {
	/** Spear jab reach, from the boss's centre to the target's. */
	public static final double POKE_REACH = 3.27;
	public static final double DASH_MIN = 3.0;
	public static final double DASH_MAX = 12.0;
	/** The dash carries him this far past where the player stood, so stepping back does not dodge it. */
	public static final double DASH_OVERSHOOT = 4.0;
	/** The dash hits whoever is this close to him on the way. */
	public static final double DASH_HIT_RADIUS = 2.2;
	public static final double THROW_MIN = 4.5;
	public static final double THROW_MAX = 28.0;
	/** Swipe and sword reach. */
	public static final double SWORD_REACH = 3.2;
	public static final double COMBO_MAX = 7.0;
	public static final double LUNGE_MIN = 2.5;
	public static final double LUNGE_MAX = 9.0;
	public static final double SWORD_DASH_MIN = 3.5;
	public static final double SWORD_DASH_MAX = 10.0;
	/** The sword dash stops this close to the player, centre to centre: point-blank. */
	public static final double SWORD_DASH_STOP = 1.5;
	/** Reach of the half-circle bash that answers a hit during the drink. */
	public static final double COUNTER_REACH = 3.5;
	/** A player this close counts as having come within reach, which resets the spear-throw timer. */
	public static final double CLOSE = THROW_MIN;
	/** Shield blocks hits from this half-angle in front of the body, in degrees. */
	public static final double SHIELD_HALF_ANGLE = 100.0;
	/** Whirl is this many times more likely with two or more players around. */
	public static final double CROWD_BONUS = 4.0;

	public enum Attack {
		POKE(1), DASH(1), THROW(1), SWIPE(1), SLASH(2), COMBO(2), LUNGE(2), WHIRL(2), SWORD_DASH(2);

		private final int phase;

		Attack(int phase) {
			this.phase = phase;
		}

		public int phase() {
			return phase;
		}
	}

	/**
	 * What the boss sees when picking an attack.
	 *
	 * @param distance       horizontal distance to the target
	 * @param canSee         line of sight to the target
	 * @param hasSpear       the spear is in hand, not in flight
	 * @param ticksOutOfReach how long no target came within {@link #CLOSE}
	 * @param playersInWhirl players within whirl radius
	 */
	public record Situation(double distance, boolean canSee, boolean hasSpear, int ticksOutOfReach, int playersInWhirl) {
	}

	/** Hits landed on the boss while his shield is down, counted for the swipe. */
	public record HitStreak(int hits, long firstTick) {
		public static final HitStreak NONE = new HitStreak(0, 0L);
	}

	/**
	 * His hits one player's shield has blocked; the shield is knocked out at {@code needed}.
	 *
	 * @param lastTick game time of the latest blocked hit
	 */
	public record BlockStreak(int hits, long lastTick, int needed) {
		public static final BlockStreak NONE = new BlockStreak(0, 0L, 0);
	}

	/** Health a hit took from the boss, and when. */
	public record Wound(long tick, float amount) {
	}

	record Candidate(Attack attack, double weight) {
	}

	private FallenLordLogic() {
	}

	/**
	 * Picks the next attack, or null when none fits. Attacks off cooldown and in range are weighted; the last attack
	 * gets {@link FallenLordSettings#repeatPenalty()} so the boss mixes them. The plain sword hit is only a fallback.
	 *
	 * @param cooldowns remaining ticks per attack, by ordinal
	 * @param roll      uniform random in [0, 1)
	 */
	public static Attack choose(int phase, Situation situation, int[] cooldowns, Attack last, FallenLordSettings settings, double roll) {
		List<Candidate> candidates = candidates(phase, situation, cooldowns, settings);
		if (candidates.isEmpty()) {
			return phase == 2 && ready(Attack.SLASH, cooldowns) && situation.canSee() && situation.distance() <= SWORD_REACH
					? Attack.SLASH : null;
		}
		double total = 0.0;
		double[] weights = new double[candidates.size()];
		for (int i = 0; i < candidates.size(); i++) {
			Candidate candidate = candidates.get(i);
			double weight = candidate.weight();
			if (candidate.attack() == last && candidates.size() > 1) {
				weight *= settings.repeatPenalty();
			}
			weights[i] = weight;
			total += weight;
		}
		double pick = roll * total;
		for (int i = 0; i < weights.length; i++) {
			pick -= weights[i];
			if (pick < 0.0) {
				return candidates.get(i).attack();
			}
		}
		return candidates.getLast().attack();
	}

	static List<Candidate> candidates(int phase, Situation s, int[] cooldowns, FallenLordSettings settings) {
		List<Candidate> list = new ArrayList<>();
		double d = s.distance();
		if (phase == 1) {
			if (!s.hasSpear()) {
				return list;
			}
			if (ready(Attack.POKE, cooldowns) && s.canSee() && d <= POKE_REACH) {
				list.add(new Candidate(Attack.POKE, 1.0));
			}
			if (ready(Attack.DASH, cooldowns) && s.canSee() && d >= DASH_MIN && d <= DASH_MAX) {
				list.add(new Candidate(Attack.DASH, 1.0));
			}
			if (ready(Attack.THROW, cooldowns) && s.canSee() && d >= THROW_MIN && d <= THROW_MAX
					&& s.ticksOutOfReach() >= settings.phaseOne().throwAfter()) {
				// Throwing is his answer to a player who keeps away: it wins over the dash most of the time.
				list.add(new Candidate(Attack.THROW, 3.0));
			}
		} else {
			if (ready(Attack.COMBO, cooldowns) && s.canSee() && d <= COMBO_MAX) {
				list.add(new Candidate(Attack.COMBO, 1.0));
			}
			if (ready(Attack.LUNGE, cooldowns) && s.canSee() && d >= LUNGE_MIN && d <= LUNGE_MAX) {
				list.add(new Candidate(Attack.LUNGE, 1.0));
			}
			if (ready(Attack.WHIRL, cooldowns) && s.canSee() && d <= settings.phaseTwo().whirlRadius()) {
				list.add(new Candidate(Attack.WHIRL, s.playersInWhirl() >= 2 ? CROWD_BONUS : 1.0));
			}
			if (ready(Attack.SWORD_DASH, cooldowns) && s.canSee() && d >= SWORD_DASH_MIN && d <= SWORD_DASH_MAX) {
				list.add(new Candidate(Attack.SWORD_DASH, 1.0));
			}
		}
		return list;
	}

	private static boolean ready(Attack attack, int[] cooldowns) {
		return cooldowns[attack.ordinal()] <= 0;
	}

	/** Ticks until the attack can be used again. */
	public static int cooldown(Attack attack, FallenLordSettings settings) {
		FallenLordSettings.PhaseOne one = settings.phaseOne();
		FallenLordSettings.PhaseTwo two = settings.phaseTwo();
		return switch (attack) {
			case POKE -> one.pokeCooldown();
			case DASH -> one.dashCooldown();
			case THROW -> one.throwCooldown();
			case SWIPE -> 0;
			case SLASH -> two.slashCooldown();
			case COMBO -> two.comboCooldown();
			case LUNGE -> two.lungeCooldown();
			case WHIRL -> two.whirlCooldown();
			case SWORD_DASH -> settings.swordDash().cooldown();
		};
	}

	/**
	 * Whether a shield held by a body facing {@code bodyYaw} (degrees, Minecraft convention) covers an attacker at
	 * offset ({@code dx}, {@code dz}) from it.
	 */
	public static boolean shieldCovers(float bodyYaw, double dx, double dz) {
		if (dx * dx + dz * dz < 1.0E-6) {
			return true;
		}
		double yawToAttacker = Math.toDegrees(Math.atan2(dz, dx)) - 90.0;
		double diff = ((yawToAttacker - bodyYaw) % 360.0 + 540.0) % 360.0 - 180.0;
		return Math.abs(diff) <= SHIELD_HALF_ANGLE;
	}

	/** Ticks the shield stays down after an axe that disables a player's shield for {@code seconds}. */
	public static int shieldDisableTicks(float seconds, float multiplier) {
		return Math.max(0, Math.round(seconds * multiplier * 20.0F));
	}

	/** Adds a hit at {@code tick} to the streak; hits older than the window start a new streak. */
	public static HitStreak addHit(HitStreak streak, long tick, int window) {
		if (streak.hits() == 0 || tick - streak.firstTick() > window) {
			return new HitStreak(1, tick);
		}
		return new HitStreak(streak.hits() + 1, streak.firstTick());
	}

	public static boolean shouldSwipe(HitStreak streak, FallenLordSettings settings) {
		return streak.hits() >= settings.phaseOne().swipeHits();
	}

	/**
	 * Whether he breaks off to drink: hurt to {@link FallenLordSettings.Regen#healthBelow()} of the phase's health,
	 * drinks left this phase and the last one at least {@link FallenLordSettings.Regen#cooldown()} ago.
	 *
	 * @param drinkCooldown ticks until he may drink again
	 */
	public static boolean shouldDrink(float health, float maxHealth, int drinksThisPhase, int drinkCooldown, FallenLordSettings settings) {
		FallenLordSettings.Regen regen = settings.regen();
		return drinkCooldown <= 0 && drinksThisPhase < regen.usesPerPhase() && regen.maxHeal() > 0.0F
				&& health <= maxHealth * regen.healthBelow();
	}

	/**
	 * Whether the wounds of the last {@link FallenLordSettings.Burst#window()} ticks took at least
	 * {@link FallenLordSettings.Burst#healthShare()} of his health. Healing in between does not count against them.
	 */
	public static boolean lostQuickly(List<Wound> wounds, long now, float maxHealth, FallenLordSettings.Burst burst) {
		float lost = 0.0F;
		for (Wound wound : wounds) {
			if (now - wound.tick() <= burst.window()) {
				lost += wound.amount();
			}
		}
		return lost > 0.0F && lost >= maxHealth * burst.healthShare();
	}

	/**
	 * Adds a hit blocked by a player's shield at {@code tick}. A new count (the first hit, or one after the window)
	 * picks how many hits this shield takes.
	 *
	 * @param roll uniform random in [0, 1)
	 */
	public static BlockStreak addBlock(BlockStreak streak, long tick, FallenLordSettings.ShieldBreak settings, double roll) {
		if (streak.hits() == 0 || tick - streak.lastTick() > settings.window()) {
			int min = Math.min(settings.hitsMin(), settings.hitsMax());
			int max = Math.max(settings.hitsMin(), settings.hitsMax());
			int needed = Math.min(max, min + (int) (roll * (max - min + 1)));
			return new BlockStreak(1, tick, needed);
		}
		return new BlockStreak(streak.hits() + 1, tick, streak.needed());
	}

	public static boolean breaksShield(BlockStreak streak) {
		return streak.hits() > 0 && streak.hits() >= streak.needed();
	}

	/** The bonuses for this many players; less than one counts as one, past the last level the last one holds. */
	public static FallenLordSettings.PlayerScaling.Level scaling(int players, FallenLordSettings.PlayerScaling scaling) {
		List<FallenLordSettings.PlayerScaling.Level> levels = scaling.levels();
		return levels.get(Math.min(Math.max(players, 1), levels.size()) - 1);
	}
}
