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
	POKE(FallenLordAction.POKE);

	public static final int SLASH_HIT = 5;
	public static final int POKE_HIT = 6;
	public static final int[] COMBO_HITS = {10, 16, 22};
	public static final int COMBO_KICK = 30;
	public static final int COMBO_BACKSTEP = 33;
	public static final int HEAVY_HIT = FallenLordAction.HEAVY_HIT;

	private final FallenLordAction animation;

	KnightAction(FallenLordAction animation) {
		this.animation = animation;
	}

	/** Length in ticks. */
	public int length() {
		return animation.length();
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
