package io.github.hellenter.eam.client.mobs.warlock;

import io.github.hellenter.eam.mobs.entity.warlock.WarlockSpell;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class UndeadWarlockRenderState extends LivingEntityRenderState {
	/** The spell in his hands. */
	public WarlockSpell spell = WarlockSpell.NONE;
	/** Ticks into it. */
	public float castTime;
}
