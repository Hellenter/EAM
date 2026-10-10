package io.github.hellenter.eam.client.mobs.fallenlord;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLord;
import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLordAction;

import com.mojang.blaze3d.vertex.PoseStack;

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

import org.joml.Quaternionf;
import org.joml.Quaternionfc;

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
	private static final float QUARTER = (float) Math.PI / 2.0F;
	/** Share of the knee sink kept while he just walks; more makes him bob up and down. */
	private static final float WALK_SINK = 1.0F / 6.0F;
	/** The shield in the guard, in model space: upright and facing forward, its top leaning back, turned a little outward. */
	private static final Quaternionfc SHIELD_GUARD = new Quaternionf().rotateY(-0.15F).rotateX(-0.12F).rotateY(QUARTER).rotateX(-QUARTER);
	/** The potion at his mouth: its face turned to the front and his right, the neck slanting down and in to his lips. */
	private static final Quaternionfc POTION_AT_MOUTH = new Quaternionf().rotateY(-3.0F * QUARTER / 2.0F).rotateX(0.29F).rotateZ(-QUARTER);
	/** The sword in his fencing stance: pointed straight ahead at the player, the tip a little low. */
	private static final Quaternionfc SWORD_POINT = new Quaternionf().rotateX(0.1F);
	/** How far back the pointed sword is pulled into the fist, in model pixels. */
	private static final float SWORD_GRIP_BACK = 3.0F;

	protected final ModelPart rightForearm;
	protected final ModelPart leftForearm;
	protected final ModelPart rightShin;
	protected final ModelPart leftShin;
	/** How far the shield and the potion are turned from where the hand holds them into {@link #SHIELD_GUARD} and {@link #POTION_AT_MOUTH}. */
	private float shieldGuard;
	private float potionAtMouth;
	/** How far the sword is turned into {@link #SWORD_POINT}. */
	private float swordPoint;

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
		shieldGuard = 0.0F;
		potionAtMouth = 0.0F;
		swordPoint = 0.0F;
		walk(state);
		guard(state.guard);
		if (state.dying >= 0.0F) {
			death(state.dying);
			return;
		}
		head.visible = true;
		body.visible = true;
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
			case SWORD_DASH_WINDUP -> swordDashWindup(t);
			case SWORD_DASH -> swordDash();
			case PARRY -> parry(t);
			case HOP -> hop(t);
			case BACKSTAB -> backstab(t);
			case HEAVY_WINDUP -> heavyWindup(t);
			case HEAVY_STRIKE -> heavyStrike(t);
			case STANCE -> stance(t);
			case NONE -> {
			}
		}
		// Bent knees would lift his feet off the ground: he sinks instead. Only a sixth as much while just walking, a slight bob.
		float sink = Math.min(footLift(rightLeg.xRot, rightShin.xRot), footLift(leftLeg.xRot, leftShin.xRot));
		root().y += sink * (state.action == FallenLordAction.NONE ? WALK_SINK : 1.0F) * MODEL_SCALE;
	}

	/** How far a leg with this hip and knee bend lifts its foot, in model pixels. */
	private static float footLift(float hip, float knee) {
		return KNEE * (1.0F - Mth.cos(hip)) + SHIN * (1.0F - Mth.cos(hip + knee));
	}

	/**
	 * Puts the item at the end of the forearm. The shield's handle sits in the hand; in the guard the shield turns
	 * upright and faces forward, whatever the arm does. While he drinks, the hand holds the potion by its middle.
	 */
	@Override
	public void translateToHand(SkeletonRenderState state, HumanoidArm arm, PoseStack poseStack) {
		ModelPart upper = getArm(arm);
		ModelPart forearm = arm == HumanoidArm.LEFT ? leftForearm : rightForearm;
		root().translateAndRotate(poseStack);
		upper.translateAndRotate(poseStack);
		forearm.translateAndRotate(poseStack);
		if (arm == HumanoidArm.LEFT) {
			// The shield turns about its handle, held one above the end of the forearm; the item layer puts the
			// handle 6 below where this leaves off.
			poseStack.translate(0.0F, (HAND - ELBOW - 1.0F) / 16.0F, 0.0F);
			turnHeldItem(poseStack, upper, forearm, SHIELD_GUARD, shieldGuard);
			poseStack.translate(0.0F, -6.0F / 16.0F, 0.0F);
		} else {
			poseStack.translate(0.0F, (HAND - ELBOW) / 16.0F, 0.0F);
			turnHeldItem(poseStack, upper, forearm, SWORD_POINT, swordPoint);
			// Pointed ahead, a held sword sits a few pixels out past the fist: pull it back so the fist closes on the grip.
			poseStack.translate(0.0F, 0.0F, SWORD_GRIP_BACK * swordPoint / 16.0F);
			turnHeldItem(poseStack, upper, forearm, POTION_AT_MOUTH, potionAtMouth);
			// A held potion stands out ahead of the hand; this moves its middle (1 out, 1 up the arm and 5 ahead) into the grip.
			poseStack.translate(potionAtMouth / 16.0F, (potionAtMouth - HAND) / 16.0F, 5.0F * potionAtMouth / 16.0F);
		}
	}

	/** Turns the held item by {@code amount} of the way into {@code target} in model space, undoing the arm's own turn. */
	private static void turnHeldItem(PoseStack poseStack, ModelPart upper, ModelPart forearm, Quaternionfc target, float amount) {
		if (amount <= 0.0F) {
			return;
		}
		Quaternionf hand = new Quaternionf().rotationZYX(upper.zRot, upper.yRot, upper.xRot)
				.mul(new Quaternionf().rotationZYX(forearm.zRot, forearm.yRot, forearm.xRot));
		poseStack.rotate(new Quaternionf().slerp(hand.conjugate().mul(target), amount));
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
	 * The shield held upright in front of his left side, from his mouth to his thighs, with the forearm reaching
	 * forward and in to its handle. The spear stands nearly upright at his right side, its point a little forward,
	 * clear of the shield's edge; it still sways a little as he walks. His knees stay behind the shield.
	 */
	private void guard(float amount) {
		if (amount <= 0.0F) {
			return;
		}
		leftArm.xRot = Mth.lerp(amount, leftArm.xRot, 0.0F);
		leftArm.yRot = Mth.lerp(amount, leftArm.yRot, 0.6F);
		leftArm.zRot = Mth.lerp(amount, leftArm.zRot, 0.0F);
		leftForearm.xRot = Mth.lerp(amount, leftForearm.xRot, -1.15F);
		shieldGuard = amount;
		// The held-item arm pose leans the arm forward by a tenth of a turn: what is left is the walking swing.
		float swing = rightArm.xRot + (float) Math.PI / 10.0F;
		rightArm.xRot = Mth.lerp(amount, rightArm.xRot, 0.2F + 0.4F * swing);
		rightArm.yRot = Mth.lerp(amount, rightArm.yRot, 0.0F);
		rightForearm.xRot = Mth.lerp(amount, rightForearm.xRot, -1.5F);
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

	/**
	 * The potion to his mouth: the elbow up and out in front, the shoulder raised, the head tipped back, small sips.
	 * The bottle sits in front of his helmet with its neck at his lips.
	 */
	private void drink(float t) {
		float raise = seg(t, 0.0F, 6.0F);
		float sip = Mth.sin(t * 0.6F) * 0.05F * raise;
		rightArm.xRot = Mth.lerp(raise, rightArm.xRot, -1.5F) + sip;
		rightArm.yRot = Mth.lerp(raise, rightArm.yRot, 0.3F);
		rightArm.zRot = Mth.lerp(raise, rightArm.zRot, 0.6F);
		rightArm.x += raise;
		rightArm.y -= raise;
		rightArm.z -= raise;
		rightForearm.xRot = Mth.lerp(raise, rightForearm.xRot, -1.4F);
		head.xRot = Mth.lerp(raise, head.xRot, -0.3F) + sip;
		potionAtMouth = raise;
	}

	/**
	 * Crouches with the sword low at his right hip, its blade out to the side where the player can see it, the left
	 * hand reaching out at the player. Half the spear dash's windup.
	 */
	private void swordDashWindup(float t) {
		float crouch = seg(t, 0.0F, 3.0F);
		body.xRot = 0.2F * crouch;
		body.yRot = -0.2F * crouch;
		head.xRot -= 0.2F * crouch;
		trailBlade(crouch);
		leftArm.xRot = Mth.lerp(crouch, leftArm.xRot, -1.4F);
		leftArm.yRot = Mth.lerp(crouch, leftArm.yRot, 0.3F);
		leftForearm.xRot = Mth.lerp(crouch, leftForearm.xRot, -0.15F);
		// Left foot forward, ready to spring.
		bendLeg(leftLeg, leftShin, crouch, -0.6F, 1.0F);
		bendLeg(rightLeg, rightShin, crouch, 0.3F, 0.8F);
	}

	/** Rushes in leaning forward, the blade still trailing; the combo's first cut comes from there. */
	private void swordDash() {
		body.xRot = 0.35F;
		head.xRot -= 0.3F;
		trailBlade(1.0F);
		leftArm.xRot = 0.4F;
		leftForearm.xRot = -0.5F;
	}

	/** The sword arm low and out at his right, turned so the blade sticks out to that side and a little back. */
	private void trailBlade(float amount) {
		rightArm.xRot = Mth.lerp(amount, rightArm.xRot, 0.2F);
		rightArm.yRot = Mth.lerp(amount, rightArm.yRot, 2.1F);
		rightArm.zRot = Mth.lerp(amount, rightArm.zRot, 0.7F);
		rightForearm.xRot = Mth.lerp(amount, rightForearm.xRot, -0.15F);
	}

	/** A flick of the sword across his front, from his right to his left, knocking an arrow aside. */
	private void parry(float t) {
		float out = seg(t, 0.0F, 1.5F);
		float sweep = seg(t, 0.5F, 2.5F);
		float hold = 1.0F - seg(t, 3.0F, 6.0F);
		rightArm.xRot = Mth.lerp(out * hold, rightArm.xRot, -1.0F);
		rightArm.yRot = Mth.lerp(out * hold, rightArm.yRot, Mth.lerp(sweep, 0.7F, -0.6F));
		rightForearm.xRot = Mth.lerp(out * hold, rightForearm.xRot, -0.2F);
		body.yRot = Mth.lerp(sweep, 0.25F, -0.3F) * out * hold;
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

	/** A short crouched hop: knees tucked in the air, arms out for balance, the guard kept. */
	private void hop(float t) {
		float hop = Mth.sin((float) Math.PI * seg(t, 0.0F, 9.0F));
		body.xRot = 0.15F * hop;
		rightForearm.xRot = Mth.lerp(hop, rightForearm.xRot, -0.9F);
		bendLeg(rightLeg, rightShin, hop, -0.45F, 1.0F);
		bendLeg(leftLeg, leftShin, hop, 0.0F, 0.8F);
	}

	/**
	 * Darts low round the player with the blade trailing, raises it on the way to their back and cuts across, landing
	 * on the backstab's cut tick.
	 */
	private void backstab(float t) {
		float crouch = seg(t, 0.0F, 2.0F) * (1.0F - seg(t, 5.0F, 7.0F));
		body.xRot = 0.35F * crouch;
		head.xRot -= 0.3F * crouch;
		trailBlade(crouch);
		bendLeg(leftLeg, leftShin, crouch, -0.6F, 1.0F);
		bendLeg(rightLeg, rightShin, crouch, 0.3F, 0.8F);
		if (t >= 5.0F) {
			slash(t, FallenLordAction.BACKSTAB_CUT - 5.0F, -1.0F);
		}
	}

	/**
	 * Both hands on the hilt, the sword raised high over his head, its blade up and leaning back, leaning back on the rear leg: a slow, plain
	 * windup that tells the player to get out of reach.
	 */
	private void heavyWindup(float t) {
		float raise = seg(t, 0.0F, 6.0F);
		float tremble = Mth.sin(t * 2.5F) * 0.03F * seg(t, 6.0F, 8.0F);
		rightArm.xRot = Mth.lerp(raise, rightArm.xRot, -2.4F) + tremble;
		rightArm.yRot = Mth.lerp(raise, rightArm.yRot, 1.2F);
		rightArm.zRot = Mth.lerp(raise, rightArm.zRot, 0.0F);
		rightForearm.xRot = Mth.lerp(raise, rightForearm.xRot, 0.3F);
		leftArm.xRot = Mth.lerp(raise, leftArm.xRot, -2.3F) + tremble;
		leftArm.yRot = Mth.lerp(raise, leftArm.yRot, 0.6F);
		leftArm.zRot = Mth.lerp(raise, leftArm.zRot, 0.0F);
		leftForearm.xRot = Mth.lerp(raise, leftForearm.xRot, 0.2F);
		body.xRot = -0.15F * raise;
		head.xRot -= 0.15F * raise;
		bendLeg(rightLeg, rightShin, raise, 0.45F, 0.5F);
		bendLeg(leftLeg, leftShin, raise, -0.4F, 0.35F);
	}

	/** Brings the sword down two-handed in front of him, bending forward into it, then straightens. */
	private void heavyStrike(float t) {
		float cut = seg(t, 0.0F, FallenLordAction.HEAVY_HIT);
		float back = seg(t, 6.0F, 12.0F);
		float hold = 1.0F - back;
		rightArm.xRot = Mth.lerp(hold, rightArm.xRot, Mth.lerp(cut, -2.4F, -0.35F));
		rightArm.yRot = Mth.lerp(hold, rightArm.yRot, Mth.lerp(cut, 1.2F, -0.2F));
		rightForearm.xRot = Mth.lerp(hold, rightForearm.xRot, Mth.lerp(cut, 0.3F, -0.1F));
		leftArm.xRot = Mth.lerp(hold, leftArm.xRot, Mth.lerp(cut, -2.3F, -0.45F));
		leftArm.yRot = Mth.lerp(hold, leftArm.yRot, Mth.lerp(cut, 0.6F, 0.5F));
		leftForearm.xRot = Mth.lerp(hold, leftForearm.xRot, Mth.lerp(cut, 0.2F, -0.3F));
		body.xRot = Mth.lerp(cut, -0.15F, 0.45F) * hold;
		head.xRot += 0.1F * cut * hold;
		bendLeg(leftLeg, leftShin, hold, Mth.lerp(cut, -0.4F, -0.7F), Mth.lerp(cut, 0.35F, 0.9F));
		bendLeg(rightLeg, rightShin, hold, Mth.lerp(cut, 0.45F, 0.5F), Mth.lerp(cut, 0.5F, 0.3F));
	}

	/**
	 * The fencing stance after a leap back: side on, the right foot forward and both knees bent, the sword arm out with
	 * the blade pointed at the player, the left arm raised behind with the hand above his head. The point circles a
	 * little and the raised hand beckons: come on.
	 */
	private void stance(float t) {
		float in = seg(t, 0.0F, 5.0F);
		float circle = t * 0.3F;
		float beckon = Mth.sin(t * 0.25F) * 0.15F * in;
		body.yRot = -0.5F * in;
		head.xRot -= 0.05F * in;
		rightArm.xRot = Mth.lerp(in, rightArm.xRot, -1.25F + Mth.cos(circle) * 0.04F);
		rightArm.yRot = Mth.lerp(in, rightArm.yRot, 0.15F + Mth.sin(circle) * 0.05F);
		rightArm.zRot = Mth.lerp(in, rightArm.zRot, 0.0F);
		rightArm.z -= 2.5F * in;
		rightForearm.xRot = Mth.lerp(in, rightForearm.xRot, -0.15F);
		swordPoint = in;
		leftArm.xRot = Mth.lerp(in, leftArm.xRot, 0.35F);
		leftArm.yRot = Mth.lerp(in, leftArm.yRot, 0.0F);
		leftArm.zRot = Mth.lerp(in, leftArm.zRot, -1.75F);
		leftArm.z += 2.0F * in;
		leftForearm.xRot = Mth.lerp(in, leftForearm.xRot, 0.0F);
		leftForearm.zRot = Mth.lerp(in, leftForearm.zRot, -1.2F + beckon);
		bendLeg(rightLeg, rightShin, in, -0.4F, 0.45F);
		bendLeg(leftLeg, leftShin, in, 0.35F, 0.3F);
		rightLeg.zRot = 0.15F * in;
		leftLeg.zRot = -0.15F * in;
	}

	/**
	 * He sinks to his knees, slumped; the skull and ribs (this base layer) burn away; then the empty armor falls in a
	 * heap on its back, the helmet rolling off to his right. Times are those of {@link FallenLord}.
	 */
	private void death(float t) {
		float kneel = seg(t, 0.0F, 10.0F);
		body.xRot = 0.3F * kneel;
		head.xRot = 0.5F * kneel;
		for (ModelPart arm : new ModelPart[] {rightArm, leftArm}) {
			arm.xRot = Mth.lerp(kneel, arm.xRot, 0.15F);
			arm.yRot = Mth.lerp(kneel, arm.yRot, 0.0F);
			arm.zRot = Mth.lerp(kneel, arm.zRot, 0.0F);
		}
		rightForearm.xRot = Mth.lerp(kneel, rightForearm.xRot, -0.15F);
		leftForearm.xRot = Mth.lerp(kneel, leftForearm.xRot, -0.15F);
		bendLeg(rightLeg, rightShin, kneel, -0.25F, 1.7F);
		bendLeg(leftLeg, leftShin, kneel, -0.15F, 1.7F);
		float sink = Math.min(footLift(rightLeg.xRot, rightShin.xRot), footLift(leftLeg.xRot, leftShin.xRot));
		// Nothing inside the armor any more.
		boolean burnt = t >= FallenLord.ASH_END;
		if (getClass() == FallenLordModel.class) {
			head.visible = !burnt;
			body.visible = !burnt;
		}

		// The armor drops piece by piece, faster as it falls.
		float ground = 24.0F * MODEL_SCALE;
		float s = FallenLord.COLLAPSE_START;
		float span = (FallenLord.COLLAPSE_END - FallenLord.COLLAPSE_START) - 6.0F;
		float helmet = drop(t, s, span);
		float chest = drop(t, s + 2.0F, span);
		float arms = drop(t, s + 4.0F, span);
		float legs = drop(t, s + 6.0F, span);
		root().y += sink * MODEL_SCALE * (1.0F - chest);
		// Helmet: off the neck, onto its side beside the breastplate.
		fall(head, helmet, -10.0F, ground - 6.6F, -5.0F, 0.3F, 0.6F, -QUARTER);
		// Breastplate flat on its back, the neck end where he knelt.
		float lying = ground - 3.6F;
		fall(body, chest, 0.0F, lying, 0.0F, QUARTER, 0.0F, 0.0F);
		fall(rightArm, arms, -7.5F, lying, 2.4F, QUARTER, 0.25F, 0.0F);
		fall(leftArm, arms, 7.5F, lying, 2.4F, QUARTER, -0.25F, 0.0F);
		rightForearm.xRot = Mth.lerp(arms, rightForearm.xRot, -0.2F);
		leftForearm.xRot = Mth.lerp(arms, leftForearm.xRot, 0.0F);
		float hips = 12.0F * MODEL_SCALE;
		fall(rightLeg, legs, -2.8F, ground - 3.0F, hips, QUARTER, 0.12F, 0.0F);
		fall(leftLeg, legs, 2.8F, ground - 3.0F, hips, QUARTER, -0.12F, 0.0F);
		rightShin.xRot = Mth.lerp(legs, rightShin.xRot, 0.0F);
		leftShin.xRot = Mth.lerp(legs, leftShin.xRot, 0.0F);
	}

	/** How far a falling piece has come, 0 to 1, accelerating like a dropped object. */
	private static float drop(float t, float start, float span) {
		float p = Mth.clamp((t - start) / span, 0.0F, 1.0F);
		return p * p;
	}

	/** Moves a part {@code amount} of the way to where it lies on the ground. */
	private static void fall(ModelPart part, float amount, float x, float y, float z, float xRot, float yRot, float zRot) {
		part.x = Mth.lerp(amount, part.x, x);
		part.y = Mth.lerp(amount, part.y, y);
		part.z = Mth.lerp(amount, part.z, z);
		part.xRot = Mth.lerp(amount, part.xRot, xRot);
		part.yRot = Mth.lerp(amount, part.yRot, yRot);
		part.zRot = Mth.lerp(amount, part.zRot, zRot);
	}
}
