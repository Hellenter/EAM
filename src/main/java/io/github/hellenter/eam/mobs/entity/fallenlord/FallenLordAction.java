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
	/** Run in, stab, hop back. */
	LUNGE(20),
	/** Sword spin that hits everyone around. */
	WHIRL(16),
	/** Leap back from the player before drinking. */
	RETREAT(10),
	/** Drinks a regeneration potion, healing; ends early once healed enough or when a player hits him. */
	DRINK(400),
	/** Quick half-circle bash at a player who hit him during the drink: the shield, or the sword in phase two. */
	COUNTER(10);

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
