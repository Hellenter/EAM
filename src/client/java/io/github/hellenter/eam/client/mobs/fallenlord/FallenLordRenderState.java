package io.github.hellenter.eam.client.mobs.fallenlord;

import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLordAction;

import net.minecraft.client.renderer.entity.state.SkeletonRenderState;

public class FallenLordRenderState extends SkeletonRenderState {
	public FallenLordAction action = FallenLordAction.NONE;
	/** Ticks since the action started, with the partial tick. */
	public float actionTime;
	public boolean shieldUp;
	public boolean eyesGlow;
}
