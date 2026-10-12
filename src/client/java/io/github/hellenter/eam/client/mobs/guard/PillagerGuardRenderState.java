package io.github.hellenter.eam.client.mobs.guard;

import io.github.hellenter.eam.mobs.entity.guard.GuardAction;

import net.minecraft.client.renderer.entity.state.IllagerRenderState;

public class PillagerGuardRenderState extends IllagerRenderState {
	/** What he is doing besides plain swings. */
	public GuardAction action = GuardAction.NONE;
	/** Ticks into it. */
	public float actionTime;
	/** His eyes glow yellow while a hit parries him. */
	public boolean parryEyes;
}
