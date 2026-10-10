package io.github.hellenter.eam.client.mobs.lurker;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.entity.lurker.LurkerAction;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * A hunched, withered body on all fours, chimp-like: a short torso leaning far forward, long arms reaching the ground,
 * bent legs. A torn monk's robe hangs off it: over the torso, a hood, ragged sleeves, a flap hanging from the chest and
 * a torn skirt over the hips. The shoulders, neck and robe follow the torso's lean, which changes as it leaps and
 * swipes; every part hangs from the root so the arms and legs swing in the world's frame.
 */
public class SwampLurkerModel extends EntityModel<SwampLurkerRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Eam.id("swamp_lurker"), "main");
	/** The hips, where the torso and legs meet. */
	private static final float HIP_Y = 13.0F;
	private static final float HIP_Z = 3.0F;
	/** How far forward the torso leans while it walks, from upright. */
	private static final float LEAN = 0.95F;
	private static final float TORSO = 11.0F;
	/** The shoulders on the torso, from the hips. */
	private static final float SHOULDER_UP = 10.0F;
	private static final float SHOULDER_OUT = 4.5F;
	private static final float UPPER_ARM = 8.0F;
	private static final float THIGH = 6.0F;
	/** Arms hang a little ahead of straight down; the knees are bent well forward, the shins nearly upright. */
	private static final float ARM_REST = -0.25F;
	private static final float ELBOW_REST = 0.15F;
	private static final float THIGH_REST = -1.0F;
	private static final float SHIN_REST = 1.2F;

	private final ModelPart body;
	private final ModelPart robeFront;
	private final ModelPart robeSkirt;
	private final ModelPart head;
	private final ModelPart rightArm;
	private final ModelPart leftArm;
	private final ModelPart rightForearm;
	private final ModelPart leftForearm;
	private final ModelPart rightLeg;
	private final ModelPart leftLeg;
	private final ModelPart rightShin;
	private final ModelPart leftShin;

	public SwampLurkerModel(ModelPart root) {
		super(root);
		body = root.getChild("body");
		robeFront = body.getChild("robe_front");
		robeSkirt = body.getChild("robe_skirt");
		head = root.getChild("head");
		rightArm = root.getChild("right_arm");
		leftArm = root.getChild("left_arm");
		rightForearm = rightArm.getChild("right_forearm");
		leftForearm = leftArm.getChild("left_forearm");
		rightLeg = root.getChild("right_leg");
		leftLeg = root.getChild("left_leg");
		rightShin = rightLeg.getChild("right_shin");
		leftShin = leftLeg.getChild("left_shin");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
						// The ribs and the robe over them, torn open in places.
						.texOffs(0, 16).addBox(-3.5F, -TORSO, -2.0F, 7.0F, TORSO, 4.0F)
						.texOffs(22, 16).addBox(-4.0F, -TORSO - 0.5F, -2.5F, 8.0F, 12.0F, 5.0F)
						// The hood's cape over the shoulders.
						.texOffs(0, 55).addBox(-4.5F, -TORSO - 1.0F, -3.0F, 9.0F, 3.0F, 6.0F),
				PartPose.offsetAndRotation(0.0F, HIP_Y, HIP_Z, LEAN, 0.0F, 0.0F));
		// A flap of the robe hanging from the chest, and its torn skirt hanging over the hips.
		body.addOrReplaceChild("robe_front", CubeListBuilder.create().texOffs(26, 44).addBox(-3.5F, 0.0F, -0.5F, 7.0F, 8.0F, 1.0F),
				PartPose.offset(0.0F, -5.0F, -2.7F));
		body.addOrReplaceChild("robe_skirt", CubeListBuilder.create().texOffs(0, 44).addBox(-4.0F, 0.0F, -2.5F, 8.0F, 6.0F, 5.0F),
				PartPose.offset(0.0F, 0.5F, 0.0F));
		// A narrow skull under a hood.
		root.addOrReplaceChild("head", CubeListBuilder.create()
						.texOffs(0, 0).addBox(-3.0F, -6.0F, -4.5F, 6.0F, 7.0F, 6.0F)
						.texOffs(24, 0).addBox(-3.5F, -6.6F, -5.0F, 7.0F, 8.0F, 7.0F),
				PartPose.ZERO);
		CubeListBuilder arm = CubeListBuilder.create()
				.texOffs(48, 16).addBox(-1.5F, -1.0F, -1.5F, 3.0F, UPPER_ARM + 1.0F, 3.0F)
				.texOffs(0, 34).addBox(-2.0F, -1.5F, -2.0F, 4.0F, 6.0F, 4.0F);
		CubeListBuilder forearm = CubeListBuilder.create()
				.texOffs(48, 28).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 9.0F, 2.0F)
				.texOffs(16, 34).addBox(-1.5F, 8.0F, -1.5F, 3.0F, 2.0F, 3.0F);
		root.addOrReplaceChild("right_arm", arm, PartPose.ZERO)
				.addOrReplaceChild("right_forearm", forearm, PartPose.offset(0.0F, UPPER_ARM, 0.0F));
		root.addOrReplaceChild("left_arm", arm.mirror(), PartPose.ZERO)
				.addOrReplaceChild("left_forearm", forearm.mirror(), PartPose.offset(0.0F, UPPER_ARM, 0.0F));
		CubeListBuilder thigh = CubeListBuilder.create().texOffs(28, 34).addBox(-1.5F, 0.0F, -1.5F, 3.0F, THIGH, 3.0F);
		CubeListBuilder shin = CubeListBuilder.create().texOffs(40, 34).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F);
		root.addOrReplaceChild("right_leg", thigh, PartPose.offset(-2.0F, HIP_Y, HIP_Z))
				.addOrReplaceChild("right_shin", shin, PartPose.offset(0.0F, THIGH, 0.0F));
		root.addOrReplaceChild("left_leg", thigh.mirror(), PartPose.offset(2.0F, HIP_Y, HIP_Z))
				.addOrReplaceChild("left_shin", shin.mirror(), PartPose.offset(0.0F, THIGH, 0.0F));
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(SwampLurkerRenderState state) {
		super.setupAnim(state);
		float t = state.actionTime;
		float walkPos = state.walkAnimationPos;
		float walk = Math.min(state.walkAnimationSpeed, 1.0F);
		float swing = Mth.cos(walkPos * 0.6662F);
		float age = state.ageInTicks;

		// Torso lean and the limbs' rest angles, then each action bends them.
		float lean = LEAN + Mth.sin(age * 0.08F) * 0.03F;
		float rightArmX = ARM_REST + swing * 0.9F * walk;
		float leftArmX = ARM_REST - swing * 0.9F * walk;
		float rightElbow = ELBOW_REST;
		float leftElbow = ELBOW_REST;
		float rightArmZ = 0.1F;
		float leftArmZ = -0.1F;
		float rightThigh = THIGH_REST - swing * 0.7F * walk;
		float leftThigh = THIGH_REST + swing * 0.7F * walk;
		float rightShinX = SHIN_REST;
		float leftShinX = SHIN_REST;
		float sink = Math.abs(Mth.sin(walkPos * 0.6662F)) * walk;

		switch (state.action) {
			case LEAP -> {
				// Crouch, then stretched out flat with the arms flung ahead; the arms drop to land as it falls.
				float stretch = Mth.clamp(t / 3.0F, 0.0F, 1.0F);
				float falling = Mth.clamp(-state.verticalSpeed * 2.0F, 0.0F, 1.0F);
				lean = Mth.lerp(stretch, LEAN, 1.35F);
				float reach = Mth.lerp(falling, -2.2F, -0.9F);
				rightArmX = Mth.lerp(stretch, ARM_REST, reach);
				leftArmX = rightArmX;
				rightElbow = leftElbow = 0.05F;
				rightArmZ = 0.35F;
				leftArmZ = -0.35F;
				rightThigh = leftThigh = Mth.lerp(stretch, THIGH_REST, 0.7F);
				rightShinX = leftShinX = Mth.lerp(stretch, SHIN_REST, 0.3F);
				sink = 0.0F;
			}
			case COMBO -> {
				// It rears up and rakes with one arm, the other, then the first again.
				lean = 0.55F;
				int[] hits = LurkerAction.COMBO_HITS;
				float r = Math.max(rake(t, hits[0]), rake(t, hits[2]));
				float l = rake(t, hits[1]);
				rightArmX = rakeAngle(r);
				leftArmX = rakeAngle(l);
				rightArmZ = rakeTwist(r);
				leftArmZ = -rakeTwist(l);
				rightElbow = leftElbow = 0.3F;
				sink = 1.5F;
			}
			case BURROW -> {
				// Digs with both arms, nose down, sinking into the ground.
				lean = 1.4F;
				rightArmX = -1.2F + Mth.sin(t * 1.6F) * 0.6F;
				leftArmX = -1.2F - Mth.sin(t * 1.6F) * 0.6F;
				sink = t / LurkerAction.BURROW.length() * 22.0F;
			}
			case NONE -> {
			}
		}
		body.xRot = lean;
		body.y = HIP_Y + sink;
		rightLeg.y = leftLeg.y = HIP_Y + sink;
		float cos = Mth.cos(lean);
		float sin = Mth.sin(lean);
		// The neck and shoulders, carried round by the torso's lean.
		float neckY = HIP_Y + sink - TORSO * cos;
		float neckZ = HIP_Z - TORSO * sin;
		head.y = neckY + 0.5F;
		head.z = neckZ - 0.5F;
		head.xRot = state.xRot * Mth.DEG_TO_RAD - 0.15F;
		head.yRot = state.yRot * Mth.DEG_TO_RAD;
		float shoulderY = HIP_Y + sink - SHOULDER_UP * cos;
		float shoulderZ = HIP_Z - SHOULDER_UP * sin;
		placeArm(rightArm, rightForearm, -SHOULDER_OUT, shoulderY, shoulderZ, rightArmX, rightArmZ, rightElbow);
		placeArm(leftArm, leftForearm, SHOULDER_OUT, shoulderY, shoulderZ, leftArmX, leftArmZ, leftElbow);
		rightLeg.xRot = rightThigh;
		leftLeg.xRot = leftThigh;
		rightShin.xRot = rightShinX;
		leftShin.xRot = leftShinX;
		rightLeg.zRot = 0.12F;
		leftLeg.zRot = -0.12F;

		// The robe flap hangs down whatever the lean, swaying as it moves; the skirt hangs over the hips.
		robeFront.xRot = -lean + 0.15F + Mth.sin(walkPos * 0.6662F) * 0.25F * walk + Mth.sin(age * 0.1F) * 0.05F;
		robeSkirt.xRot = 0.3F - lean + Mth.cos(walkPos * 0.6662F) * 0.2F * walk;
	}

	private static void placeArm(ModelPart arm, ModelPart forearm, float x, float y, float z, float xRot, float zRot, float elbow) {
		arm.x = x;
		arm.y = y;
		arm.z = z;
		arm.xRot = xRot;
		arm.zRot = zRot;
		forearm.xRot = -elbow;
	}

	/** How far through the rake on {@code hit} the arm is: 0 at rest, rising to 1 raised, then the strike back down. */
	private static float rake(float t, int hit) {
		float start = hit - 3.0F;
		if (t < start || t > hit + 2.0F) {
			return 0.0F;
		}
		if (t < hit - 0.5F) {
			return (t - start) / 2.5F;
		}
		// The strike: from raised (1) through to swept down past rest (-0.4), then back.
		float s = (t - (hit - 0.5F)) / 2.5F;
		return s < 0.4F ? Mth.lerp(s / 0.4F, 1.0F, -0.4F) : Mth.lerp((s - 0.4F) / 0.6F, -0.4F, 0.0F);
	}

	private static float rakeAngle(float rake) {
		return ARM_REST - 0.6F - rake * 2.0F;
	}

	private static float rakeTwist(float rake) {
		return 0.1F + rake * 0.5F;
	}
}
