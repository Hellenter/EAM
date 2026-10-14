package io.github.hellenter.eam.mobs.entity.duergar;

/**
 * What a renegade is busy with, synced so the client can pose him. Tick numbers count from the start of the action.
 */
public enum RenegadeAction {
	NONE(0),
	/** Weapon raised over his head, then brought down at {@link #CHARGE_HIT}. */
	CHARGE(24),
	/** Takes out the bomb and the flint and steel, strikes, the fuse catches, he lobs it at {@link #BOMB_THROW}. */
	BOMB(40),
	/** Grabs a squad mate, lifts him over his head and hurls him at {@link #ALLY_THROW_RELEASE}. */
	ALLY_THROW(30),
	/** Lifted over a squad mate's head; ends when he is thrown or put down. */
	HELD(200),
	/** Thrown through the air; ends when he lands. */
	FLYING(100);

	public static final int CHARGE_HIT = 16;
	public static final int BOMB_TAKE_OUT = 8;
	public static final int BOMB_STRIKE = 12;
	public static final int BOMB_LIT = 18;
	public static final int BOMB_SWING = 28;
	public static final int BOMB_THROW = 34;
	public static final int ALLY_LIFT = 6;
	public static final int ALLY_THROW_RELEASE = 24;

	private final int length;

	RenegadeAction(int length) {
		this.length = length;
	}

	/** Ticks the action lasts at most. */
	public int length() {
		return length;
	}

	static RenegadeAction byId(int id) {
		RenegadeAction[] values = values();
		return id >= 0 && id < values.length ? values[id] : NONE;
	}
}
