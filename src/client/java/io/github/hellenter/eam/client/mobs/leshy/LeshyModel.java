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
 * The leshy, after the leshen of the old tales (and of the Witcher games): a gaunt, towering body of bark, its ribs of
 * root standing out over a hollow chest, wrapped in rotten rags and a rope belt, a bone stuck through its shoulder and a
 * little skull hung off the other; the bleached skull of a stag for a head, hung low on a short neck, with a long toothed
 * muzzle, a loose jaw and a wide crown of antlers; arms so long the hooked claws hang to its knees, and thin legs like
 * gnarled saplings splaying into root toes. The torso leans from the hips, the head and arms ride on it.
 */
public class LeshyModel extends EntityModel<LeshyRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Eam.id("leshy"), "main");
	private static final float THIGH_LENGTH = 12.0F;
	private static final float SHIN_LENGTH = 13.0F;
	private static final float THIGH_REST = -0.3F;
	private static final float SHIN_REST = 0.45F;
	/** The hips sit so that the feet of legs at rest touch the ground (24 in model space). */
	private static final float HIP_Y = 24.0F - legLength(THIGH_REST, SHIN_REST);
	private static final float HIP_Z = 1.0F;
	/** Forward stoop of the torso while it haunts, and in a fight. */
	private static final float LEAN = 0.3F;
	private static final float ANGRY_LEAN = 0.5F;
	/** The skull hangs a little low, muzzle down. */
	private static final float HEAD_DROOP = 0.25F;
	private static final float ELBOW_REST = -0.35F;
	private static final int FINGERS = 4;

	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart jaw;
	private final ModelPart rightArm;
	private final ModelPart leftArm;
	private final ModelPart rightForearm;
	private final ModelPart leftForearm;
	private final ModelPart[] rightFingers = new ModelPart[FINGERS];
	private final ModelPart[] leftFingers = new ModelPart[FINGERS];
	private final ModelPart[] rightClaws = new ModelPart[FINGERS];
	private final ModelPart[] leftClaws = new ModelPart[FINGERS];
	private final ModelPart rightLeg;
	private final ModelPart leftLeg;
	private final ModelPart rightShin;
	private final ModelPart leftShin;
	private final ModelPart rightFoot;
	private final ModelPart leftFoot;
	private final ModelPart ragFront;
	private final ModelPart ragBack;
	private final ModelPart ragRight;
	private final ModelPart ragLeft;
	private final ModelPart charm;
	private final ModelPart headMoss;

	public LeshyModel(ModelPart root) {
		super(root);
		body = root.getChild("body");
		ragFront = body.getChild("rag_front");
		ragBack = body.getChild("rag_back");
		ragRight = body.getChild("rag_right");
		ragLeft = body.getChild("rag_left");
		charm = body.getChild("charm");
		head = body.getChild("head");
		jaw = head.getChild("jaw");
		headMoss = head.getChild("moss");
		rightArm = body.getChild("right_arm");
		leftArm = body.getChild("left_arm");
		rightForearm = rightArm.getChild("forearm");
		leftForearm = leftArm.getChild("forearm");
		ModelPart rightHand = rightForearm.getChild("hand");
		ModelPart leftHand = leftForearm.getChild("hand");
		for (int i = 0; i < FINGERS; i++) {
			rightFingers[i] = rightHand.getChild("finger" + i);
			leftFingers[i] = leftHand.getChild("finger" + i);
			rightClaws[i] = rightFingers[i].getChild("claw");
			leftClaws[i] = leftFingers[i].getChild("claw");
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
		CubeListBuilder torso = CubeListBuilder.create()
				// Narrow hips and a gaunt waist of bark.
				.texOffs(47, 36).addBox(-3.0F, -3.0F, -2.5F, 6.0F, 3.0F, 5.0F)
				.texOffs(98, 24).addBox(-2.5F, -8.0F, -2.0F, 5.0F, 5.0F, 4.0F)
				// The hollow of the chest, dark inside the cage of ribs.
				.texOffs(70, 0).addBox(-3.0F, -16.0F, -2.0F, 6.0F, 8.0F, 4.0F)
				// The breastbone, a twisted root.
				.texOffs(70, 36).addBox(-0.5F, -16.0F, -3.5F, 1.0F, 7.0F, 1.0F)
				// Bony shoulders.
				.texOffs(75, 36).addBox(-5.5F, -18.0F, -2.5F, 11.0F, 3.0F, 5.0F)
				// The rope belt.
				.texOffs(74, 46).addBox(-3.5F, -3.5F, -3.0F, 7.0F, 1.0F, 6.0F)
				// Moss grown over the shoulders.
				.texOffs(108, 36).addBox(-6.0F, -19.0F, -3.0F, 4.0F, 2.0F, 6.0F)
				.texOffs(108, 36).mirror().addBox(2.0F, -19.0F, -3.0F, 4.0F, 2.0F, 6.0F).mirror(false);
		// Ribs of root standing out over the chest.
		for (int i = 0; i < 4; i++) {
			torso.texOffs(43, 46).addBox(-4.5F, -15.0F + i * 2.0F, -3.0F, 9.0F, 1.0F, 6.0F);
		}
		// Knots of the spine down the back.
		for (int i = 0; i < 5; i++) {
			torso.texOffs(102, 55).addBox(-0.5F, -17.0F + i * 3.0F, 2.0F, 1.0F, 1.0F, 1.0F);
		}
		PartDefinition body = root.addOrReplaceChild("body", torso, PartPose.offset(0.0F, HIP_Y, HIP_Z));
		// A rotten rag wound across the chest from the right shoulder.
		body.addOrReplaceChild("sash", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -8.0F, -3.5F, 3.0F, 16.0F, 7.0F),
				PartPose.offsetAndRotation(0.0F, -11.0F, 0.0F, 0.0F, 0.0F, 0.75F));
		// Tatters hanging from the belt, front, back and sides; they hang free.
		body.addOrReplaceChild("rag_front", CubeListBuilder.create().texOffs(30, 24).addBox(-2.5F, 0.0F, 0.0F, 5.0F, 12.0F, 0.0F),
				PartPose.offset(0.0F, -3.0F, -3.1F));
		body.addOrReplaceChild("rag_back", CubeListBuilder.create().texOffs(91, 0).addBox(-3.0F, 0.0F, 0.0F, 6.0F, 15.0F, 0.0F),
				PartPose.offset(0.0F, -3.0F, 3.1F));
		body.addOrReplaceChild("rag_right", CubeListBuilder.create().texOffs(104, 0).addBox(0.0F, 0.0F, -2.0F, 0.0F, 10.0F, 4.0F),
				PartPose.offset(-3.6F, -3.0F, 0.0F));
		body.addOrReplaceChild("rag_left", CubeListBuilder.create().texOffs(104, 0).mirror().addBox(0.0F, 0.0F, -2.0F, 0.0F, 10.0F, 4.0F),
				PartPose.offset(3.6F, -3.0F, 0.0F));
		// An old bone driven through the left shoulder, sticking up behind the head.
		body.addOrReplaceChild("bone", CubeListBuilder.create()
						.texOffs(21, 0).addBox(-0.5F, -13.0F, -0.5F, 1.0F, 14.0F, 1.0F)
						.texOffs(71, 55).addBox(-1.0F, -14.5F, -1.0F, 2.0F, 2.0F, 2.0F)
						.texOffs(71, 55).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F),
				PartPose.offsetAndRotation(4.0F, -14.0F, 1.5F, -0.35F, 0.0F, 0.45F));
		// A small skull hung on a cord off the right shoulder.
		PartDefinition charm = body.addOrReplaceChild("charm",
				CubeListBuilder.create().texOffs(80, 55).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 3.0F, 0.0F), PartPose.offset(-4.0F, -17.5F, -3.0F));
		charm.addOrReplaceChild("skull", CubeListBuilder.create().texOffs(62, 55).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F),
				PartPose.offset(0.0F, 3.0F, 0.0F));

		PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
						// The neck, knotted vertebrae of wood.
						.texOffs(0, 36).addBox(-1.5F, -1.5F, -2.0F, 3.0F, 3.0F, 3.0F)
						// The skull of a stag: crown, brow ridge, long muzzle and the end of the nose.
						.texOffs(0, 24).addBox(-3.0F, -4.0F, -7.0F, 6.0F, 5.0F, 6.0F)
						.texOffs(83, 55).addBox(-3.5F, -4.5F, -8.0F, 7.0F, 1.0F, 2.0F)
						.texOffs(41, 24).addBox(-2.0F, -3.0F, -13.0F, 4.0F, 3.0F, 6.0F)
						.texOffs(26, 55).addBox(-1.5F, -2.5F, -15.0F, 3.0F, 2.0F, 2.0F)
						// The upper teeth.
						.texOffs(19, 36).addBox(-1.5F, 0.0F, -14.0F, 3.0F, 1.0F, 6.0F),
				PartPose.offset(0.0F, -17.5F, -2.0F));
		head.addOrReplaceChild("jaw", CubeListBuilder.create()
						.texOffs(64, 24).addBox(-1.5F, 0.0F, -7.5F, 3.0F, 1.0F, 7.0F)
						.texOffs(0, 46).addBox(-1.0F, -1.0F, -7.0F, 2.0F, 1.0F, 5.0F),
				PartPose.offset(0.0F, 1.0F, -7.0F));
		// Moss hanging off the back of the skull.
		head.addOrReplaceChild("moss", CubeListBuilder.create().texOffs(101, 46).addBox(-3.0F, 0.0F, 0.0F, 6.0F, 7.0F, 0.0F),
				PartPose.offset(0.0F, -2.0F, -1.5F));
		antler(head, -1.0F);
		antler(head, 1.0F);

		arm(body, "right_arm", -1.0F);
		arm(body, "left_arm", 1.0F);
		leg(root, "right_leg", -1.0F);
		leg(root, "left_leg", 1.0F);
		return LayerDefinition.create(mesh, 128, 64);
	}

	/** A tine of the antlers, {@code length} long, pointing up from its pivot. */
	private static CubeListBuilder tine(float length) {
		return CubeListBuilder.create().texOffs(42, 36).addBox(-0.5F, -length, -0.5F, 1.0F, length, 1.0F);
	}

	/**
	 * A great antler on the {@code side} of the skull (-1 right, 1 left): a heavy beam thrown out sideways, curving up in
	 * two more sections, with a brow tine forward over the eyes, tines along it and a forked crown.
	 */
	private static void antler(PartDefinition head, float side) {
		String name = side < 0 ? "right" : "left";
		PartDefinition beam = head.addOrReplaceChild(name + "_antler",
				CubeListBuilder.create().texOffs(89, 24).addBox(-1.0F, -6.0F, -1.0F, 2.0F, 6.0F, 2.0F),
				PartPose.offsetAndRotation(side * 2.0F, -4.0F, -3.5F, -0.4F, 0.0F, side * 1.25F));
		beam.addOrReplaceChild("brow", tine(4.0F), PartPose.offsetAndRotation(0.0F, -1.5F, -0.5F, 1.25F, 0.0F, -side * 0.5F));
		beam.addOrReplaceChild("bez", tine(5.0F), PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.35F, 0.0F, -side * 1.15F));
		PartDefinition middle = beam.addOrReplaceChild("middle", tine(7.0F),
				PartPose.offsetAndRotation(0.0F, -5.5F, 0.0F, -0.15F, 0.0F, -side * 0.5F));
		middle.addOrReplaceChild("trez", tine(5.0F), PartPose.offsetAndRotation(0.0F, -3.0F, 0.0F, 0.3F, 0.0F, -side * 0.75F));
		middle.addOrReplaceChild("inner", tine(4.0F), PartPose.offsetAndRotation(0.0F, -6.0F, 0.0F, -0.4F, 0.0F, -side * 0.85F));
		PartDefinition top = middle.addOrReplaceChild("top", tine(6.0F),
				PartPose.offsetAndRotation(0.0F, -7.0F, 0.0F, -0.2F, 0.0F, -side * 0.15F));
		top.addOrReplaceChild("fork_a", tine(3.0F), PartPose.offsetAndRotation(0.0F, -6.0F, 0.0F, 0.0F, 0.0F, side * 0.6F));
		top.addOrReplaceChild("fork_b", tine(4.0F), PartPose.offsetAndRotation(0.0F, -6.0F, 0.0F, 0.35F, 0.0F, -side * 0.4F));
		top.addOrReplaceChild("fork_c", tine(3.0F), PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, -0.65F, 0.0F, side * 0.1F));
	}

	/**
	 * A long arm on the {@code side} of the torso: a knotted shoulder, a thin upper arm with a root wound round it, a thick
	 * forearm plated with bark, and three long fingers and a thumb hooked into claws.
	 */
	private static void arm(PartDefinition body, String name, float side) {
		float outer = side < 0 ? -1.6F : 0.6F;
		CubeListBuilder upperArm = CubeListBuilder.create()
				.texOffs(19, 46).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F)
				.texOffs(48, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F)
				.texOffs(25, 24).addBox(outer, 1.0F, 0.2F, 1.0F, 10.0F, 1.0F);
		PartDefinition arm = body.addOrReplaceChild(name, upperArm,
				PartPose.offset(side * 6.0F, -16.5F, -0.5F));
		CubeListBuilder forearmCubes = CubeListBuilder.create()
				.texOffs(57, 0).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 11.0F, 3.0F)
				// A plate of bark standing off the outside of the forearm.
				.texOffs(36, 46).addBox(side < 0 ? -2.3F : 1.3F, 2.0F, -1.0F, 1.0F, 6.0F, 2.0F);
		PartDefinition forearm = arm.addOrReplaceChild("forearm", forearmCubes,
				PartPose.offset(0.0F, 11.5F, 0.0F));
		PartDefinition hand = forearm.addOrReplaceChild("hand",
				CubeListBuilder.create().texOffs(114, 46).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 3.0F, 3.0F), PartPose.offset(0.0F, 10.5F, 0.0F));
		for (int i = 0; i < FINGERS; i++) {
			// Three fingers side by side, a gap between each, and a thumb in front of them.
			boolean thumb = i == FINGERS - 1;
			PartDefinition finger = hand.addOrReplaceChild("finger" + i,
					CubeListBuilder.create().texOffs(39, 55).addBox(-0.5F, 0.0F, -0.5F, 1.0F, thumb ? 3.0F : 4.0F, 1.0F),
					PartPose.offset(thumb ? 0.0F : (i - 1) * 1.25F, thumb ? 1.5F : 2.5F, thumb ? -1.6F : 0.0F));
			finger.addOrReplaceChild("claw", CubeListBuilder.create().texOffs(44, 55).addBox(-0.5F, 0.0F, -0.5F, 1.0F, thumb ? 3.0F : 4.0F, 1.0F),
					PartPose.offset(0.0F, thumb ? 2.8F : 3.8F, 0.0F));
		}
	}

	/** A thin leg like a gnarled sapling, a knot for a knee, its foot splaying into three root toes and a spur. */
	private static void leg(PartDefinition root, String name, float side) {
		PartDefinition thigh = root.addOrReplaceChild(name, CubeListBuilder.create()
						.texOffs(26, 0).addBox(-1.5F, 0.0F, -1.5F, 3.0F, THIGH_LENGTH, 3.0F)
						.texOffs(25, 24).addBox(side < 0 ? -2.0F : 1.0F, 1.0F, -0.5F, 1.0F, 9.0F, 1.0F),
				PartPose.offset(side * 2.0F, HIP_Y, HIP_Z));
		PartDefinition shin = thigh.addOrReplaceChild("shin", CubeListBuilder.create()
						.texOffs(0, 55).addBox(-1.5F, -1.5F, -2.0F, 3.0F, 3.0F, 3.0F)
						.texOffs(39, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, SHIN_LENGTH, 2.0F)
						// A ridge of root down the front of the shin.
						.texOffs(25, 24).addBox(-0.5F, 2.0F, -1.6F, 1.0F, 8.0F, 1.0F),
				PartPose.offset(0.0F, THIGH_LENGTH, 0.0F));
		PartDefinition foot = shin.addOrReplaceChild("foot", CubeListBuilder.create()
						.texOffs(49, 55).addBox(-1.5F, -2.0F, -1.5F, 3.0F, 2.0F, 3.0F)
						// The spur behind the heel.
						.texOffs(13, 55).addBox(-0.5F, -1.0F, 1.0F, 1.0F, 1.0F, 2.0F),
				PartPose.offset(0.0F, SHIN_LENGTH, 0.0F));
		for (int i = 0; i < 3; i++) {
			foot.addOrReplaceChild("toe" + i, CubeListBuilder.create().texOffs(13, 55).addBox(-0.5F, -1.0F, -5.0F, 1.0F, 1.0F, 5.0F),
					PartPose.offsetAndRotation((i - 1) * 1.0F, 0.0F, -1.0F, 0.0F, (1 - i) * 0.4F, 0.0F));
		}
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
		float headPitch = state.xRot * Mth.DEG_TO_RAD + HEAD_DROOP;
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
		headMoss.xRot = -headPitch + Mth.sin(age * 0.08F) * 0.06F;
		// Arms hang from the shoulders in the world's frame, then swing.
		rightArm.xRot = rightArmX - lean;
		leftArm.xRot = leftArmX - lean;
		rightArm.zRot = rightArmZ;
		leftArm.zRot = leftArmZ;
		rightForearm.xRot = rightElbow;
		leftForearm.xRot = leftElbow;
		for (int i = 0; i < FINGERS; i++) {
			boolean thumb = i == FINGERS - 1;
			// The fingers fan out; the thumb hooks forward under them.
			float spread = thumb ? 0.0F : (i - 1) * 0.3F;
			float reach = thumb ? -0.5F : 0.0F;
			rightFingers[i].xRot = reach - curl - Mth.sin(age * 0.2F + i) * 0.05F;
			leftFingers[i].xRot = reach - curl - Mth.sin(age * 0.2F + i + 1.5F) * 0.05F;
			rightFingers[i].zRot = spread;
			leftFingers[i].zRot = spread;
			// The claws hook further than the fingers bend.
			rightClaws[i].xRot = -0.35F - curl * 0.8F;
			leftClaws[i].xRot = -0.35F - curl * 0.8F;
		}
		// The rags and the charm hang straight down, swaying.
		float sway = Mth.cos(walkPos * 0.5F) * 0.12F * walk;
		ragFront.xRot = -lean - 0.05F - Math.abs(sway) + Mth.sin(age * 0.07F) * 0.04F;
		ragBack.xRot = -lean + 0.1F + Math.abs(sway) + Mth.sin(age * 0.06F + 1.0F) * 0.04F;
		ragRight.zRot = 0.08F + sway * 0.5F;
		ragLeft.zRot = -0.08F + sway * 0.5F;
		ragRight.xRot = ragLeft.xRot = -lean;
		charm.xRot = -lean + Mth.sin(age * 0.09F) * 0.1F + sway;

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
		return THIGH_LENGTH * Mth.cos(thigh) + SHIN_LENGTH * Mth.cos(thigh + shin);
	}
}
