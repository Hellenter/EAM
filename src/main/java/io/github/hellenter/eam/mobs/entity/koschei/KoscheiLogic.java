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
		MAKE_ROOM,
		/** The phantom ravens, his rare pool alongside the bolt. */
		RAVENS
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
		return choose(distance, shieldReady, shieldTicks, lungeFails, cooldowns, last, false, false, settings, rolls);
	}

	/**
	 * As above, with the bolt held back or wanted.
	 *
	 * @param boltBlocked he knocked the target's shield out and it is not about to be ready again: no bolt yet
	 * @param boltHungry  the bolt has been ready a long while without a cast: he leaves raised shields alone (no orange
	 *                    blow, no lunge or kick into one) and shoots more often and from closer
	 */
	public static Choice choose(double distance, boolean shieldReady, int shieldTicks, int lungeFails, int[] cooldowns, @Nullable Attack last,
			boolean boltBlocked, boolean boltHungry, KoscheiSettings.PhaseOne settings, DoubleSupplier rolls) {
		KoscheiSettings.Bolt bolt = settings.bolt();
		// The bolt is his rare pool: never from close by.
		double boltMin = boltHungry ? Math.min(bolt.minRange(), bolt.hungerMinRange()) : bolt.minRange();
		double boltChance = boltHungry ? bolt.chance() * bolt.hungerChance() : bolt.chance();
		if (!boltBlocked && cooldowns[Attack.BOLT.ordinal()] <= 0 && distance >= boltMin && distance <= bolt.maxRange()
				&& rolls.getAsDouble() < boltChance) {
			return new Choice(Attack.BOLT, false);
		}
		// Hungry for the bolt, he leaves a raised shield standing.
		boolean spareShield = boltHungry && shieldReady;
		KoscheiSettings.Lunge lunge = settings.lunge();
		boolean lungeReady = cooldowns[Attack.LUNGE.ordinal()] <= 0 && !spareShield;
		if (distance > settings.reach()) {
			if (lungeReady && distance >= lunge.minRange() && distance <= lunge.maxRange() && rolls.getAsDouble() < lunge.chance()) {
				return new Choice(Attack.LUNGE, false);
			}
			return Choice.WALK;
		}
		KoscheiSettings.Kick kick = settings.kick();
		boolean kickReady = cooldowns[Attack.KICK.ordinal()] <= 0 && distance <= kick.reach() && !spareShield;
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
		return new Choice(melee(shieldReady && !boltHungry, kickReady, cooldowns, last, settings, rolls), failed);
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

	/** Whether the bolt waits at {@code now}: a shield he knocked out is ready at {@code back}, more than {@code lead} ticks off. */
	public static boolean boltBlocked(long now, long back, int lead) {
		return now < back - lead;
	}

	/**
	 * Whether he calls up his phantom ravens on a target this far: his rare pool, only while none of the last flock are
	 * about (they hold the cooldown up) and the target is in range.
	 *
	 * @param cooldown ticks left before he may call them again
	 * @param roll     uniform random in [0, 1)
	 */
	public static boolean callsRavens(double distance, int cooldown, KoscheiSettings.Ravens ravens, double roll) {
		return ravens.count() > 0 && cooldown <= 0 && distance <= ravens.maxRange() && roll < ravens.chance();
	}

	/**
	 * How he answers a target that keeps out of his reach: the bolt when it is ready, not held back, and the target in its
	 * range and in sight; else (null) he stands behind it.
	 *
	 * @param boltCooldown ticks left before he may shoot again
	 */
	public static @Nullable Attack answerRetreat(double distance, boolean sees, int boltCooldown, boolean boltBlocked, KoscheiSettings.Bolt bolt) {
		return sees && boltCooldown <= 0 && !boltBlocked && distance >= bolt.minRange() && distance <= bolt.maxRange() ? Attack.BOLT : null;
	}

	/** Whether the {@code hits}-th hit during the overhead windup is one too many: then the blow can't be parried. */
	public static boolean windupSpammed(int hits, int allowed) {
		return hits > allowed;
	}

	/** How he answers a target out of his reach above him. */
	public enum HeightAnswer {
		/** Vanishes and stands by the target up there. */
		TELEPORT,
		/** The burst of force throws the target off. */
		FORCE,
		/** The bolt holds the target where it stands. */
		BOLT
	}

	/** Whether a target this much higher than him has been out of his reach above long enough for him to answer it. */
	public static boolean outOfReachAbove(double rise, int ticks, KoscheiSettings.Height height) {
		return rise > height.above() && ticks >= height.after();
	}

	/**
	 * What he does about a target out of his reach above him: stands by it if there is room up there and the teleport is
	 * ready, else the burst of force if it reaches, else the bolt; null when none is ready and he waits below.
	 */
	public static @Nullable HeightAnswer answerHeight(boolean teleportReady, boolean roomByTarget, boolean forceReady, boolean forceReaches,
			boolean boltReady, boolean sees) {
		if (teleportReady && roomByTarget) {
			return HeightAnswer.TELEPORT;
		}
		if (forceReady && forceReaches) {
			return HeightAnswer.FORCE;
		}
		return boltReady && sees ? HeightAnswer.BOLT : null;
	}

	// ---- Second phase ----

	/** His attacks with the staff in his second phase. */
	public enum StaffAttack {
		BOLT,
		STRIKE,
		SOULS,
		/** Not an attack yet: he stops backing off and waits for the target to come close, then springs the trap. */
		TRAP,
		BALL,
		STORM,
		ICE_RAVENS,
		ICE_WALL,
		/** Shield bearers rise in a row before him and he hides behind them. */
		TURTLE,
		ICE_COPIES,
		DRAIN
	}

	/**
	 * How things stand when he picks his next staff attack.
	 *
	 * @param thralls     his draugr up now
	 * @param rushing     the target is running at him
	 * @param ballAbout   a ball of lightning of his is about
	 * @param copiesAbout copies of him in ice are about
	 * @param turtling    he is hiding behind a row of his shield bearers
	 * @param canDrain    he may drain a draugr's strength now: hurt enough, uses left and one of his draugr near
	 */
	public record StaffSituation(double distance, boolean sees, int thralls, boolean rushing, boolean ballAbout, boolean copiesAbout,
			boolean turtling, boolean canDrain) {
	}

	/**
	 * What he does next in his second phase, or null to keep moving: the staff blow at a target in its reach; now and then
	 * the drain of strength when he may; behind his shield bearers, only the bolt; else a wall of ice before one running at
	 * him, now and then; else, now and then, the ball of lightning while none of his is about, the ice copies while none
	 * are about, the shield bearers, the souls or the trap while there is room under his cap of draugr, the ice ravens or
	 * the snow storm at a target in their reach; else the bolt at a target in sight.
	 *
	 * @param cooldowns ticks left for each {@link StaffAttack}, by ordinal
	 * @param rolls     random numbers in [0, 1)
	 */
	public static @Nullable StaffAttack chooseStaff(StaffSituation now, int[] cooldowns, PhaseTwoSettings two, DoubleSupplier rolls) {
		double distance = now.distance();
		boolean sees = now.sees();
		if (ready(cooldowns, StaffAttack.STRIKE) && distance <= two.strike().reach()) {
			return StaffAttack.STRIKE;
		}
		if (ready(cooldowns, StaffAttack.DRAIN) && now.canDrain() && rolls.getAsDouble() < two.drain().chance()) {
			return StaffAttack.DRAIN;
		}
		boolean bolt = sees && ready(cooldowns, StaffAttack.BOLT) && distance <= two.bolt().maxRange();
		if (now.turtling()) {
			return bolt ? StaffAttack.BOLT : null;
		}
		PhaseTwoSettings.IceWall wall = two.iceWall();
		if (ready(cooldowns, StaffAttack.ICE_WALL) && now.rushing() && sees && distance >= wall.minDistance() && distance <= wall.maxDistance()
				&& rolls.getAsDouble() < wall.chance()) {
			return StaffAttack.ICE_WALL;
		}
		if (ready(cooldowns, StaffAttack.BALL) && !now.ballAbout() && rolls.getAsDouble() < two.ball().chance()) {
			return StaffAttack.BALL;
		}
		if (ready(cooldowns, StaffAttack.ICE_COPIES) && !now.copiesAbout() && rolls.getAsDouble() < two.iceCopies().chance()) {
			return StaffAttack.ICE_COPIES;
		}
		int room = two.thralls().cap() - now.thralls();
		if (ready(cooldowns, StaffAttack.TURTLE) && room >= two.turtle().minRoom() && rolls.getAsDouble() < two.turtle().chance()) {
			return StaffAttack.TURTLE;
		}
		if (ready(cooldowns, StaffAttack.SOULS) && room > 0 && rolls.getAsDouble() < two.souls().chance()) {
			return StaffAttack.SOULS;
		}
		if (ready(cooldowns, StaffAttack.TRAP) && room > 0 && rolls.getAsDouble() < two.trap().chance()) {
			return StaffAttack.TRAP;
		}
		if (ready(cooldowns, StaffAttack.ICE_RAVENS) && distance <= two.iceRavens().maxRange() && rolls.getAsDouble() < two.iceRavens().chance()) {
			return StaffAttack.ICE_RAVENS;
		}
		if (ready(cooldowns, StaffAttack.STORM) && sees && distance <= two.storm().range() && rolls.getAsDouble() < two.storm().chance()) {
			return StaffAttack.STORM;
		}
		return bolt ? StaffAttack.BOLT : null;
	}

	/** Whether he may drain a draugr's strength at {@code health} of {@code maxHealth}, having done it {@code used} times. */
	public static boolean mayDrain(float health, float maxHealth, int used, PhaseTwoSettings.Drain drain) {
		return used < drain.uses() && health < maxHealth * drain.below();
	}

	/** What he heals in second {@code second} of a drain, counted from 0: less each second, down to the least. */
	public static float drainHeal(int second, PhaseTwoSettings.Drain drain) {
		return Math.max(drain.healMin(), drain.healStart() - drain.healStep() * second);
	}

	private static boolean ready(int[] cooldowns, StaffAttack attack) {
		return cooldowns[attack.ordinal()] <= 0;
	}

	/** Whether a point {@code dx}, {@code dz} from him is within {@code range} and {@code angle} degrees either side of where he faces. */
	public static boolean inCone(double dx, double dz, double facingX, double facingZ, double range, float angle) {
		double length = Math.sqrt(dx * dx + dz * dz);
		if (length > range) {
			return false;
		}
		if (length < 1.0E-4) {
			return true;
		}
		return (dx * facingX + dz * facingZ) / length >= Math.cos(Math.toRadians(angle));
	}

	/** The slowness level after one more ice raven: one more than {@code current} (-1 for none), at most {@code max}. */
	public static int stackedSlowness(int current, int max) {
		return Math.min(max, current + 1);
	}

	/** How many of {@code wanted} draugr he may raise with {@code alive} already up, so as not to pass {@code cap}. */
	public static int thrallsToRaise(int wanted, int alive, int cap) {
		return Math.max(0, Math.min(wanted, cap - alive));
	}

	/** His health through the transformation: from {@code from} up to {@code full} as it goes, full at its end. */
	public static float transformHealth(int t, int length, float from, float full) {
		float progress = Math.clamp((float) t / Math.max(1, length), 0.0F, 1.0F);
		return from + (full - from) * progress;
	}
}
