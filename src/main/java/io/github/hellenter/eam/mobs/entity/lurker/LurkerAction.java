package io.github.hellenter.eam.mobs.entity.lurker;

/** What the swamp lurker is doing. Synced to clients, which animate it; the hit ticks are shared with the model. */
public enum LurkerAction {
	NONE(0),
	/** A leap through the air, at the target or away from it; ends on landing. */
	LEAP(60),
	/** Three fast swipes, one on each of {@link #COMBO_HITS}. */
	COMBO(16),
	/** Sinks into the ground under it, then hides there. */
	BURROW(12);

	public static final int[] COMBO_HITS = {3, 7, 11};

	private final int length;

	LurkerAction(int length) {
		this.length = length;
	}

	/** Length in ticks; a leap may end sooner, when it lands. */
	public int length() {
		return length;
	}

	public static LurkerAction byId(int id) {
		LurkerAction[] values = values();
		return id >= 0 && id < values.length ? values[id] : NONE;
	}
}
