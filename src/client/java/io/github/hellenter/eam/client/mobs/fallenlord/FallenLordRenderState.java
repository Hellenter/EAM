package io.github.hellenter.eam.client.mobs.fallenlord;

import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLordAction;
import io.github.hellenter.eam.mobs.entity.knight.KnightAction;

import net.minecraft.client.renderer.entity.state.SkeletonRenderState;

public class FallenLordRenderState extends SkeletonRenderState {
	public FallenLordAction action = FallenLordAction.NONE;
	/** Ticks since the action started, with the partial tick. */
	public float actionTime;
	public int phase = 1;
	public boolean shieldUp;
	/** How far the shield is raised into his guard, 0 to 1. */
	public float guard;
	/** How far the shield is held lowered in front of the hips, 0 to 1. Only the skeleton knight lowers his. */
	public float shieldLowered;
	/** How far the shield hangs knocked out of the guard, 0 to 1. */
	public float shieldKnocked;
	/** Frame of the eye flash, or -1 when the eyes do not glow. */
	public int eyeFrame = -1;
	/** Frame of the yellow star flashing in his eyes while he can be parried, or -1; drawn over the red flash. */
	public int parryEyeFrame = -1;
	/** Ticks between the combo's cuts: the Fallen Lord's are slower than the skeleton knight's. */
	public int comboGap = 6;
	/** Ticks since he died, with the partial tick, or -1 while he lives. */
	public float dying = -1.0F;
	/** A mob built on the skeleton knight: its own action, for the actions with no Fallen Lord animation. */
	public KnightAction knightAction = KnightAction.NONE;
	/** A Lost cavalryman: how fast his horse moves, 0 to 1. */
	public float mountSpeed;
}
