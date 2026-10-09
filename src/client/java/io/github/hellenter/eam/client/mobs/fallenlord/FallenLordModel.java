package io.github.hellenter.eam.client.mobs.fallenlord;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.monster.skeleton.SkeletonModel;
import net.minecraft.util.Mth;

/**
 * Skeleton model with the Fallen Lord's attack animations. Each attack is keyed by ticks since it started, matching
 * the server's timing (hits land on the frames named in {@code FallenLord}). The right arm holds the weapon.
 */
public class FallenLordModel extends SkeletonModel<FallenLordRenderState> {
	private static final float ARM_FORWARD = -(float) Math.PI / 2.0F;

	public FallenLordModel(ModelPart root) {
		super(root);
	}

	@Override
	public void setupAnim(FallenLordRenderState state) {
		super.setupAnim(state);
		float t = state.actionTime;
		switch (state.action) {
			case POKE -> poke(t);
			case DASH_WINDUP -> dashWindup(t);
			case DASH -> dash();
			case THROW -> throwSpear(t, 0.0F);
			case SWIPE -> swipe(t);
			case TRANSITION -> transition(t);
			case SLASH -> slash(t, 0.0F, 1.0F);
			case COMBO -> combo(t);
			case LUNGE -> lunge(t);
			case WHIRL -> whirl(t);
			case RETREAT -> retreat(t);
			case DRINK -> drink(t);
			case COUNTER -> counter(t, state.phase);
			case NONE -> {
			}
		}
	}

	/** 0 before {@code start}, 1 after {@code end}, smooth in between. */
	private static float seg(float t, float start, float end) {
		float p = Mth.clamp((t - start) / (end - start), 0.0F, 1.0F);
		return p * p * (3.0F - 2.0F * p);
	}

	private void poke(float t) {
		float pull = seg(t, 0.0F, 5.0F);
		float push = seg(t, 5.0F, 7.0F);
		float back = seg(t, 8.0F, 12.0F);
		float hold = 1.0F - back;
		rightArm.xRot = Mth.lerp(hold, rightArm.xRot, ARM_FORWARD + 0.1F);
		rightArm.yRot = 0.0F;
		rightArm.z += (3.0F * pull - 9.0F * push) * hold;
		body.yRot = (0.3F * pull - 0.6F * push) * hold;
	}

	private void dashWindup(float t) {
		float crouch = seg(t, 0.0F, 4.0F);
		body.xRot = 0.25F * crouch;
		head.z -= 1.0F * crouch;
		head.xRot -= 0.25F * crouch;
		rightArm.xRot = Mth.lerp(crouch, rightArm.xRot, 0.8F);
		rightArm.z += 2.0F * crouch;
		rightLeg.xRot = -0.4F * crouch;
		leftLeg.xRot = 0.35F * crouch;
	}

	private void dash() {
		body.xRot = 0.3F;
		rightArm.xRot = ARM_FORWARD;
		rightArm.yRot = 0.0F;
		rightArm.z -= 3.0F;
	}

	/** Overhead throw; {@code start} shifts it within a longer action. */
	private void throwSpear(float t, float start) {
		float raise = seg(t, start, start + 7.0F);
		float release = seg(t, start + 7.0F, start + 10.0F);
		float back = seg(t, start + 11.0F, start + 16.0F);
		float x = Mth.lerp(raise, rightArm.xRot, -2.9F);
		x = Mth.lerp(release, x, -1.0F);
		rightArm.xRot = Mth.lerp(back, x, rightArm.xRot);
		rightArm.yRot = 0.0F;
		body.yRot = (-0.4F * raise + 0.7F * release) * (1.0F - back);
	}

	private void swipe(float t) {
		float wind = seg(t, 0.0F, 6.0F);
		float sweep = seg(t, 6.0F, 9.0F);
		float hold = 1.0F - seg(t, 10.0F, 14.0F);
		rightArm.xRot = Mth.lerp(hold * wind, rightArm.xRot, ARM_FORWARD);
		rightArm.yRot = Mth.lerp(sweep, -1.2F * wind, 1.2F) * hold;
		body.yRot = Mth.lerp(sweep, -0.5F * wind, 0.6F) * hold;
	}

	private void transition(float t) {
		// Flings the shield aside.
		float fling = Mth.sin((float) Math.PI * seg(t, 0.0F, 8.0F));
		leftArm.zRot -= 1.3F * fling;
		leftArm.xRot = Mth.lerp(fling, leftArm.xRot, -0.4F);
		// Throws the spear.
		if (t < 20.0F) {
			throwSpear(t, 7.0F);
		} else {
			// Draws the sword from the hip and raises it.
			float reach = seg(t, 20.0F, 26.0F);
			float draw = seg(t, 26.0F, 31.0F);
			float lower = seg(t, 35.0F, 40.0F);
			float x = Mth.lerp(reach, rightArm.xRot, -0.5F);
			x = Mth.lerp(draw, x, -2.6F);
			rightArm.xRot = Mth.lerp(lower, x, rightArm.xRot);
			rightArm.yRot = (0.9F * reach * (1.0F - draw));
			head.xRot -= 0.5F * draw * (1.0F - lower);
		}
	}

	/** One diagonal sword cut; {@code dir} flips it to the other side. */
	private void slash(float t, float start, float dir) {
		float raise = seg(t, start, start + 4.0F);
		float strike = seg(t, start + 4.0F, start + 6.0F);
		float back = seg(t, start + 6.0F, start + 10.0F);
		float x = Mth.lerp(raise, rightArm.xRot, -2.6F);
		x = Mth.lerp(strike, x, -0.3F);
		rightArm.xRot = Mth.lerp(back, x, rightArm.xRot);
		rightArm.yRot = Mth.lerp(strike, -0.5F * dir * raise, 0.6F * dir) * (1.0F - back);
		body.yRot = Mth.lerp(strike, -0.2F * dir * raise, 0.35F * dir) * (1.0F - back);
	}

	private void combo(float t) {
		if (t < 8.0F) {
			// Leap with the sword raised.
			float up = seg(t, 0.0F, 3.0F);
			rightArm.xRot = Mth.lerp(up, rightArm.xRot, -2.7F);
			leftArm.xRot = Mth.lerp(up, leftArm.xRot, -1.2F);
		} else if (t < 26.0F) {
			// Slashes land at 10, 16 and 22, alternating sides.
			int i = Mth.clamp((int) ((t - 6.0F) / 6.0F), 0, 2);
			float hit = 10.0F + 6.0F * i;
			slash(t, hit - 4.5F, i % 2 == 0 ? 1.0F : -1.0F);
		} else {
			// Kick at 30.
			float kick = Mth.sin((float) Math.PI * seg(t, 26.0F, 34.0F));
			rightLeg.xRot = -1.5F * kick;
			body.xRot = -0.2F * kick;
			rightArm.xRot = Mth.lerp(kick, rightArm.xRot, 0.4F);
			leftArm.xRot = Mth.lerp(kick, leftArm.xRot, 0.4F);
		}
	}

	private void lunge(float t) {
		float pull = seg(t, 0.0F, 4.0F);
		float push = seg(t, 4.0F, 7.0F);
		float back = seg(t, 11.0F, 20.0F);
		float hold = 1.0F - back;
		rightArm.xRot = Mth.lerp(hold, rightArm.xRot, ARM_FORWARD + 0.15F);
		rightArm.yRot = 0.0F;
		rightArm.z += (3.0F * pull - 8.0F * push) * hold;
		body.xRot = (-0.15F * pull + 0.45F * push) * hold;
		rightLeg.xRot = -0.6F * push * hold;
		leftLeg.xRot = 0.5F * push * hold;
	}

	private void whirl(float t) {
		// The renderer spins the whole body; the arms stretch out.
		float out = seg(t, 0.0F, 3.0F) * (1.0F - seg(t, 12.0F, 16.0F));
		rightArm.xRot = Mth.lerp(out, rightArm.xRot, ARM_FORWARD);
		rightArm.yRot = -1.3F * out;
		leftArm.zRot -= 1.2F * out;
	}

	private void retreat(float t) {
		float hop = Mth.sin((float) Math.PI * seg(t, 0.0F, 10.0F));
		body.xRot = -0.2F * hop;
		rightLeg.xRot = 0.5F * hop;
		leftLeg.xRot = -0.3F * hop;
		rightArm.xRot = Mth.lerp(hop, rightArm.xRot, -0.7F);
		leftArm.xRot = Mth.lerp(hop, leftArm.xRot, -0.7F);
	}

	/** The potion to his mouth, head tipped back, small sips. */
	private void drink(float t) {
		float raise = seg(t, 0.0F, 6.0F);
		float sip = Mth.sin(t * 0.6F) * 0.06F * raise;
		rightArm.xRot = Mth.lerp(raise, rightArm.xRot, -2.2F) + sip;
		rightArm.yRot = -0.45F * raise;
		head.xRot = Mth.lerp(raise, head.xRot, -0.45F) + sip;
	}

	/** Quick half-circle bash, landing at tick 3: the shield arm in phase one, the sword arm in phase two. */
	private void counter(float t, int phase) {
		float wind = seg(t, 0.0F, 2.0F);
		float sweep = seg(t, 2.0F, 4.0F);
		float hold = 1.0F - seg(t, 5.0F, 10.0F);
		if (phase == 1) {
			// From his left across to his right.
			leftArm.xRot = Mth.lerp(hold * wind, leftArm.xRot, ARM_FORWARD);
			leftArm.yRot = Mth.lerp(sweep, -1.0F * wind, 1.0F) * hold;
			body.yRot = Mth.lerp(sweep, -0.5F * wind, 0.6F) * hold;
		} else {
			// From his right across to his left.
			rightArm.xRot = Mth.lerp(hold * wind, rightArm.xRot, ARM_FORWARD);
			rightArm.yRot = Mth.lerp(sweep, 1.2F * wind, -1.2F) * hold;
			body.yRot = Mth.lerp(sweep, 0.5F * wind, -0.6F) * hold;
		}
	}
}
