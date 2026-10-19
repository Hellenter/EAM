package io.github.hellenter.eam.client.mobs.leshy;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class LeshyRootRenderState extends EntityRenderState {
	public float yRot;
	/** How far out of the ground it stands, 0 to 1. */
	public float rise;
	/** Ticks since it burst out, for its writhing. */
	public float upTime;
}
