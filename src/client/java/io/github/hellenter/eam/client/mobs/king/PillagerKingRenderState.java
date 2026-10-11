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

	public enum Eyes {
		NONE,
		RED,
		YELLOW
	}
}
