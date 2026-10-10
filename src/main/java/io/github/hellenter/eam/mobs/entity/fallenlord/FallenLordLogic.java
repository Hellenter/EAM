package io.github.hellenter.eam.mobs.entity.fallenlord;

import java.util.ArrayList;
import java.util.Comparator;
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
	/** He hops in at players up to this far away; further, he walks. */
	public static final double HOP_IN_MAX = 16.0;
	/** A hop in leaves him at least this far from the player, so he never lands on them. */
	public static final double HOP_LAND_GAP = 2.0;
	/** He hops back from players this close. */
	public static final double HOP_BACK_MAX = 4.0;
	/** A hop back is this many times likelier while his shield is knocked out. */
	public static final double SHIELD_DOWN_HOP_BONUS = 10.0;
	public static final double BACKSTAB_MIN = 2.0;
	public static final double BACKSTAB_MAX = 8.0;
	/** The backstab ends this far behind the player. */
	public static final double BACKSTAB_BEHIND = 1.6;
	/** On the way he passes the player's side this far out. */
	public static final double BACKSTAB_SIDE = 2.0;
	/** Jumps: a mob's plain jump speed, gravity and vertical air drag per tick, and the fastest jump he makes. */
	private static final double PLAIN_JUMP = 0.42;
	private static final double GRAVITY = 0.08;
	private static final double AIR_DRAG = 0.98;
	private static final double MAX_JUMP = 1.5;
	/** The death burst never has more orbs than this. */
	public static final int MAX_ORBS = 256;

	public enum Attack {
		POKE(1), DASH(1), THROW(1), SWIPE(1), SLASH(2), COMBO(2), LUNGE(2), WHIRL(2), SWORD_DASH(2),
		/** Short hop at the player; both phases. */
		HOP(0),
		/** Short hop away from the player, then a throw or a dash; both phases. */
		HOP_BACK(0),
		BACKSTAB(2), HEAVY(2);

		private final int phase;

		Attack(int phase) {
			this.phase = phase;
		}

		/** The phase that uses it, 0 for both. */
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
	 * @param ticksSinceHit  how long since one of his hits last landed
	 * @param shieldDown     his own shield is knocked out
	 * @param targetBlocking the target holds a shield up
	 */
	public record Situation(double distance, boolean canSee, boolean hasSpear, int ticksOutOfReach, int playersInWhirl, int ticksSinceHit,
			boolean shieldDown, boolean targetBlocking) {
		public Situation(double distance, boolean canSee, boolean hasSpear, int ticksOutOfReach, int playersInWhirl) {
			this(distance, canSee, hasSpear, ticksOutOfReach, playersInWhirl, 0, false, false);
		}
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
		if (phase == 1 && !s.hasSpear()) {
			return list;
		}
		if (phase == 1) {
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
			if (ready(Attack.BACKSTAB, cooldowns) && s.canSee() && d >= BACKSTAB_MIN && d <= BACKSTAB_MAX) {
				list.add(new Candidate(Attack.BACKSTAB, s.targetBlocking() ? settings.backstab().blockingBonus() : 1.0));
			}
			if (ready(Attack.HEAVY, cooldowns) && s.canSee() && d <= SWORD_REACH + settings.heavyStrike().extraReach()) {
				list.add(new Candidate(Attack.HEAVY, 1.0));
			}
		}
		hops(phase, s, cooldowns, settings, list);
		// A hop back is an option beside an attack, not instead of one; with his shield knocked out it is the plan.
		if (list.size() == 1 && list.getFirst().attack() == Attack.HOP_BACK && !(phase == 1 && s.shieldDown())) {
			list.clear();
		}
		return list;
	}

	/**
	 * Hops in at a player who stays away or has not been hit for a while; hops back from one who crowds him when a
	 * throw or a dash is ready to follow, and nearly always while his shield is knocked out.
	 */
	private static void hops(int phase, Situation s, int[] cooldowns, FallenLordSettings settings, List<Candidate> list) {
		FallenLordSettings.Movement movement = settings.movement();
		double d = s.distance();
		if (!s.canSee() || movement.hopMax() <= 0.0) {
			return;
		}
		if (ready(Attack.HOP, cooldowns) && d - movement.hopMin() >= HOP_LAND_GAP && d <= HOP_IN_MAX
				&& (d >= movement.hopInFrom() || s.ticksSinceHit() >= movement.hopInAfter())) {
			list.add(new Candidate(Attack.HOP, 1.0));
		}
		if (ready(Attack.HOP_BACK, cooldowns) && d <= HOP_BACK_MAX) {
			boolean shieldDown = phase == 1 && s.shieldDown();
			if (shieldDown || afterHopBack(phase, s.hasSpear(), cooldowns) != null) {
				list.add(new Candidate(Attack.HOP_BACK, movement.hopBackChance() * (shieldDown ? SHIELD_DOWN_HOP_BONUS : 1.0)));
			}
		}
	}

	/**
	 * What he does on landing from a hop back: throws the spear or dashes in phase one, sword dashes in phase two; null
	 * when none of them is ready. The throw does not wait for the player to keep away.
	 */
	public static Attack afterHopBack(int phase, boolean hasSpear, int[] cooldowns) {
		if (phase == 1) {
			if (!hasSpear) {
				return null;
			}
			if (ready(Attack.THROW, cooldowns)) {
				return Attack.THROW;
			}
			return ready(Attack.DASH, cooldowns) ? Attack.DASH : null;
		}
		return ready(Attack.SWORD_DASH, cooldowns) ? Attack.SWORD_DASH : null;
	}

	/**
	 * How far a hop goes: {@code roll} picks between the shortest and the longest; a hop in also stops
	 * {@link #HOP_LAND_GAP} short of the player.
	 *
	 * @param roll uniform random in [0, 1)
	 */
	public static double hopLength(boolean towards, double distance, FallenLordSettings.Movement movement, double roll) {
		double min = Math.min(movement.hopMin(), movement.hopMax());
		double length = min + roll * (Math.max(movement.hopMin(), movement.hopMax()) - min);
		return towards ? Math.max(0.0, Math.min(length, distance - HOP_LAND_GAP)) : length;
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
			case HOP, HOP_BACK -> settings.movement().hopCooldown();
			case BACKSTAB -> settings.backstab().cooldown();
			case HEAVY -> settings.heavyStrike().cooldown();
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

	/**
	 * Where the backstab goes: first out to the player's side, then to their back, as seen from where the player
	 * faces now. Positions are horizontal, x and z.
	 *
	 * @param playerYaw the way the player faces, degrees in the Minecraft convention
	 * @param bossX     the boss's offset from the player; picks the side he passes on
	 * @return side x, side z, back x, back z, all relative to the player
	 */
	public static double[] backstabPath(float playerYaw, double bossX, double bossZ) {
		double yaw = Math.toRadians(playerYaw);
		// Minecraft's facing for a yaw: (-sin, cos).
		double fx = -Math.sin(yaw);
		double fz = Math.cos(yaw);
		// The player's right is (-fz, fx); pass on the side he is already on.
		double side = -fz * bossX + fx * bossZ >= 0.0 ? 1.0 : -1.0;
		double sx = -fz * side * BACKSTAB_SIDE;
		double sz = fx * side * BACKSTAB_SIDE;
		return new double[] {sx, sz, -fx * BACKSTAB_BEHIND, -fz * BACKSTAB_BEHIND};
	}

	/** Experience he drops for this many players around him: one more player adds the same, up to the cap. */
	public static int deathXp(int players, FallenLordSettings.Death death) {
		int counted = Math.min(Math.max(players, 1), death.maxPlayers());
		return death.xp() + death.xpPerPlayer() * (counted - 1);
	}

	/**
	 * Splits the death experience into {@code orbs} orb values, as even as possible, and says how many go to the
	 * inner sphere (the first ones). Every orb is worth at least one point.
	 *
	 * @return the orb values; the first {@link #innerOrbs} of them form the inner sphere
	 */
	public static int[] orbValues(int xp, int orbs) {
		int count = Math.max(1, Math.min(orbs, xp));
		int[] values = new int[count];
		for (int i = 0; i < count; i++) {
			values[i] = xp / count + (i < xp % count ? 1 : 0);
		}
		return values;
	}

	/** Orbs for {@code xp} experience: about {@link FallenLordSettings.Death#orbXp()} each, at most {@link #MAX_ORBS}. */
	public static int orbCount(int xp, FallenLordSettings.Death death) {
		return Math.clamp((xp + death.orbXp() - 1) / death.orbXp(), 1, MAX_ORBS);
	}

	/** How many of {@code orbs} orbs fly into the inner sphere. */
	public static int innerOrbs(int orbs, FallenLordSettings.Death death) {
		return (int) Math.round(orbs * death.innerShare());
	}

	/**
	 * A spear dash multiplier at {@code progress} of the run, 0 at its start and 1 at its end: from {@code start} to
	 * {@code end} evenly.
	 */
	public static double alongDash(double start, double end, double progress) {
		return start + (end - start) * Math.clamp(progress, 0.0, 1.0);
	}

	/** Hits a breather waits for: picked between the least and the most. */
	public static int breatherHits(FallenLordSettings.Breather breather, double roll) {
		int min = Math.min(breather.hitsMin(), breather.hitsMax());
		int max = Math.max(breather.hitsMin(), breather.hitsMax());
		return Math.min(max, min + (int) (roll * (max - min + 1)));
	}

	/**
	 * Whether his hits on one player within the breather's window are many: at least {@code hitsNeeded} of them, or
	 * together at least {@link FallenLordSettings.Breather#health()} of the player's health.
	 *
	 * @param blows the hits, each with the health it took
	 */
	public static boolean needsBreather(List<Wound> blows, long now, int hitsNeeded, FallenLordSettings.Breather breather) {
		int hits = 0;
		float lost = 0.0F;
		for (Wound blow : blows) {
			if (now - blow.tick() <= breather.window()) {
				hits++;
				lost += blow.amount();
			}
		}
		return hits > 0 && (hits >= hitsNeeded || lost >= breather.health());
	}

	/** Chance he leaps back after {@code attacks} attacks in a row. */
	public static double leapBackChance(int attacks, FallenLordSettings.LeapBack leapBack) {
		return Math.clamp((attacks - leapBack.after() + 1) * leapBack.chanceStep(), 0.0, 1.0);
	}

	/**
	 * Whether a player whose feet are {@code rise} blocks above his and {@code flat} blocks away along the ground is
	 * out of his reach but close enough to leap at.
	 */
	public static boolean shouldLeapUp(double rise, double flat, FallenLordSettings.LeapUp leapUp) {
		return rise >= leapUp.minHeight() && rise <= leapUp.maxHeight() && flat <= leapUp.maxRange();
	}

	/**
	 * Share of the leap up done {@code ticks} ticks after he springs off, out of {@code rise} ticks: upwards it is fast
	 * at first and slows to the top, so he clears the edge of a pillar before he is over it.
	 */
	public static double leapUpHeight(int ticks, int rise) {
		double done = Math.clamp((double) ticks / rise, 0.0, 1.0);
		return 1.0 - (1.0 - done) * (1.0 - done);
	}

	/**
	 * Upward speed a jump needs to rise {@code height} blocks, under mob gravity and air drag: at least the plain
	 * jump's.
	 */
	public static double jumpSpeed(double height) {
		for (double speed = PLAIN_JUMP; ; speed += 0.01) {
			double y = 0.0;
			double v = speed;
			while (v > 0.0) {
				y += v;
				v = (v - GRAVITY) * AIR_DRAG;
			}
			if (y >= height || speed >= MAX_JUMP) {
				return speed;
			}
		}
	}

	/**
	 * Whether the player gave up on the lure of his stance: ran off by {@link FallenLordSettings.Stance#runAway()} from
	 * where they stood when it began, or started to heal.
	 *
	 * @param eating the player is eating or drinking something
	 */
	public static boolean stanceBroken(double distance, double startDistance, float health, float startHealth, boolean eating,
			FallenLordSettings.Stance stance) {
		return eating || distance >= startDistance + stance.runAway() || health >= startHealth + stance.healed();
	}

	/**
	 * Whether the player came at him in his stance: into sword reach, or nearer than when it began by
	 * {@link FallenLordSettings.Stance#approach()} and within {@link FallenLordSettings.Stance#lungeFrom()}.
	 */
	public static boolean stanceApproached(double distance, double startDistance, FallenLordSettings.Stance stance) {
		return distance <= SWORD_REACH + 0.2 || distance <= stance.lungeFrom() && distance <= startDistance - stance.approach();
	}

	/**
	 * Ticks the target has kept out of his reach, counting this one: back to 0 once they are within his weapon's reach,
	 * held while he is busy with an action.
	 */
	public static int chaseTicks(int ticks, double distance, int phase, boolean busy) {
		double reach = phase == 1 ? POKE_REACH : SWORD_REACH;
		if (distance <= reach + 0.5) {
			return 0;
		}
		return busy ? ticks : ticks + 1;
	}

	/** Obsidian's hardness: in his room he breaks every block softer than this. */
	public static final float OBSIDIAN_HARDNESS = 50.0F;

	/** In his boss room he breaks any block that can be broken at all and is softer than obsidian. */
	public static boolean breakableInArena(float hardness) {
		return hardness >= 0.0F && hardness < OBSIDIAN_HARDNESS;
	}

	/** A stretch of a dash's run, in blocks along its direction. */
	public record Span(double from, double to) {
	}

	/**
	 * The stretch of the run a smashed block fills: a cube whose centre lies at {@code along}, measured along the
	 * horizontal dash direction ({@code dirX}, {@code dirZ}, a unit vector). Crossed corner to corner it is longer.
	 */
	public static Span blockSpan(double along, double dirX, double dirZ) {
		double half = 0.5 * (Math.abs(dirX) + Math.abs(dirZ));
		return new Span(along - half, along + half);
	}

	/**
	 * Whether a smashed block lies in his own path and so adds to how deep the dash has gone, rather than being a side
	 * block his wide body clips: its centre is at most half a block outside his body, sideways from the run.
	 */
	public static boolean inDashPath(double sideways, double halfWidth) {
		return Math.abs(sideways) <= halfWidth + 0.5;
	}

	/** How deep into blocks one dash has gone: the length the spans cover together, overlaps counted once. */
	public static double dashDepth(List<Span> spans) {
		List<Span> sorted = new ArrayList<>(spans);
		sorted.sort(Comparator.comparingDouble(Span::from));
		double depth = 0.0;
		double end = Double.NEGATIVE_INFINITY;
		for (Span span : sorted) {
			if (span.to() <= end) {
				continue;
			}
			depth += span.to() - Math.max(span.from(), end);
			end = span.to();
		}
		return depth;
	}

	/** After a passive player broke his stance he sword dashes at once if they are in its range. */
	public static boolean dashFromStance(double distance) {
		return distance >= SWORD_REACH && distance <= SWORD_DASH_MAX;
	}

	/** The bonuses for this many players; less than one counts as one, past the last level the last one holds. */
	public static FallenLordSettings.PlayerScaling.Level scaling(int players, FallenLordSettings.PlayerScaling scaling) {
		List<FallenLordSettings.PlayerScaling.Level> levels = scaling.levels();
		return levels.get(Math.min(Math.max(players, 1), levels.size()) - 1);
	}
}
