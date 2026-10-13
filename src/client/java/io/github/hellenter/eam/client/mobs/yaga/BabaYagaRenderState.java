package io.github.hellenter.eam.client.mobs.yaga;

import io.github.hellenter.eam.mobs.entity.yaga.YagaAction;
import io.github.hellenter.eam.mobs.entity.yaga.YagaGlow;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

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
	/** Her fire beam, from her feet: the staff's head and where it hits; null while it is not burning. */
	public @Nullable Vec3 beamFrom;
	public @Nullable Vec3 beamTo;
}
