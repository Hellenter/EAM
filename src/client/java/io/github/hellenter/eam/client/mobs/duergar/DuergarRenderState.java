package io.github.hellenter.eam.client.mobs.duergar;

import io.github.hellenter.eam.mobs.entity.duergar.RenegadeAction;

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

public class DuergarRenderState extends HumanoidRenderState {
	/** The gear, beard and hair he wears, packed as {@code DuergarGear} packs them. */
	public int gear;
	/** Which of his kind's skins. */
	public int skin;
	/** A renegade's special attack, posed by the model; {@code NONE} for every other duergar. */
	public RenegadeAction action = RenegadeAction.NONE;
	/** Ticks into that action. */
	public float actionTime;
}
