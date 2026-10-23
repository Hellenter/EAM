package io.github.hellenter.eam.client.mobs.shambler;

import net.minecraft.client.renderer.entity.state.ZombieRenderState;

public class ShamblerRenderState extends ZombieRenderState {
	/** How far it is crouched into a mushroom: 0 standing, 1 a mushroom. */
	public float disguise;
	/** The charred kind from the burnt forest. */
	public boolean smoldering;
}
