package io.github.hellenter.eam.mobs.entity.guard;

import io.github.hellenter.eam.mobs.entity.king.KingAction;

/**
 * What a pillager guard is doing besides his plain axe swings. Synced to clients, which play the matching animation.
 * The onslaught keeps the king's timings ({@link KingAction}), so it reads the same.
 */
public enum GuardAction {
	NONE(0),
	/** The king's onslaught with one axe: rush, crouch, leap, blow. A hit while he is in the air parries him. */
	ONSLAUGHT(KingAction.ONSLAUGHT.length()),
	/** The axe drawn back and thrown on {@link #THROW_RELEASE}. */
	THROW(14),
	/** Parried in the air: off balance, open to hits. */
	STAGGER(20);

	public static final int THROW_RELEASE = 8;

	private final int length;

	GuardAction(int length) {
		this.length = length;
	}

	/** Length in ticks. */
	public int length() {
		return length;
	}

	public static GuardAction byId(int id) {
		GuardAction[] values = values();
		return id >= 0 && id < values.length ? values[id] : NONE;
	}
}
