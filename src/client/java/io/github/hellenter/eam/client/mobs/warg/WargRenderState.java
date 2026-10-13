package io.github.hellenter.eam.client.mobs.warg;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class WargRenderState extends LivingEntityRenderState {
	/** The pack's leader: bigger, darker. */
	public boolean leader;
	/** Hunting: head low, tail straight out. */
	public boolean angry;
}
