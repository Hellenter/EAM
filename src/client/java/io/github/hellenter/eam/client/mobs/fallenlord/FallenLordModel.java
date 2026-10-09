package io.github.hellenter.eam.client.mobs.fallenlord;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLordAction;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.monster.skeleton.SkeletonModel;
import net.minecraft.client.renderer.entity.state.SkeletonRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

/**
 * Skeleton model with the Fallen Lord's attack animations. His arms and legs have two segments each, bending at the
 * elbows and knees as he walks and fights. Each attack is keyed by ticks since it started, matching the server's
 * timing (hits land on the frames named in {@code FallenLord}). The right arm holds the weapon, the left the shield,
 * which he keeps raised in front of him while it is up.
 */
public class FallenLordModel extends SkeletonModel<FallenLordRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Eam.id("fallen_lord"), "main");
	/** As big as the wither skeleton model. */
	static final float MODEL_SCALE = 1.2F;
	/** The elbow below the shoulder pivot; arms reach 10 below it, like the vanilla arm. */
	static final float ELBOW = 4.0F;
	/** The knee below the hip; legs are 12 long. */
	static final float KNEE = 6.0F;
	private static final float SHIN = 6.0F;
	/** The item layer puts the item this far down the arm from where {@link #translateToHand} leaves off. */
	private static final float HAND = 10.0F;
	private static final float ARM_FORWARD = -(float) Math.PI / 2.0F;

	protected final ModelPart rightForearm;
	protected final ModelPart leftForearm;
	protected final ModelPart rightShin;
	protected final ModelPart leftShin;
	/** In the guard the shield is turned back nearly upright and facing forward, whatever the arm does. */
	private float shieldTilt;
	private float shieldTurn;

	public FallenLordModel(ModelPart root) {
		super(root);
		rightForearm = rightArm.getChild("right_forearm");
		leftForearm = leftArm.getChild("left_forearm");
		rightShin = rightLeg.getChild("right_shin");
		leftShin = leftLeg.getChild("left_shin");
	}

	/** The skull and ribcage of the vanilla skeleton texture; his limb bones are drawn by the gear layer. */
	public static LayerDefinition createLayer() {
		MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
		addEmptyLimbs(mesh.getRoot());
		return LayerDefinition.create(mesh, 64, 32).apply(MeshTransformer.scaling(MODEL_SCALE));
	}

	/**
	 * Adds the arms and legs, each as an upper segment with the lower one hanging from its joint. Every layer of his
	 * model has the same joints, so the layers bend together.
	 */
	static void addLimbs(PartDefinition root, CubeListBuilder rightArm, CubeListBuilder rightForearm, CubeListBuilder leftArm,
			CubeListBuilder leftForearm, CubeListBuilder rightLeg, CubeListBuilder rightShin, CubeListBuilder leftLeg, CubeListBuilder leftShin) {
		root.addOrReplaceChild("right_arm", rightArm, PartPose.offset(-5.0F, 2.0F, 0.0F))
				.addOrReplaceChild("right_forearm", rightForearm, PartPose.offset(0.0F, ELBOW, 0.0F));
		root.addOrReplaceChild("left_arm", leftArm, PartPose.offset(5.0F, 2.0F, 0.0F))
				.addOrReplaceChild("left_forearm", leftForearm, PartPose.offset(0.0F, ELBOW, 0.0F));
		root.addOrReplaceChild("right_leg", rightLeg, PartPose.offset(-2.0F, 12.0F, 0.0F))
				.addOrReplaceChild("right_shin", rightShin, PartPose.offset(0.0F, KNEE, 0.0F));
		root.addOrReplaceChild("left_leg", leftLeg, PartPose.offset(2.0F, 12.0F, 0.0F))
				.addOrReplaceChild("left_shin", leftShin, PartPose.offset(0.0F, KNEE, 0.0F));
	}

	static void addEmptyLimbs(PartDefinition root) {
		addLimbs(root, CubeListBuilder.create(), CubeListBuilder.create(), CubeListBuilder.create(), CubeListBuilder.create(),
				CubeListBuilder.create(), CubeListBuilder.create(), CubeListBuilder.create(), CubeListBuilder.create());
	}

	@Override
	public void setupAnim(FallenLordRenderState state) {
		super.setupAnim(state);
		shieldTilt = 0.0F;
		shieldTurn = 0.0F;
		walk(state);
		guard(state.guard);
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
		// Bent knees would lift his feet off the ground: he sinks instead. Half as much while just walking, a gentle bob.
		float sink = Math.min(footLift(rightLeg.xRot, rightShin.xRot), footLift(leftLeg.xRot, leftShin.xRot));
		root().y += sink * (state.action == FallenLordAction.NONE ? 0.5F : 1.0F) * MODEL_SCALE;
	}

	/** How far a leg with this hip and knee bend lifts its foot, in model pixels. */
	private static float footLift(float hip, float knee) {
		return KNEE * (1.0F - Mth.cos(hip)) + SHIN * (1.0F - Mth.cos(hip + knee));
	}

	/** Puts the item at the end of the forearm. For the shield in the guard, also turns it back upright. */
	@Override
	public void translateToHand(SkeletonRenderState state, HumanoidArm arm, PoseStack poseStack) {
		root().translateAndRotate(poseStack);
		getArm(arm).translateAndRotate(poseStack);
		(arm == HumanoidArm.LEFT ? leftForearm : rightForearm).translateAndRotate(poseStack);
		poseStack.translate(0.0F, (HAND - ELBOW) / 16.0F, 0.0F);
		if (arm == HumanoidArm.LEFT) {
			poseStack.rotate(Axis.XP, -shieldTilt);
			poseStack.rotate(Axis.YP, shieldTurn);
		}
		poseStack.translate(0.0F, -HAND / 16.0F, 0.0F);
	}

	/** 0 before {@code start}, 1 after {@code end}, smooth in between. */
	private static float seg(float t, float start, float end) {
		float p = Mth.clamp((t - start) / (end - start), 0.0F, 1.0F);
		return p * p * (3.0F - 2.0F * p);
	}

	/** Bends a leg, blending from its current pose: {@code hip} below zero swings it forward, {@code knee} folds the shin back. */
	private static void bendLeg(ModelPart leg, ModelPart shin, float amount, float hip, float knee) {
		leg.xRot = Mth.lerp(amount, leg.xRot, hip);
		shin.xRot = Mth.lerp(amount, shin.xRot, knee);
	}

	/** Knees fold as each leg swings forward; elbows stay a little bent, more as the arm swings forward. */
	private void walk(FallenLordRenderState state) {
		float speed = Math.min(state.walkAnimationSpeed, 1.0F);
		float phase = state.walkAnimationPos * 0.6662F;
		rightShin.xRot = (0.1F + 1.1F * Math.max(0.0F, Mth.sin(phase))) * speed;
		leftShin.xRot = (0.1F + 1.1F * Math.max(0.0F, -Mth.sin(phase))) * speed;
		rightForearm.xRot = -0.25F - 0.6F * Math.max(0.0F, -rightArm.xRot);
		leftForearm.xRot = -0.25F - 0.6F * Math.max(0.0F, -leftArm.xRot);
	}

	/**
	 * The shield held low across the front of his body: forearm forward, face forward, its top a little back. Held at
	 * the side, a shield faces outward; in the guard it turns a quarter round, less what the arm already turned in.
	 */
	private void guard(float amount) {
		if (amount <= 0.0F) {
			return;
		}
		leftArm.xRot = Mth.lerp(amount, leftArm.xRot, 0.0F);
		leftArm.yRot = Mth.lerp(amount, leftArm.yRot, 0.35F);
		leftArm.zRot = Mth.lerp(amount, leftArm.zRot, 0.0F);
		leftForearm.xRot = Mth.lerp(amount, leftForearm.xRot, -1.0F);
		shieldTilt = amount * 0.9F * (leftArm.xRot + leftForearm.xRot);
		shieldTurn = amount * ((float) Math.PI / 2.0F - leftArm.yRot);
	}

	private void poke(float t) {
		float pull = seg(t, 0.0F, 5.0F);
		float push = seg(t, 5.0F, 7.0F);
		float back = seg(t, 8.0F, 12.0F);
		float hold = 1.0F - back;
		// A held spear points the way the forearm would if it were raised a quarter turn: with the forearm hanging
		// it lies level. He draws it back level, elbow bent, then straightens the arm and drives it out, tip a little up.
		rightArm.xRot = Mth.lerp(hold * Math.max(pull, push), rightArm.xRot, Mth.lerp(push, 0.5F, -0.35F));
		rightArm.yRot = 0.0F;
		rightArm.z += (3.0F * pull - 9.0F * push) * hold;
		rightForearm.xRot = Mth.lerp(hold * Math.max(pull, push), rightForearm.xRot, -0.5F * (1.0F - push));
		body.yRot = (0.3F * pull - 0.6F * push) * hold;
		bendLeg(rightLeg, rightShin, push * hold, -0.4F, 0.45F);
		bendLeg(leftLeg, leftShin, push * hold, 0.3F, 0.1F);
	}

	private void dashWindup(float t) {
		float crouch = seg(t, 0.0F, 4.0F);
		body.xRot = 0.25F * crouch;
		head.z -= 1.0F * crouch;
		head.xRot -= 0.25F * crouch;
		rightArm.xRot = Mth.lerp(crouch, rightArm.xRot, 0.8F);
		rightArm.z += 2.0F * crouch;
		rightForearm.xRot = Mth.lerp(crouch, rightForearm.xRot, -0.7F);
		// Low and coiled, the right foot forward.
		bendLeg(rightLeg, rightShin, crouch, -0.6F, 1.0F);
		bendLeg(leftLeg, leftShin, crouch, 0.25F, 0.8F);
	}

	/** Runs with the spear held low and level, aimed ahead. */
	private void dash() {
		body.xRot = 0.3F;
		rightArm.xRot = 0.3F;
		rightArm.yRot = 0.0F;
		rightArm.z -= 2.0F;
		rightForearm.xRot = -0.4F;
		leftArm.xRot = 0.5F;
		leftForearm.xRot = -0.6F;
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
		// The spear goes back behind his head, then the elbow whips straight.
		float elbow = Mth.lerp(raise, rightForearm.xRot, -1.2F);
		elbow = Mth.lerp(release, elbow, 0.0F);
		rightForearm.xRot = Mth.lerp(back, elbow, rightForearm.xRot);
		body.yRot = (-0.4F * raise + 0.7F * release) * (1.0F - back);
		bendLeg(leftLeg, leftShin, release * (1.0F - back), -0.45F, 0.4F);
		bendLeg(rightLeg, rightShin, raise * (1.0F - back), 0.3F, 0.3F);
	}

	private void swipe(float t) {
		float wind = seg(t, 0.0F, 6.0F);
		float sweep = seg(t, 6.0F, 9.0F);
		float hold = 1.0F - seg(t, 10.0F, 14.0F);
		rightArm.xRot = Mth.lerp(hold * wind, rightArm.xRot, ARM_FORWARD);
		rightArm.yRot = Mth.lerp(sweep, -1.2F * wind, 1.2F) * hold;
		rightForearm.xRot = Mth.lerp(hold, rightForearm.xRot, -0.9F * wind * (1.0F - sweep));
		body.yRot = Mth.lerp(sweep, -0.5F * wind, 0.6F) * hold;
		bendLeg(rightLeg, rightShin, wind * hold, -0.3F, 0.5F);
		bendLeg(leftLeg, leftShin, wind * hold, 0.2F, 0.4F);
	}

	private void transition(float t) {
		// Flings the shield aside.
		float fling = Mth.sin((float) Math.PI * seg(t, 0.0F, 8.0F));
		leftArm.zRot -= 1.3F * fling;
		leftArm.xRot = Mth.lerp(fling, leftArm.xRot, -0.4F);
		leftForearm.xRot = Mth.lerp(fling, leftForearm.xRot, 0.0F);
		// Throws the spear.
		if (t < 20.0F) {
			throwSpear(t, 7.0F);
		} else {
			// Reaches to the hip, draws the sword and raises it.
			float reach = seg(t, 20.0F, 26.0F);
			float draw = seg(t, 26.0F, 31.0F);
			float lower = seg(t, 35.0F, 40.0F);
			float x = Mth.lerp(reach, rightArm.xRot, -0.5F);
			x = Mth.lerp(draw, x, -2.6F);
			rightArm.xRot = Mth.lerp(lower, x, rightArm.xRot);
			rightArm.yRot = (0.9F * reach * (1.0F - draw));
			float elbow = Mth.lerp(reach, rightForearm.xRot, -1.1F);
			elbow = Mth.lerp(draw, elbow, -0.3F);
			rightForearm.xRot = Mth.lerp(lower, elbow, rightForearm.xRot);
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
		// The blade cocks behind his head with the elbow bent, the arm straightens through the cut.
		float elbow = Mth.lerp(raise, rightForearm.xRot, -1.1F);
		elbow = Mth.lerp(strike, elbow, -0.1F);
		rightForearm.xRot = Mth.lerp(back, elbow, rightForearm.xRot);
		body.yRot = Mth.lerp(strike, -0.2F * dir * raise, 0.35F * dir) * (1.0F - back);
		float step = strike * (1.0F - back);
		bendLeg(rightLeg, rightShin, step, -0.35F, 0.45F);
		bendLeg(leftLeg, leftShin, step, 0.25F, 0.3F);
	}

	private void combo(float t) {
		if (t < 8.0F) {
			// Leap with the sword raised and the knees tucked.
			float up = seg(t, 0.0F, 3.0F);
			float land = seg(t, 5.0F, 8.0F);
			rightArm.xRot = Mth.lerp(up, rightArm.xRot, -2.7F);
			rightForearm.xRot = Mth.lerp(up, rightForearm.xRot, -0.9F);
			leftArm.xRot = Mth.lerp(up, leftArm.xRot, -1.2F);
			leftForearm.xRot = Mth.lerp(up, leftForearm.xRot, -0.5F);
			bendLeg(rightLeg, rightShin, up, Mth.lerp(land, -0.8F, -0.4F), Mth.lerp(land, 1.4F, 0.7F));
			bendLeg(leftLeg, leftShin, up, Mth.lerp(land, -0.3F, 0.2F), Mth.lerp(land, 1.2F, 0.6F));
		} else if (t < 26.0F) {
			// Slashes land at 10, 16 and 22, alternating sides, from a low stance.
			int i = Mth.clamp((int) ((t - 6.0F) / 6.0F), 0, 2);
			float hit = 10.0F + 6.0F * i;
			slash(t, hit - 4.5F, i % 2 == 0 ? 1.0F : -1.0F);
			bendLeg(rightLeg, rightShin, 0.6F, -0.4F, 0.6F);
			bendLeg(leftLeg, leftShin, 0.6F, 0.2F, 0.5F);
		} else {
			// Kick at 30: knee up, the foot snaps out, back down.
			float chamber = seg(t, 26.0F, 29.0F);
			float extend = seg(t, 29.0F, 31.0F);
			float retract = seg(t, 31.0F, 36.0F);
			rightLeg.xRot = -1.4F * chamber * (1.0F - retract);
			rightShin.xRot = 1.5F * chamber * (1.0F - extend) + 0.6F * Mth.sin((float) Math.PI * retract);
			bendLeg(leftLeg, leftShin, chamber * (1.0F - retract), 0.15F, 0.35F);
			float kick = Mth.sin((float) Math.PI * seg(t, 26.0F, 34.0F));
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
		// A stab: the blade level as he draws back, driven forward and a little up.
		rightArm.xRot = Mth.lerp(hold * Math.max(pull, push), rightArm.xRot, Mth.lerp(push, 0.5F, -0.4F));
		rightArm.yRot = 0.0F;
		rightArm.z += (3.0F * pull - 8.0F * push) * hold;
		rightForearm.xRot = Mth.lerp(hold * Math.max(pull, push), rightForearm.xRot, -0.5F * (1.0F - push));
		body.xRot = (-0.15F * pull + 0.45F * push) * hold;
		// Deep lunge: the front knee bends, the back leg stretches out.
		bendLeg(rightLeg, rightShin, push * hold, -0.9F, 0.9F);
		bendLeg(leftLeg, leftShin, push * hold, 0.6F, 0.15F);
	}

	private void whirl(float t) {
		// The renderer spins the whole body; the arms stretch out, the knees bend.
		float out = seg(t, 0.0F, 3.0F) * (1.0F - seg(t, 12.0F, 16.0F));
		rightArm.xRot = Mth.lerp(out, rightArm.xRot, ARM_FORWARD);
		rightArm.yRot = -1.3F * out;
		rightForearm.xRot = Mth.lerp(out, rightForearm.xRot, -0.15F);
		leftArm.zRot -= 1.2F * out;
		leftForearm.xRot = Mth.lerp(out, leftForearm.xRot, -0.3F);
		bendLeg(rightLeg, rightShin, out, -0.35F, 0.6F);
		bendLeg(leftLeg, leftShin, out, -0.35F, 0.6F);
	}

	private void retreat(float t) {
		float hop = Mth.sin((float) Math.PI * seg(t, 0.0F, 10.0F));
		body.xRot = -0.2F * hop;
		rightArm.xRot = Mth.lerp(hop, rightArm.xRot, -0.7F);
		leftArm.xRot = Mth.lerp(hop, leftArm.xRot, -0.7F);
		rightForearm.xRot = Mth.lerp(hop, rightForearm.xRot, -0.6F);
		leftForearm.xRot = Mth.lerp(hop, leftForearm.xRot, -0.6F);
		// Knees tucked in the air.
		bendLeg(rightLeg, rightShin, hop, -0.5F, 1.1F);
		bendLeg(leftLeg, leftShin, hop, -0.1F, 0.8F);
	}

	/** The potion to his mouth, elbow bent, head tipped back, small sips. */
	private void drink(float t) {
		float raise = seg(t, 0.0F, 6.0F);
		float sip = Mth.sin(t * 0.6F) * 0.06F * raise;
		rightArm.xRot = Mth.lerp(raise, rightArm.xRot, -1.0F);
		rightArm.yRot = -0.55F * raise;
		rightForearm.xRot = Mth.lerp(raise, rightForearm.xRot, -1.5F) + sip;
		head.xRot = Mth.lerp(raise, head.xRot, -0.45F) + sip;
	}

	/** Quick half-circle bash, landing at tick 3: the shield arm in phase one, the sword arm in phase two. */
	private void counter(float t, int phase) {
		float wind = seg(t, 0.0F, 2.0F);
		float sweep = seg(t, 2.0F, 4.0F);
		float hold = 1.0F - seg(t, 5.0F, 10.0F);
		float elbow = -0.9F * wind * (1.0F - sweep);
		if (phase == 1) {
			// From his left across to his right.
			leftArm.xRot = Mth.lerp(hold * wind, leftArm.xRot, ARM_FORWARD);
			leftArm.yRot = Mth.lerp(sweep, -1.0F * wind, 1.0F) * hold;
			leftForearm.xRot = Mth.lerp(hold, leftForearm.xRot, elbow);
			body.yRot = Mth.lerp(sweep, -0.5F * wind, 0.6F) * hold;
		} else {
			// From his right across to his left.
			rightArm.xRot = Mth.lerp(hold * wind, rightArm.xRot, ARM_FORWARD);
			rightArm.yRot = Mth.lerp(sweep, 1.2F * wind, -1.2F) * hold;
			rightForearm.xRot = Mth.lerp(hold, rightForearm.xRot, elbow);
			body.yRot = Mth.lerp(sweep, 0.5F * wind, -0.6F) * hold;
		}
		bendLeg(rightLeg, rightShin, wind * hold, -0.3F, 0.5F);
		bendLeg(leftLeg, leftShin, wind * hold, 0.2F, 0.4F);
	}
}
