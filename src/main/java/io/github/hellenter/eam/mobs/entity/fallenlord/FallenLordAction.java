package io.github.hellenter.eam.mobs.entity.fallenlord;

/**
 * What the Fallen Lord is doing right now. Synced to clients, which play the matching animation; the length is the
 * same on both sides so the animation ends with the action.
 */
public enum FallenLordAction {
	NONE(0),
	/** Spear jab when a player comes close. */
	POKE(12),
	/** Eyes flash red, he stands still; then {@link #DASH}. */
	DASH_WINDUP(14),
	/** Unblockable run through where the player stood when it started, a few blocks past it. */
	DASH(22),
	/** Spear thrown at a player who keeps away. */
	THROW(16),
	/** Half-circle spear swing after a flurry of hits on a broken shield; brings the shield back. */
	SWIPE(14),
	/** Phase change: shield dropped, spear thrown, sword drawn. Invulnerable meanwhile. */
	TRANSITION(40),
	/** Plain sword hit. */
	SLASH(10),
	/** Leap, three quick slashes, a kick. */
	COMBO(38),
	/** Draws back until {@link #LUNGE_PUSH}, runs in and stabs, hops back. */
	LUNGE(21),
	/** Sword spin that hits everyone around. */
	WHIRL(16),
	/** Leap back from the player before drinking. */
	RETREAT(10),
	/** Drinks a regeneration potion, healing; ends early once healed enough or when a player hits him. */
	DRINK(400),
	/** Quick half-circle bash at a player who hit him during the drink: the shield, or the sword in phase two. */
	COUNTER(10),
	/** Sword drawn back low, half as long as the red-eyed windup; then {@link #SWORD_DASH}. */
	SWORD_DASH_WINDUP(7),
	/** Rush that stops point-blank in front of the player with a blockable hit; then his combo. */
	SWORD_DASH(14),
	/** A flick of the sword that knocks an arrow aside. */
	PARRY(6),
	/** Short hop at the player or away from him. */
	HOP(9),
	/** Darts round the player's side to their back and cuts there, past a raised shield. */
	BACKSTAB(18),
	/** Sword raised overhead, its edge glowing red; then {@link #HEAVY_STRIKE}. Long enough to step aside out of its wedge. */
	HEAVY_WINDUP(16),
	/** The overhead cut: double damage, more reach, goes through a shield and knocks it out. */
	HEAVY_STRIKE(12),
	/**
	 * Phase two, after a leap back: the fencing stance, sword pointed at the player and the other hand raised behind,
	 * luring them in. The server ends it; this is only the longest it can last.
	 */
	STANCE(400),
	/**
	 * Phase two: a leap up at a player standing too high. Crouches until {@link #LEAP_UP_LAUNCH}, rises beside them
	 * with the sword raised and cuts on {@link #LEAP_UP_CUT}, then falls back.
	 */
	LEAP_UP(24);

	/** Backstab: crouch until this tick, dart to the player's side, then to their back, cut on {@link #BACKSTAB_CUT}. */
	public static final int BACKSTAB_DART = 2;
	public static final int BACKSTAB_BACK = 6;
	public static final int BACKSTAB_CUT = 10;
	/** Lunge: the draw-back ends and the run in starts on this tick; the time a player has to raise a shield. */
	public static final int LUNGE_PUSH = 5;
	/** Heavy strike: the blade starts glowing on this tick of the windup. */
	public static final int HEAVY_GLOW = 3;
	/** Heavy strike: the cut lands on this tick. */
	public static final int HEAVY_HIT = 3;

	/** Leap up: springs off the ground on this tick and cuts at the top of the leap on the other. */
	public static final int LEAP_UP_LAUNCH = 5;
	public static final int LEAP_UP_CUT = 13;

	/** Eyes glow through the whole windup. */
	public static final int EYE_FLASH_TICKS = 14;

	private final int length;

	FallenLordAction(int length) {
		this.length = length;
	}

	/** Length in ticks. */
	public int length() {
		return length;
	}

	public static FallenLordAction byId(int id) {
		FallenLordAction[] values = values();
		return id >= 0 && id < values.length ? values[id] : NONE;
	}
}
