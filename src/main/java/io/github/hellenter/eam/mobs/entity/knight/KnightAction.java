package io.github.hellenter.eam.mobs.entity.knight;

import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLordAction;

/**
 * What the skeleton knight is doing. Synced to clients, which play the Fallen Lord's animation of the same name, so the
 * lengths and hit ticks are his.
 */
public enum KnightAction {
	NONE(FallenLordAction.NONE),
	/** One cut, landing on {@link #SLASH_HIT}. */
	SLASH(FallenLordAction.SLASH),
	/** A leap in, three cuts on {@link #COMBO_HITS}, a kick on {@link #COMBO_KICK}, then a hop back. */
	COMBO(FallenLordAction.COMBO),
	/** Sword raised overhead: the time a player has to step back. Then {@link #HEAVY_STRIKE}. */
	HEAVY_WINDUP(FallenLordAction.HEAVY_WINDUP),
	/** The overhead cut, landing on {@link #HEAVY_HIT}. */
	HEAVY_STRIKE(FallenLordAction.HEAVY_STRIKE),
	/** A spear jab, landing on {@link #POKE_HIT}; only the Lost carry spears. */
	POKE(FallenLordAction.POKE),
	/**
	 * The heavy cavalryman's gallop with the lance couched, until it strikes or he gives up; the server ends it. The
	 * cavalryman's own model draws this and the actions below.
	 */
	LANCE_CHARGE(FallenLordAction.NONE, 100),
	/** The cavalryman throws his lance overhand from the saddle, letting go on {@link #LANCE_THROW_RELEASE}. */
	LANCE_THROW(FallenLordAction.THROW),
	/** Two quick axe cuts, right then left, on {@link #AXE_DOUBLE_HITS}. */
	AXE_DOUBLE(FallenLordAction.NONE, 16),
	/** Axe drawn far back with a crouch, then a full turn hitting everyone around on {@link #AXE_SWEEP_HIT}. */
	AXE_SWEEP(FallenLordAction.NONE, 26),
	/** Thrown from his dying horse: he lands, staggers up and draws his axe. */
	DISMOUNT(FallenLordAction.NONE, 24),
	/**
	 * The Lost centurion raises his horn and blows it, calling up more of the Lost: the call sounds on
	 * {@link #HORN_SOUND}, they come on {@link #HORN_CALL}. His own model draws it.
	 */
	HORN(FallenLordAction.NONE, 40),
	/**
	 * The Lost straggler drives his sword into the floor and calls the Lost to him, on {@link #STRAGGLER_CALL_HIT}; drawn
	 * as the Fallen Lord's overhead strike.
	 */
	STRAGGLER_CALL(FallenLordAction.HEAVY_STRIKE);

	public static final int SLASH_HIT = 5;
	public static final int POKE_HIT = 6;
	public static final int[] COMBO_HITS = {10, 16, 22};
	public static final int COMBO_KICK = 30;
	public static final int COMBO_BACKSTEP = 33;
	public static final int HEAVY_HIT = FallenLordAction.HEAVY_HIT;
	public static final int LANCE_THROW_RELEASE = 8;
	public static final int[] AXE_DOUBLE_HITS = {5, 10};
	public static final int AXE_SWEEP_WINDUP = 11;
	public static final int AXE_SWEEP_HIT = 14;
	public static final int HORN_SOUND = 8;
	public static final int HORN_CALL = 20;
	public static final int STRAGGLER_CALL_HIT = FallenLordAction.HEAVY_HIT;

	private final FallenLordAction animation;
	private final int length;

	KnightAction(FallenLordAction animation) {
		this(animation, animation.length());
	}

	KnightAction(FallenLordAction animation, int length) {
		this.animation = animation;
		this.length = length;
	}

	/** Length in ticks. */
	public int length() {
		return length;
	}


	/** The Fallen Lord's action whose animation the client plays. */
	public FallenLordAction animation() {
		return animation;
	}

	public static KnightAction byId(int id) {
		KnightAction[] values = values();
		return id >= 0 && id < values.length ? values[id] : NONE;
	}
}
