package io.github.hellenter.eam.client.mobs.yaga;

import io.github.hellenter.eam.mobs.entity.yaga.YagaAction;
import io.github.hellenter.eam.mobs.entity.yaga.YagaGlow;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class BabaYagaRenderState extends LivingEntityRenderState {
	/** Phase one: standing in her flying mortar. */
	public boolean inMortar = true;
	public YagaAction action = YagaAction.NONE;
	/** Ticks since the current action started, with the partial tick. */
	public float actionTime;
	public YagaGlow glow = YagaGlow.NONE;
	/** Timings from the data file, for the dive and the claws. */
	public int strikeTick;
	public int lunge;
	public int clawGap;
	public int clawHits;
	public int beamWindup;
}
