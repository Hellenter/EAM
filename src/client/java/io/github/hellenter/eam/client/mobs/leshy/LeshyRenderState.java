package io.github.hellenter.eam.client.mobs.leshy;

import io.github.hellenter.eam.mobs.entity.leshy.Leshy;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class LeshyRenderState extends LivingEntityRenderState {
	/** Its claw swing, 0 to 1. */
	public float swing;
	/** What it is doing, and for how many ticks it has been at it. */
	public Leshy.Action action = Leshy.Action.NONE;
	public float actionTime;
	/** Whether it fights rather than haunts. */
	public boolean angry;
}
