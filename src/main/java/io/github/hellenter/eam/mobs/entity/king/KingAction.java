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
	/** Eyes flash red, both axes drawn back: the time to step away. Then {@link #WHIRL}. */
	WHIRL_WINDUP(16),
	/** A quick full turn on the spot; each axe cuts everyone around once, on {@link #WHIRL_HITS}. Shields don't stop it. */
	WHIRL(12),
	/** Spins up, then six weak cuts all around on {@link #TORNADO_HITS} while drifting at the target. */
	TORNADO(42),
	/** Left axe drawn back, thrown on {@link #THROW_RELEASE}. */
	THROW(16),
	/** Parried at the start of a combo: thrown off balance, open to hits. */
	STAGGER(24),
	/** His totem breaks instead of him: invulnerable for the moment, healed. */
	TOTEM(30);

	public static final int SLASH_HIT = 7;
	public static final int COMBO_PARRY_END = 10;
	public static final int[] COMBO_HITS = {10, 16, 22, 28};
	public static final int[] WHIRL_HITS = {4, 8};
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
