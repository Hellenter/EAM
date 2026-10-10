package io.github.hellenter.eam.client.mobs.fallenlord;

import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLordAction;

import net.minecraft.client.renderer.entity.state.SkeletonRenderState;

public class FallenLordRenderState extends SkeletonRenderState {
	public FallenLordAction action = FallenLordAction.NONE;
	/** Ticks since the action started, with the partial tick. */
	public float actionTime;
	public int phase = 1;
	public boolean shieldUp;
	/** How far the shield is raised into his guard, 0 to 1. */
	public float guard;
	/** Frame of the eye flash, or -1 when the eyes do not glow. */
	public int eyeFrame = -1;
	/** Ticks since he died, with the partial tick, or -1 while he lives. */
	public float dying = -1.0F;
}
