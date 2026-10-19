package io.github.hellenter.eam.client.mobs.leshy;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.entity.leshy.Leshy;

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
 * The leshy, after the leshen of the old tales (and of the Witcher games): a towering, stooped body of bark and knotted
 * roots under a mantle of moss and an old pelt, the bleached skull of a stag for a head, with a long muzzle, a loose jaw
 * and great branching antlers; arms so long the twig claws hang to its knees, and legs like gnarled trunks splaying into
 * roots. The torso leans from the hips, the head and arms ride on it.
 */
public class LeshyModel extends EntityModel<LeshyRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Eam.id("leshy"), "main");
	private static final float HIP_Y = 5.0F;
	private static final float HIP_Z = 1.5F;
	/** Forward stoop of the torso while it haunts, and in a fight. */
	private static final float LEAN = 0.3F;
	private static final float ANGRY_LEAN = 0.5F;
	private static final float THIGH_REST = -0.3F;
	private static final float SHIN_REST = 0.45F;
	private static final float ELBOW_REST = -0.35F;

	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart jaw;
	private final ModelPart rightArm;
	private final ModelPart leftArm;
	private final ModelPart rightForearm;
	private final ModelPart leftForearm;
	private final ModelPart[] rightFingers = new ModelPart[3];
	private final ModelPart[] leftFingers = new ModelPart[3];
	private final ModelPart rightLeg;
	private final ModelPart leftLeg;
	private final ModelPart rightShin;
	private final ModelPart leftShin;
	private final ModelPart rightFoot;
	private final ModelPart leftFoot;
	private final ModelPart pelt;
	private final ModelPart beard;

	public LeshyModel(ModelPart root) {
		super(root);
		body = root.getChild("body");
		pelt = body.getChild("pelt");
		beard = body.getChild("beard");
		head = body.getChild("head");
		jaw = head.getChild("jaw");
		rightArm = body.getChild("right_arm");
		leftArm = body.getChild("left_arm");
		rightForearm = rightArm.getChild("forearm");
		leftForearm = leftArm.getChild("forearm");
		ModelPart rightHand = rightForearm.getChild("hand");
		ModelPart leftHand = leftForearm.getChild("hand");
		for (int i = 0; i < 3; i++) {
			rightFingers[i] = rightHand.getChild("finger" + i);
			leftFingers[i] = leftHand.getChild("finger" + i);
		}
		rightLeg = root.getChild("right_leg");
		leftLeg = root.getChild("left_leg");
		rightShin = rightLeg.getChild("shin");
		leftShin = leftLeg.getChild("shin");
		rightFoot = rightShin.getChild("foot");
		leftFoot = leftShin.getChild("foot");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
						// The trunk of the body, knotted bark over a cage of roots.
						.texOffs(0, 0).addBox(-5.0F, -14.0F, -3.0F, 10.0F, 14.0F, 6.0F)
						// Ribs of root standing out of the chest.
						.texOffs(0, 20).addBox(-4.0F, -13.0F, -4.0F, 8.0F, 8.0F, 1.0F)
						// A thick mantle of moss and lichen over the shoulders.
						.texOffs(32, 0).addBox(-7.0F, -16.0F, -4.5F, 14.0F, 6.0F, 9.0F)
						// A skirt of hanging roots and moss over the hips.
						.texOffs(0, 30).addBox(-5.0F, -1.0F, -3.5F, 10.0F, 6.0F, 7.0F),
				PartPose.offset(0.0F, HIP_Y, HIP_Z));
		// An old pelt down its back, and a beard of moss down its chest; both hang free.
		body.addOrReplaceChild("pelt", CubeListBuilder.create().texOffs(32, 15).addBox(-6.0F, 0.0F, 0.0F, 12.0F, 13.0F, 1.0F),
				PartPose.offset(0.0F, -12.0F, 3.5F));
		body.addOrReplaceChild("beard", CubeListBuilder.create().texOffs(58, 15).addBox(-3.0F, 0.0F, -1.0F, 6.0F, 9.0F, 1.0F),
				PartPose.offset(0.0F, -9.0F, -3.5F));

		PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
						// The neck, the skull and the long muzzle of a stag.
						.texOffs(72, 15).addBox(-1.5F, -1.5F, -4.0F, 3.0F, 3.0F, 5.0F)
						.texOffs(80, 0).addBox(-3.0F, -5.0F, -9.0F, 6.0F, 6.0F, 6.0F)
						.texOffs(88, 15).addBox(-2.0F, -4.0F, -15.0F, 4.0F, 4.0F, 6.0F)
						// Moss hanging off the back of the skull.
						.texOffs(104, 0).addBox(-3.5F, -4.0F, -3.5F, 7.0F, 8.0F, 1.0F),
				PartPose.offset(0.0F, -15.0F, -3.0F));
		head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(88, 25).addBox(-1.5F, 0.0F, -6.0F, 3.0F, 1.0F, 6.0F),
				PartPose.offset(0.0F, 1.0F, -8.5F));
		antler(head, -1.0F);
		antler(head, 1.0F);

		arm(body, "right_arm", -1.0F);
		arm(body, "left_arm", 1.0F);
		leg(root, "right_leg", -1.0F);
		leg(root, "left_leg", 1.0F);
		return LayerDefinition.create(mesh, 128, 128);
	}

	/** A bone of the antlers, {@code length} long, pointing up from its pivot. */
	private static CubeListBuilder tine(float length) {
		return CubeListBuilder.create().texOffs(120, 0).addBox(-0.5F, -length, -0.5F, 1.0F, length, 1.0F);
	}

	/** A great antler on the {@code side} of the skull (-1 right, 1 left): a beam curving up, out and back, and its tines. */
	private static void antler(PartDefinition head, float side) {
		String name = side < 0 ? "right" : "left";
		PartDefinition beam = head.addOrReplaceChild(name + "_antler", tine(7.0F),
				PartPose.offsetAndRotation(side * 2.2F, -4.5F, -5.5F, -0.35F, 0.0F, side * 0.75F));
		// The brow tine, forward over the eyes.
		beam.addOrReplaceChild("brow", tine(4.0F), PartPose.offsetAndRotation(0.0F, -2.0F, 0.0F, 1.2F, 0.0F, -side * 0.2F));
		PartDefinition upper = beam.addOrReplaceChild("upper", tine(6.0F),
				PartPose.offsetAndRotation(0.0F, -7.0F, 0.0F, -0.25F, 0.0F, -side * 0.55F));
		upper.addOrReplaceChild("bez", tine(4.0F), PartPose.offsetAndRotation(0.0F, -2.0F, 0.0F, 0.9F, 0.0F, side * 0.35F));
		upper.addOrReplaceChild("back", tine(4.0F), PartPose.offsetAndRotation(0.0F, -4.0F, 0.0F, -0.7F, 0.0F, side * 0.4F));
		PartDefinition crown = upper.addOrReplaceChild("crown", tine(5.0F),
				PartPose.offsetAndRotation(0.0F, -6.0F, 0.0F, 0.15F, 0.0F, side * 0.45F));
		crown.addOrReplaceChild("fork_a", tine(3.0F), PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.5F, 0.0F, -side * 0.3F));
		crown.addOrReplaceChild("fork_b", tine(3.0F), PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, -0.4F, 0.0F, side * 0.5F));
	}

	/** A long arm on the {@code side} of the torso: moss on the shoulder, bark forearm and three twig claws. */
	private static void arm(PartDefinition body, String name, float side) {
		CubeListBuilder upperArm = CubeListBuilder.create()
				.texOffs(0, 44).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 11.0F, 3.0F)
				.texOffs(12, 44).addBox(-3.0F, -2.5F, -3.0F, 6.0F, 4.0F, 6.0F)
				// A strip of moss hanging off the outside of the arm.
				.texOffs(36, 44).addBox(side < 0 ? -2.5F : 1.5F, 0.0F, -1.0F, 1.0F, 8.0F, 2.0F);
		PartDefinition arm = body.addOrReplaceChild(name, side < 0 ? upperArm : upperArm.mirror(),
				PartPose.offset(side * 6.5F, -13.0F, -0.5F));
		PartDefinition forearm = arm.addOrReplaceChild("forearm",
				CubeListBuilder.create().texOffs(44, 44).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F), PartPose.offset(0.0F, 10.0F, 0.0F));
		PartDefinition hand = forearm.addOrReplaceChild("hand",
				CubeListBuilder.create().texOffs(52, 44).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 2.0F, 3.0F), PartPose.offset(0.0F, 12.0F, 0.0F));
		for (int i = 0; i < 3; i++) {
			hand.addOrReplaceChild("finger" + i, CubeListBuilder.create().texOffs(64, 44).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 6.0F, 1.0F),
					PartPose.offset(-1.0F + i, 1.5F, -1.0F));
		}
	}

	/** A leg like a gnarled trunk, its foot splaying into roots. */
	private static void leg(PartDefinition root, String name, float side) {
		PartDefinition thigh = root.addOrReplaceChild(name,
				CubeListBuilder.create().texOffs(0, 60).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 9.0F, 4.0F), PartPose.offset(side * 2.5F, HIP_Y, HIP_Z));
		PartDefinition shin = thigh.addOrReplaceChild("shin",
				CubeListBuilder.create().texOffs(16, 60).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 10.0F, 3.0F), PartPose.offset(0.0F, 8.5F, 0.0F));
		shin.addOrReplaceChild("foot", CubeListBuilder.create()
						.texOffs(28, 60).addBox(-2.0F, -1.0F, -4.0F, 4.0F, 1.0F, 6.0F)
						.texOffs(48, 60).addBox(-2.5F, -0.5F, -6.0F, 1.0F, 1.0F, 3.0F)
						.texOffs(48, 60).addBox(1.5F, -0.5F, -6.0F, 1.0F, 1.0F, 3.0F),
				PartPose.offset(0.0F, 10.5F, 0.0F));
	}

	@Override
	public void setupAnim(LeshyRenderState state) {
		super.setupAnim(state);
		float walkPos = state.walkAnimationPos;
		float walk = Math.min(state.walkAnimationSpeed, 1.0F);
		// A slow, heavy stride.
		float swing = Mth.cos(walkPos * 0.5F);
		float age = state.ageInTicks;

		float lean = (state.angry ? ANGRY_LEAN : LEAN) + Mth.sin(age * 0.05F) * 0.03F + walk * 0.1F;
		float headPitch = state.xRot * Mth.DEG_TO_RAD;
		float headYaw = state.yRot * Mth.DEG_TO_RAD;
		// Arm angles are in the world's frame: 0 hangs straight down, negative swings forward.
		float rightArmX = -0.15F + swing * 0.45F * walk;
		float leftArmX = -0.15F - swing * 0.45F * walk;
		float rightArmZ = state.angry ? 0.35F : 0.15F;
		float leftArmZ = -rightArmZ;
		float rightElbow = ELBOW_REST - Math.max(0.0F, swing) * 0.3F * walk;
		float leftElbow = ELBOW_REST - Math.max(0.0F, -swing) * 0.3F * walk;
		float jawOpen = 0.1F + Mth.sin(age * 0.04F) * 0.05F;
		float curl = 0.3F + Mth.sin(age * 0.11F) * 0.15F;

		float t = state.actionTime;
		switch (state.action) {
			case ROOTS -> {
				if (t < Leshy.ROOTS_AT) {
					// Both arms raised high, the body thrown back.
					float up = Mth.clamp(t / (Leshy.ROOTS_AT - 2.0F), 0.0F, 1.0F);
					lean = Mth.lerp(up, lean, 0.0F);
					rightArmX = leftArmX = Mth.lerp(up, rightArmX, -2.8F);
					rightArmZ = Mth.lerp(up, rightArmZ, 0.25F);
					leftArmZ = -rightArmZ;
					rightElbow = leftElbow = Mth.lerp(up, ELBOW_REST, -0.2F);
					jawOpen = Mth.lerp(up, jawOpen, 0.7F);
					curl = 0.0F;
				} else {
					// Down it plunges, claws driven into the ground.
					float down = Mth.clamp((t - Leshy.ROOTS_AT) / 3.0F, 0.0F, 1.0F);
					lean = Mth.lerp(down, 0.0F, 1.1F);
					rightArmX = leftArmX = Mth.lerp(down, -2.8F, -0.35F);
					rightArmZ = 0.2F;
					leftArmZ = -0.2F;
					rightElbow = leftElbow = -0.05F;
					jawOpen = 0.4F;
					curl = 0.9F;
				}
			}
			case HOWL -> {
				// Head thrown back and arms flung wide as it calls the wolves.
				float k = Mth.clamp(t / 6.0F, 0.0F, 1.0F) * Mth.clamp((Leshy.HOWL_TIME - t) / 6.0F, 0.0F, 1.0F);
				lean = Mth.lerp(k, lean, -0.1F);
				headPitch = Mth.lerp(k, headPitch, -0.9F);
				rightArmX = leftArmX = Mth.lerp(k, rightArmX, -0.6F);
				rightArmZ = Mth.lerp(k, rightArmZ, 1.3F);
				leftArmZ = -rightArmZ;
				jawOpen = Mth.lerp(k, jawOpen, 0.75F + Mth.sin(age * 1.5F) * 0.08F);
				curl = 0.0F;
			}
			case EAT -> {
				// Bent over, it feeds the bread into its jaw with one claw.
				lean = 0.75F;
				headPitch = 0.4F;
				rightArmX = -1.4F;
				rightElbow = -1.7F;
				rightArmZ = -0.3F;
				jawOpen = 0.25F + Math.abs(Mth.sin(t * 0.5F)) * 0.35F;
			}
			default -> {
				if (state.swing > 0.0F) {
					// A raking blow of the right claws from over the shoulder, the left arm swung back.
					float s = state.swing;
					float arc = Mth.sin(s * Mth.PI);
					float down = Mth.sin(Mth.sqrt(s) * Mth.PI);
					rightArmX = Mth.lerp(down, rightArmX, -2.7F) + s * 1.8F * arc;
					rightArmZ = 0.2F + arc * 0.35F;
					rightElbow = -0.1F - arc * 0.2F;
					leftArmX = 0.3F + arc * 0.5F;
					lean += arc * 0.2F;
					jawOpen = 0.55F;
					curl = 0.0F;
				}
			}
		}

		body.xRot = lean;
		// The head looks where it looks whatever the stoop.
		head.xRot = headPitch - lean;
		head.yRot = headYaw;
		jaw.xRot = jawOpen;
		// Arms hang from the shoulders in the world's frame, then swing.
		rightArm.xRot = rightArmX - lean;
		leftArm.xRot = leftArmX - lean;
		rightArm.zRot = rightArmZ;
		leftArm.zRot = leftArmZ;
		rightForearm.xRot = rightElbow;
		leftForearm.xRot = leftElbow;
		for (int i = 0; i < 3; i++) {
			float spread = (i - 1) * 0.2F;
			rightFingers[i].xRot = -curl - Mth.sin(age * 0.2F + i) * 0.05F;
			leftFingers[i].xRot = -curl - Mth.sin(age * 0.2F + i + 1.5F) * 0.05F;
			rightFingers[i].zRot = spread;
			leftFingers[i].zRot = spread;
		}
		// The pelt and the beard hang straight down, swaying.
		pelt.xRot = -lean + 0.1F + Mth.cos(walkPos * 0.5F) * 0.08F * walk + Mth.sin(age * 0.07F) * 0.03F;
		beard.xRot = -lean + Mth.sin(age * 0.09F) * 0.05F;

		float rightThigh = THIGH_REST - swing * 0.55F * walk;
		float leftThigh = THIGH_REST + swing * 0.55F * walk;
		float rightShinX = SHIN_REST + Math.max(0.0F, swing) * 0.5F * walk;
		float leftShinX = SHIN_REST + Math.max(0.0F, -swing) * 0.5F * walk;
		if (state.action == Leshy.Action.ROOTS && t >= Leshy.ROOTS_AT) {
			// Knees bent deep as it drives its claws down.
			rightThigh = leftThigh = -0.8F;
			rightShinX = leftShinX = 1.2F;
		}
		rightLeg.xRot = rightThigh;
		leftLeg.xRot = leftThigh;
		rightLeg.zRot = 0.06F;
		leftLeg.zRot = -0.06F;
		rightShin.xRot = rightShinX;
		leftShin.xRot = leftShinX;
		rightFoot.xRot = -(rightThigh + rightShinX);
		leftFoot.xRot = -(leftThigh + leftShinX);
		// Bent knees bring the hips down, so the feet stay on the ground.
		float drop = legLength(THIGH_REST, SHIN_REST) - legLength(rightThigh, rightShinX);
		if (state.action != Leshy.Action.ROOTS) {
			drop = 0.0F;
		}
		body.y = HIP_Y + drop;
		rightLeg.y = HIP_Y + drop;
		leftLeg.y = HIP_Y + drop;
	}

	/** How far below the hip the foot of a leg bent like this reaches. */
	private static float legLength(float thigh, float shin) {
		return 9.0F * Mth.cos(thigh) + 10.0F * Mth.cos(thigh + shin);
	}
}
