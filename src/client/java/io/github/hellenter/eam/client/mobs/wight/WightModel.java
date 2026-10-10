package io.github.hellenter.eam.client.mobs.wight;

import io.github.hellenter.eam.Eam;

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
 * A gaunt, hunched man-shape: a thin torso leaning well forward, the skull thrust out ahead of it with a bared jaw and
 * lank hair hanging behind, arms so long the clawed hands hang below the knees, and bent legs on long bare feet. Dark
 * rags hang off it: a torn tunic tied with rope, a ragged skirt over the hips, a sleeve stub on each shoulder and a
 * strip of cloth hanging from the left forearm. Every part hangs from the root, placed from the torso's lean, so the
 * limbs swing in the world's frame.
 */
public class WightModel extends EntityModel<WightRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Eam.id("wight"), "main");
	private static final float HIP_Y = 8.6F;
	private static final float HIP_Z = 1.5F;
	private static final float TORSO = 11.0F;
	/** Forward lean of the torso from upright, standing and at a run. */
	private static final float LEAN = 0.75F;
	private static final float RUN_LEAN = 1.05F;
	private static final float SHOULDER_UP = 10.0F;
	private static final float SHOULDER_OUT = 4.6F;
	private static final float UPPER_ARM = 8.0F;
	private static final float FOREARM = 7.0F;
	private static final float THIGH = 7.0F;
	private static final float SHIN = 9.0F;
	/** Knees bent forward, shins back: a permanent crouch. */
	private static final float THIGH_REST = -0.55F;
	private static final float SHIN_REST = 0.9F;
	private static final float ARM_REST = -0.15F;
	private static final float ELBOW_REST = 0.45F;

	private final ModelPart body;
	private final ModelPart skirt;
	private final ModelPart head;
	private final ModelPart jaw;
	private final ModelPart hair;
	private final ModelPart rightArm;
	private final ModelPart leftArm;
	private final ModelPart rightForearm;
	private final ModelPart leftForearm;
	private final ModelPart leftRag;
	private final ModelPart rightLeg;
	private final ModelPart leftLeg;
	private final ModelPart rightShin;
	private final ModelPart leftShin;
	private final ModelPart rightFoot;
	private final ModelPart leftFoot;

	public WightModel(ModelPart root) {
		super(root);
		body = root.getChild("body");
		skirt = body.getChild("skirt");
		head = root.getChild("head");
		jaw = head.getChild("jaw");
		hair = head.getChild("hair");
		rightArm = root.getChild("right_arm");
		leftArm = root.getChild("left_arm");
		rightForearm = rightArm.getChild("right_forearm");
		leftForearm = leftArm.getChild("left_forearm");
		leftRag = leftForearm.getChild("rag");
		rightLeg = root.getChild("right_leg");
		leftLeg = root.getChild("left_leg");
		rightShin = rightLeg.getChild("right_shin");
		leftShin = leftLeg.getChild("left_shin");
		rightFoot = rightShin.getChild("right_foot");
		leftFoot = leftShin.getChild("left_foot");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
						// Ribs and stretched skin, and the torn tunic over them.
						.texOffs(0, 16).addBox(-3.5F, -TORSO, -2.0F, 7.0F, TORSO, 4.0F)
						.texOffs(22, 16).addBox(-4.0F, -TORSO + 0.5F, -2.5F, 8.0F, 10.0F, 5.0F),
				PartPose.offsetAndRotation(0.0F, HIP_Y, HIP_Z, LEAN, 0.0F, 0.0F));
		body.addOrReplaceChild("skirt", CubeListBuilder.create().texOffs(0, 32).addBox(-4.0F, 0.0F, -2.5F, 8.0F, 7.0F, 5.0F),
				PartPose.offset(0.0F, -1.0F, 0.0F));
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
						.texOffs(0, 0).addBox(-3.5F, -6.0F, -6.0F, 7.0F, 7.0F, 7.0F),
				PartPose.ZERO);
		// The lower jaw, its teeth bared.
		head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(28, 0).addBox(-2.5F, 0.0F, -3.0F, 5.0F, 2.0F, 3.0F),
				PartPose.offset(0.0F, 1.0F, -3.0F));
		// Lank hair hanging from the back of the skull.
		head.addOrReplaceChild("hair", CubeListBuilder.create().texOffs(44, 0).addBox(-3.5F, 0.0F, 0.0F, 7.0F, 9.0F, 1.0F),
				PartPose.offset(0.0F, -6.0F, 1.0F));
		CubeListBuilder arm = CubeListBuilder.create()
				.texOffs(48, 16).addBox(-1.5F, -1.0F, -1.5F, 3.0F, UPPER_ARM, 3.0F)
				// A torn sleeve stub.
				.texOffs(36, 44).addBox(-2.0F, -1.5F, -2.0F, 4.0F, 4.0F, 4.0F);
		CubeListBuilder forearm = CubeListBuilder.create()
				.texOffs(26, 32).addBox(-1.0F, 0.0F, -1.0F, 2.0F, FOREARM, 2.0F)
				// The hand, and three long claws off its front.
				.texOffs(34, 32).addBox(-1.5F, FOREARM - 0.5F, -1.5F, 3.0F, 4.0F, 3.0F)
				.texOffs(46, 32).addBox(-1.5F, FOREARM + 3.5F, -1.5F, 1.0F, 3.0F, 1.0F)
				.texOffs(46, 32).addBox(-0.5F, FOREARM + 3.5F, -1.5F, 1.0F, 3.0F, 1.0F)
				.texOffs(46, 32).addBox(0.5F, FOREARM + 3.5F, -1.5F, 1.0F, 3.0F, 1.0F);
		root.addOrReplaceChild("right_arm", arm, PartPose.ZERO)
				.addOrReplaceChild("right_forearm", forearm, PartPose.offset(0.0F, UPPER_ARM - 1.0F, 0.0F));
		PartDefinition leftForearm = root.addOrReplaceChild("left_arm", arm.mirror(), PartPose.ZERO)
				.addOrReplaceChild("left_forearm", forearm.mirror(), PartPose.offset(0.0F, UPPER_ARM - 1.0F, 0.0F));
		// A strip of rag tied round the left forearm, hanging off its outside.
		leftForearm.addOrReplaceChild("rag", CubeListBuilder.create().texOffs(52, 44).addBox(0.0F, 0.0F, -1.5F, 1.0F, 8.0F, 3.0F),
				PartPose.offset(1.0F, 3.0F, 0.0F));
		CubeListBuilder thigh = CubeListBuilder.create().texOffs(0, 44).addBox(-1.5F, 0.0F, -1.5F, 3.0F, THIGH, 3.0F);
		CubeListBuilder shin = CubeListBuilder.create().texOffs(12, 44).addBox(-1.0F, 0.0F, -1.0F, 2.0F, SHIN, 2.0F);
		CubeListBuilder foot = CubeListBuilder.create().texOffs(20, 44).addBox(-1.5F, 0.0F, -4.0F, 3.0F, 1.0F, 5.0F);
		root.addOrReplaceChild("right_leg", thigh, PartPose.offset(-2.0F, HIP_Y, HIP_Z))
				.addOrReplaceChild("right_shin", shin, PartPose.offset(0.0F, THIGH, 0.0F))
				.addOrReplaceChild("right_foot", foot, PartPose.offset(0.0F, SHIN, 0.0F));
		root.addOrReplaceChild("left_leg", thigh.mirror(), PartPose.offset(2.0F, HIP_Y, HIP_Z))
				.addOrReplaceChild("left_shin", shin.mirror(), PartPose.offset(0.0F, THIGH, 0.0F))
				.addOrReplaceChild("left_foot", foot.mirror(), PartPose.offset(0.0F, SHIN, 0.0F));
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(WightRenderState state) {
		super.setupAnim(state);
		float walkPos = state.walkAnimationPos;
		float walk = Math.min(state.walkAnimationSpeed, 1.0F);
		float swing = Mth.cos(walkPos * 0.6662F);
		float age = state.ageInTicks;

		float lean = Mth.lerp(walk, LEAN, RUN_LEAN) + Mth.sin(age * 0.07F) * 0.03F;
		float rightArmX = ARM_REST + swing * 0.8F * walk;
		float leftArmX = ARM_REST - swing * 0.8F * walk;
		float rightArmZ = 0.12F;
		float leftArmZ = -0.12F;
		float rightElbow = ELBOW_REST + Math.max(0.0F, swing) * 0.4F * walk;
		float leftElbow = ELBOW_REST + Math.max(0.0F, -swing) * 0.4F * walk;
		float rightThigh = THIGH_REST - swing * 0.9F * walk;
		float leftThigh = THIGH_REST + swing * 0.9F * walk;
		float rightShinX = SHIN_REST + Math.max(0.0F, swing) * 0.6F * walk;
		float leftShinX = SHIN_REST + Math.max(0.0F, -swing) * 0.6F * walk;
		float jawOpen = 0.15F + Mth.sin(age * 0.05F) * 0.05F;
		float headTwitch = Mth.sin(age * 0.9F) * Mth.sin(age * 0.23F) * 0.04F;

		if (state.displayFireAnimation) {
			// Burning: it straightens up, beating at itself with both arms, head thrown back, jaw wide.
			lean = 0.2F + Mth.sin(age * 0.6F) * 0.08F;
			rightArmX = -2.2F + Mth.sin(age * 1.3F) * 0.9F;
			leftArmX = -2.2F + Mth.sin(age * 1.3F + 2.0F) * 0.9F;
			rightArmZ = 0.5F + Mth.sin(age * 0.9F) * 0.4F;
			leftArmZ = -0.5F - Mth.sin(age * 0.9F + 1.0F) * 0.4F;
			rightElbow = leftElbow = 1.2F;
			jawOpen = 0.6F + Mth.sin(age * 0.8F) * 0.15F;
			headTwitch = Mth.sin(age * 1.7F) * 0.25F;
		} else if (state.swing > 0.0F) {
			// A raking blow of the right claws from high over the shoulder, the left arm flung back.
			float s = state.swing;
			float arc = Mth.sin(s * Mth.PI);
			float down = Mth.sin(Mth.sqrt(s) * Mth.PI);
			rightArmX = Mth.lerp(down, ARM_REST, -2.6F) + s * 1.6F * arc;
			rightArmZ = 0.12F + arc * 0.3F;
			rightElbow = 0.1F + arc * 0.3F;
			leftArmX = ARM_REST + arc * 0.6F;
			lean -= arc * 0.25F;
			jawOpen = 0.5F;
		}

		body.xRot = lean;
		float cos = Mth.cos(lean);
		float sin = Mth.sin(lean);
		float neckY = HIP_Y - TORSO * cos;
		float neckZ = HIP_Z - TORSO * sin;
		head.x = 0.0F;
		head.y = neckY + 2.5F;
		head.z = neckZ - 1.0F;
		head.xRot = state.xRot * Mth.DEG_TO_RAD - 0.1F + headTwitch;
		head.yRot = state.yRot * Mth.DEG_TO_RAD;
		head.zRot = headTwitch * 0.5F;
		jaw.xRot = jawOpen;
		// The hair hangs down whatever the skull does.
		hair.xRot = -head.xRot + 0.1F + Mth.sin(walkPos * 0.6662F) * 0.15F * walk;

		float shoulderY = HIP_Y - SHOULDER_UP * cos;
		float shoulderZ = HIP_Z - SHOULDER_UP * sin;
		placeArm(rightArm, rightForearm, -SHOULDER_OUT, shoulderY, shoulderZ, rightArmX, rightArmZ, rightElbow);
		placeArm(leftArm, leftForearm, SHOULDER_OUT, shoulderY, shoulderZ, leftArmX, leftArmZ, leftElbow);
		leftRag.xRot = -(leftArmX - leftElbow) + Mth.sin(age * 0.11F) * 0.08F;
		leftRag.zRot = -leftArmZ;

		rightLeg.xRot = rightThigh;
		leftLeg.xRot = leftThigh;
		rightLeg.zRot = 0.12F;
		leftLeg.zRot = -0.12F;
		rightShin.xRot = rightShinX;
		leftShin.xRot = leftShinX;
		// Feet stay about flat on the ground.
		rightFoot.xRot = -(rightThigh + rightShinX);
		leftFoot.xRot = -(leftThigh + leftShinX);
		// The skirt hangs over the hips.
		skirt.xRot = 0.25F - lean + Mth.cos(walkPos * 0.6662F) * 0.15F * walk;
	}

	private static void placeArm(ModelPart arm, ModelPart forearm, float x, float y, float z, float xRot, float zRot, float elbow) {
		arm.x = x;
		arm.y = y;
		arm.z = z;
		arm.xRot = xRot;
		arm.zRot = zRot;
		forearm.xRot = -elbow;
	}
}
