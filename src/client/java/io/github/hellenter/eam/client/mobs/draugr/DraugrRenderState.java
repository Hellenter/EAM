package io.github.hellenter.eam.client.mobs.draugr;

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

public class DraugrRenderState extends HumanoidRenderState {
	/** The gear he wears, packed as {@code DraugrGear} packs it. */
	public int gear;
	/** How far he has climbed out of the floor: 0 buried, 1 up. */
	public float rise = 1.0F;
	/** Raised by Koschei: his eyes burn green. */
	public boolean thrall;
}
