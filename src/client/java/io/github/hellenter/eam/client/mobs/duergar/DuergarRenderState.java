package io.github.hellenter.eam.client.mobs.duergar;

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

public class DuergarRenderState extends HumanoidRenderState {
	/** The gear, beard and hair he wears, packed as {@code DuergarGear} packs them. */
	public int gear;
	/** Which of his kind's skins. */
	public int skin;
}
