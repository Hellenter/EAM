package io.github.hellenter.eam.mobs.entity.king;

/**
 * What the Pillager King is doing right now. Synced to clients, which play the matching animation; the length is the
 * same on both sides so the animation ends with the action.
 */
public enum KingAction {
	NONE(0),
	/** One overhead cut with the right axe, landing on {@link #SLASH_HIT}. */
	SLASH(14),
	/**
	 * Both axes raised, then four cuts on {@link #COMBO_HITS}, right and left in turn. A player's hit before the first
	 * cut ({@link #COMBO_PARRY_END}) parries him.
	 */
	COMBO(36),
	/**
	 * The onslaught: he rushes at the target, kicks it back on {@link #ONSLAUGHT_KICK}, leaps after it with both axes
	 * raised on {@link #ONSLAUGHT_LEAP} and brings them down on {@link #ONSLAUGHT_HIT}. A shield only softens the blow;
	 * a player's hit while he is in the air ({@link #ONSLAUGHT_PARRY_START} to {@link #ONSLAUGHT_PARRY_END}) parries him.
	 */
	ONSLAUGHT(30),
	/** Eyes flash red, axes drawn back, feet set: the time to step away. Then {@link #CHARGE}. */
	CHARGE_WINDUP(14),
	/** Runs at the target; reaching it, he goes on to {@link #CHARGE_HITS}. */
	CHARGE(12),
	/** Three quick cuts on {@link #CHARGE_CUTS}, then a kick on {@link #CHARGE_KICK}. */
	CHARGE_HITS(24),
	/** Spins up, then six weak cuts all around on {@link #TORNADO_HITS} while drifting at the target. */
	TORNADO(42),
	/** Left axe drawn back, thrown on {@link #THROW_RELEASE}. */
	THROW(16),
	/** Left axe swung out flat and thrown on {@link #THROW_RELEASE}; it curves round the target and comes in from behind. */
	BOOMERANG(16),
	/** Knocks an arrow aside with an axe. */
	PARRY_ARROW(6),
	/** Parried at the start of a combo: thrown off balance, open to hits. */
	STAGGER(24),
	/** His totem breaks instead of him: invulnerable for the moment, healed. */
	TOTEM(30);

	public static final int SLASH_HIT = 7;
	public static final int COMBO_PARRY_END = 10;
	public static final int[] COMBO_HITS = {10, 16, 22, 28};
	public static final int ONSLAUGHT_RUSH_END = 6;
	public static final int ONSLAUGHT_KICK = 7;
	public static final int ONSLAUGHT_LEAP = 10;
	public static final int ONSLAUGHT_PARRY_START = 10;
	public static final int ONSLAUGHT_PARRY_END = 19;
	public static final int ONSLAUGHT_HIT = 20;
	public static final int[] CHARGE_CUTS = {3, 7, 11};
	public static final int CHARGE_KICK = 17;
	/** The tornado spins up until this tick. */
	public static final int TORNADO_SPIN_UP = 8;
	public static final int[] TORNADO_HITS = {12, 17, 22, 27, 32, 37};
	public static final int THROW_RELEASE = 8;

	private final int length;

	KingAction(int length) {
		this.length = length;
	}

	/** Length in ticks. */
	public int length() {
		return length;
	}

	public static KingAction byId(int id) {
		KingAction[] values = values();
		return id >= 0 && id < values.length ? values[id] : NONE;
	}
}
