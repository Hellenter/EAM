package io.github.hellenter.eam.client.mobs.king;

import io.github.hellenter.eam.mobs.entity.king.KingAction;

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

public class PillagerKingRenderState extends HumanoidRenderState {
	/** What he is doing. */
	public KingAction action = KingAction.NONE;
	/** Ticks into it. */
	public float actionTime;
	/** His eyes glow red before the whirl, yellow while he can be parried; none otherwise. */
	public Eyes eyes = Eyes.NONE;
	/** Degrees per tick he turns on the spot in a whirl or a tornado. */
	public float spinSpeed;
	/** Degrees per tick his body turns as he walks round. */
	public float turnRate;
	/** Blocks per tick he moves up (falling: below zero). */
	public float fallSpeed;
	/** How far into a dash he is, by his speed over the ground: 0 walking, 1 dashing. */
	public float dash;

	public enum Eyes {
		NONE,
		RED,
		YELLOW
	}
}
