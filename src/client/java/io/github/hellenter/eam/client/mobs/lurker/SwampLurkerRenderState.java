package io.github.hellenter.eam.client.mobs.lurker;

import io.github.hellenter.eam.mobs.entity.lurker.LurkerAction;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class SwampLurkerRenderState extends LivingEntityRenderState {
	public LurkerAction action = LurkerAction.NONE;
	/** Ticks since the action started, with the partial tick. */
	public float actionTime;
	/** Vertical speed, blocks per tick: rising or falling through a leap. */
	public float verticalSpeed;
}
