package io.github.hellenter.eam.mobs.entity.koschei;

/**
 * What Koschei is doing right now. Synced to clients, which play the matching animation; the length is the same on both
 * sides so the animation ends with the action.
 */
public enum KoscheiAction {
	NONE(0),
	/** One diagonal cut, landing on {@link #STRIKE_HIT}. */
	STRIKE(14),
	/** Two quick cuts, forehand and backhand, on {@link #DOUBLE_HITS}. */
	DOUBLE(20),
	/**
	 * The orange blow: the blade glows orange from the start and he runs at the target, a wide sweep lands on
	 * {@link #ORANGE_HIT}. A raised shield takes it and is knocked out; anything else takes the full blow.
	 */
	ORANGE(21),
	/** The red blow: the blade glows red, a low sweep lands on {@link #RED_HIT}, walking in as he draws it back. A shield only halves it. */
	RED(23),
	/**
	 * The sword raised over the head with a red glow, held, then brought down from {@link #OVERHEAD_DESCENT} to
	 * {@link #OVERHEAD_HIT}. A shield halves it; a hit as the sword starts down parries him.
	 */
	OVERHEAD(33),
	/**
	 * The lunge: drawn back and held still for a moment until {@link #LUNGE_PUSH}, a rush in until {@link #LUNGE_RUSH_END}, the stab on
	 * {@link #LUNGE_STAB}, then a hop back. A hit just before the stab parries him.
	 */
	LUNGE(28),
	/** The right hand thrust out: a blue burst of force on {@link #FORCE_RELEASE} throws whoever is in front far back. */
	FORCE(16),
	/**
	 * The sword levelled at the target like a spear, sparks gathering at its point, the bolt loosed on {@link #BOLT_RELEASE}
	 * and held on the target until {@link #BOLT_END}.
	 */
	BOLT(52),
	/** His eyes flicker purple and blue; at the end he vanishes and stands behind the target. */
	TELEPORT(12),
	/** A short hop to one side or back. */
	HOP(9),
	/** A flick of the sword that knocks an arrow aside. */
	PARRY_ARROW(6),
	/** The sword turns a player's blow aside, held across where the blow comes. */
	BLOCK(8),
	/** A step in and a kick on {@link #KICK_HIT}, the way the Fallen Lord kicks: it knocks a raised shield out. */
	KICK(14),
	/** Parried: thrown off balance and open to hits; then the burst of force. */
	STAGGER(24),
	/** The right hand raised high, dark motes gathering about it; three phantom ravens burst from it on {@link #SUMMON_RELEASE}. */
	SUMMON(22),
	/**
	 * Struck down in the first phase: his sword crumbles into a flock of ravens, they wheel about him and become a staff in
	 * his hand on {@link #TRANSFORM_STAFF}; nothing hurts him meanwhile, and he rises at full health in his second phase.
	 */
	TRANSFORM(60),
	/** The staff levelled at the target, a blue glow at its head; the bolt flies on {@link #STAFF_BOLT_RELEASE}. */
	STAFF_BOLT(16),
	/** A sweep of the staff at one who came close, on {@link #STAFF_STRIKE_HIT}; it throws them back. */
	STAFF_STRIKE(14),
	/** Both hands raised, green souls gathering; three fly out over the arena on {@link #SOULS_RELEASE}. */
	SOULS(24),
	/** The trap sprung: his eyes flicker purple and blue, he vanishes at the end and draugr rise round the target. */
	TRAP(10),
	/** Both hands thrust out, frost gathering; from {@link #STORM_START} a wide icy blast blows from them to the end. */
	SNOWSTORM(72),
	/** The right hand raised, ice gathering about it; the ravens of ice burst from it on {@link #ICE_RAVENS_RELEASE}. */
	ICE_RAVENS(20),
	/** The staff raised high, lightning crawling over its crystal; the ball of lightning breaks from it on {@link #BALL_RELEASE}. */
	BALL(24),
	/** The staff struck down on the ground; the wall of ice rises before the target on {@link #ICE_WALL_RELEASE}. */
	ICE_WALL(10),
	/** The staff struck down on the ground before him; his shield bearers climb out in a row there on {@link #TURTLE_RELEASE}. */
	TURTLE(20),
	/** Arms spread wide, frost gathering about him; his copies of ice stand up at his sides on {@link #ICE_COPIES_RELEASE}. */
	ICE_COPIES(20),
	/**
	 * He rises {@link #DRAIN_RISE} ticks into the air and draws the strength out of one of his draugr, a hand reaching to it;
	 * as long as the draugr lasts, at most the whole length.
	 */
	DRAIN(400),
	/**
	 * Struck down in the second phase: a shell of ice closes round him, the staff snaps in his hand on
	 * {@link #SHATTER_STAFF} and the shell bursts on {@link #SHATTER_BURST}; nothing hurts him meanwhile, and he rises at full
	 * health in his third phase with two daggers.
	 */
	SHATTER(44),
	/** A quick stab with the left dagger, on {@link #DAGGER_STAB_HIT}. */
	DAGGER_STAB(10),
	/** Three cuts, the daggers by turns, on {@link #DAGGER_FLURRY_HITS}. */
	DAGGER_FLURRY(20),
	/** Crouched, then a dash at the target from {@link #DAGGER_DASH_GO}, both blades driven in on {@link #DAGGER_DASH_HIT}. */
	DAGGER_DASH(18),
	/** The daggers crossed and raised; his draugr climb out round the target on {@link #RAISE_RELEASE}. */
	RAISE(24);

	public static final int STRIKE_HIT = 7;
	public static final int[] DOUBLE_HITS = {6, 12};
	public static final int ORANGE_HIT = 12;
	public static final int RED_HIT = 13;
	/** The sword starts down on this tick (the parry window opens) and lands on the hit. */
	public static final int OVERHEAD_DESCENT = 18;
	public static final int OVERHEAD_HIT = 25;
	/** Ticks the lunge holds drawn back before the rush, a beat to read it by. */
	public static final int LUNGE_HOLD = 3;
	public static final int LUNGE_PUSH = 9;
	public static final int LUNGE_RUSH_END = 14;
	/** The rush ends a moment before the stab: a last beat before the blade goes in. */
	public static final int LUNGE_STAB = 16;
	public static final int KICK_HIT = 6;
	public static final int FORCE_RELEASE = 8;
	public static final int BOLT_RELEASE = 18;
	public static final int BOLT_END = 48;
	public static final int SUMMON_RELEASE = 12;
	/** The sword crumbles into ravens on this tick of the transformation, and the staff forms on the second. */
	public static final int TRANSFORM_CRUMBLE = 12;
	public static final int TRANSFORM_STAFF = 40;
	public static final int STAFF_BOLT_RELEASE = 9;
	public static final int STAFF_STRIKE_HIT = 7;
	public static final int SOULS_RELEASE = 14;
	public static final int STORM_START = 12;
	public static final int ICE_RAVENS_RELEASE = 10;
	public static final int BALL_RELEASE = 14;
	public static final int ICE_WALL_RELEASE = 5;
	public static final int TURTLE_RELEASE = 10;
	public static final int ICE_COPIES_RELEASE = 12;
	public static final int DRAIN_RISE = 16;
	public static final int SHATTER_STAFF = 14;
	public static final int SHATTER_BURST = 30;
	public static final int DAGGER_STAB_HIT = 5;
	public static final int[] DAGGER_FLURRY_HITS = {5, 10, 15};
	public static final int DAGGER_DASH_GO = 7;
	public static final int DAGGER_DASH_HIT = 12;
	public static final int RAISE_RELEASE = 14;

	private final int length;

	KoscheiAction(int length) {
		this.length = length;
	}

	/** Length in ticks. */
	public int length() {
		return length;
	}

	/** The tick a parryable action can be parried from, for a window of {@code window} ticks; -1 when it can't be. */
	public int parryStart(int window) {
		return switch (this) {
			case LUNGE -> LUNGE_STAB - window;
			case OVERHEAD -> OVERHEAD_HIT - window;
			default -> -1;
		};
	}

	/** The tick its parry window closes, exclusive: the blow lands. */
	public int parryEnd() {
		return switch (this) {
			case LUNGE -> LUNGE_STAB;
			case OVERHEAD -> OVERHEAD_HIT;
			default -> -1;
		};
	}

	public static KoscheiAction byId(int id) {
		KoscheiAction[] values = values();
		return id >= 0 && id < values.length ? values[id] : NONE;
	}
}
