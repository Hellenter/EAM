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
 * A hunched, withered body on all fours, chimp-like: a ribcage leaning far forward with the spine poking through the
 * robe, long arms ending in clawed hands, bent legs on long bare feet. A torn monk's robe hangs off it: a hood with a
 * pointed tail over a skull with a loose jaw, a ragged mantle over the shoulders, a rope belt with a knotted end,
 * tattered sleeves with a rag hanging from each, two strips hanging from the chest and a skirt of torn strips over the
 * hips. The shoulders and neck follow the torso's lean, which changes as it leaps and swipes; the limbs hang from the
 * root so they swing in the world's frame, and every strip of cloth is turned back against its parent so it hangs
 * down and sways on its own.
 */
public class SwampLurkerModel extends EntityModel<SwampLurkerRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Eam.id("swamp_lurker"), "main");
	/** The hips, where the torso and legs meet. */
	private static final float HIP_Y = 13.0F;
	private static final float HIP_Z = 3.0F;
	/** How far forward the torso leans while it walks, from upright. */
	private static final float LEAN = 0.95F;
	private static final float TORSO = 11.0F;
	/** The base of the skull and the shoulders on the torso, from the hips. */
	private static final float NECK = 12.0F;
	private static final float SHOULDER_UP = 9.5F;
	private static final float SHOULDER_OUT = 4.5F;
	private static final float UPPER_ARM = 7.0F;
	private static final float FOREARM = 9.0F;
	private static final float THIGH = 6.0F;
	private static final float SHIN = 7.0F;
	/** Arms hang a little ahead of straight down; the knees are bent well forward, the shins nearly upright. */
	private static final float ARM_REST = -0.2F;
	private static final float FOREARM_REST = -0.1F;
	private static final float THIGH_REST = -1.0F;
	private static final float SHIN_REST = 1.2F;

	private final ModelPart body;
	private final ModelPart[] strips;
	private final ModelPart skirtRight;
	private final ModelPart skirtLeft;
	private final ModelPart head;
	private final ModelPart jaw;
	private final ModelPart hoodTail;
	private final Arm rightArm;
	private final Arm leftArm;
	private final Leg rightLeg;
	private final Leg leftLeg;

	public SwampLurkerModel(ModelPart root) {
		super(root);
		body = root.getChild("body");
		strips = new ModelPart[]{body.getChild("chest_right"), body.getChild("chest_left"), body.getChild("skirt_back"),
				body.getChild("skirt_front_right"), body.getChild("skirt_front_left"), body.getChild("rope")};
		skirtRight = body.getChild("skirt_right");
		skirtLeft = body.getChild("skirt_left");
		head = root.getChild("head");
		jaw = head.getChild("jaw");
		hoodTail = head.getChild("hood_tail");
		rightArm = new Arm(root.getChild("right_arm"));
		leftArm = new Arm(root.getChild("left_arm"));
		rightLeg = new Leg(root.getChild("right_leg"));
		leftLeg = new Leg(root.getChild("left_leg"));
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
						// The ribs, and the robe over them torn open down the spine and on the flanks.
						.texOffs(54, 0).addBox(-3.5F, -TORSO, -2.0F, 7.0F, TORSO, 4.0F)
						.texOffs(0, 0).addBox(-4.0F, -TORSO - 0.5F, -2.5F, 8.0F, 12.0F, 5.0F)
						// The hood's ragged mantle over the shoulders, the stringy neck, and the rope belt.
						.texOffs(76, 0).addBox(-5.0F, -TORSO - 1.5F, -3.5F, 10.0F, 4.0F, 7.0F)
						.texOffs(72, 28).addBox(-1.0F, -NECK - 1.5F, -1.5F, 2.0F, 3.0F, 2.0F)
						.texOffs(90, 17).addBox(-4.5F, -3.0F, -3.0F, 9.0F, 1.0F, 6.0F)
						// Vertebrae poking out through the tear down the back.
						.texOffs(116, 28).addBox(-0.5F, -9.0F, 2.0F, 1.0F, 1.0F, 1.0F)
						.texOffs(116, 28).addBox(-0.5F, -7.0F, 2.0F, 1.0F, 1.0F, 1.0F)
						.texOffs(116, 28).addBox(-0.5F, -5.0F, 2.0F, 1.0F, 1.0F, 1.0F)
						.texOffs(116, 28).addBox(-0.5F, -3.0F, 2.2F, 1.0F, 1.0F, 1.0F),
				PartPose.offsetAndRotation(0.0F, HIP_Y, HIP_Z, LEAN, 0.0F, 0.0F));
		// Two strips hanging from the chest, a skirt of torn strips round the hips and the belt's knotted end.
		body.addOrReplaceChild("chest_right", CubeListBuilder.create().texOffs(52, 17).addBox(-1.5F, 0.0F, -0.5F, 3.0F, 8.0F, 1.0F),
				PartPose.offset(-2.0F, -7.0F, -2.8F));
		body.addOrReplaceChild("chest_left", CubeListBuilder.create().texOffs(120, 17).addBox(-1.5F, 0.0F, -0.5F, 3.0F, 6.0F, 1.0F),
				PartPose.offset(2.0F, -6.0F, -2.8F));
		body.addOrReplaceChild("skirt_back", CubeListBuilder.create().texOffs(0, 28).addBox(-4.0F, 0.0F, -0.5F, 8.0F, 6.0F, 1.0F),
				PartPose.offset(0.0F, -1.0F, 2.6F));
		body.addOrReplaceChild("skirt_front_right", CubeListBuilder.create().texOffs(80, 17).addBox(-2.0F, 0.0F, -0.5F, 4.0F, 7.0F, 1.0F),
				PartPose.offset(-2.0F, -1.0F, -2.6F));
		body.addOrReplaceChild("skirt_front_left", CubeListBuilder.create().texOffs(18, 28).addBox(-2.0F, 0.0F, -0.5F, 4.0F, 6.0F, 1.0F),
				PartPose.offset(2.0F, -1.0F, -2.6F));
		body.addOrReplaceChild("skirt_right", CubeListBuilder.create().texOffs(110, 0).addBox(-0.5F, 0.0F, -2.5F, 1.0F, 6.0F, 5.0F),
				PartPose.offset(-4.1F, -1.0F, 0.0F));
		body.addOrReplaceChild("skirt_left", CubeListBuilder.create().texOffs(110, 0).mirror().addBox(-0.5F, 0.0F, -2.5F, 1.0F, 6.0F, 5.0F),
				PartPose.offset(4.1F, -1.0F, 0.0F));
		body.addOrReplaceChild("rope", CubeListBuilder.create()
						.texOffs(28, 28).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 6.0F, 1.0F)
						.texOffs(108, 28).addBox(-1.0F, 5.0F, -1.0F, 2.0F, 1.0F, 2.0F),
				PartPose.offset(3.0F, -2.0F, -3.2F));
		// A narrow skull deep in an open hood, its jaw hanging loose, the hood's point hanging behind.
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
						.texOffs(24, 17).addBox(-2.5F, -5.0F, -4.5F, 5.0F, 5.0F, 5.0F)
						.texOffs(26, 0).addBox(-3.5F, -6.5F, -5.5F, 7.0F, 9.0F, 7.0F),
				PartPose.ZERO);
		head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(32, 28).addBox(-2.0F, 0.0F, -4.0F, 4.0F, 2.0F, 4.0F),
				PartPose.offset(0.0F, 0.0F, -0.5F));
		head.addOrReplaceChild("hood_tail", CubeListBuilder.create().texOffs(48, 28).addBox(-1.5F, 0.0F, -0.5F, 3.0F, 5.0F, 1.0F),
				PartPose.offset(0.0F, -6.0F, 2.0F));
		addArm(root, "right_arm", false);
		addArm(root, "left_arm", true);
		addLeg(root, "right_leg", -2.0F, false);
		addLeg(root, "left_leg", 2.0F, true);
		return LayerDefinition.create(mesh, 128, 64);
	}

	/** A bony arm in a torn sleeve with a rag hanging off it, a bandaged forearm and a hand with three long claws. */
	private static void addArm(PartDefinition root, String name, boolean mirror) {
		PartDefinition arm = root.addOrReplaceChild(name, CubeListBuilder.create().mirror(mirror)
				.texOffs(44, 17).addBox(-1.0F, -1.0F, -1.0F, 2.0F, UPPER_ARM + 1.0F, 2.0F)
				.texOffs(0, 17).addBox(-2.0F, -1.5F, -2.0F, 4.0F, 7.0F, 4.0F), PartPose.ZERO);
		arm.addOrReplaceChild("rag", CubeListBuilder.create().mirror(mirror).texOffs(80, 28).addBox(-1.5F, 0.0F, 0.0F, 3.0F, 5.0F, 0.0F),
				PartPose.offset(mirror ? 0.5F : -0.5F, 4.5F, 2.0F));
		PartDefinition forearm = arm.addOrReplaceChild("forearm", CubeListBuilder.create().mirror(mirror)
				.texOffs(16, 17).addBox(-1.0F, 0.0F, -1.0F, 2.0F, FOREARM, 2.0F), PartPose.offset(0.0F, UPPER_ARM, 0.0F));
		PartDefinition hand = forearm.addOrReplaceChild("hand", CubeListBuilder.create().mirror(mirror)
				.texOffs(86, 28).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 1.0F, 3.0F), PartPose.offset(0.0F, FOREARM - 0.5F, 0.0F));
		hand.addOrReplaceChild("fingers", CubeListBuilder.create().mirror(mirror)
				.texOffs(100, 28).addBox(-2.0F, -0.5F, -3.0F, 1.0F, 1.0F, 3.0F)
				.texOffs(100, 28).addBox(-0.5F, -0.5F, -3.5F, 1.0F, 1.0F, 3.0F)
				.texOffs(100, 28).addBox(1.0F, -0.5F, -3.0F, 1.0F, 1.0F, 3.0F), PartPose.offset(0.0F, 0.5F, -2.0F));
	}

	/** A thin thigh, a bandaged shin and a long bare foot. */
	private static void addLeg(PartDefinition root, String name, float x, boolean mirror) {
		root.addOrReplaceChild(name, CubeListBuilder.create().mirror(mirror)
						.texOffs(60, 17).addBox(-1.5F, 0.0F, -1.5F, 3.0F, THIGH, 3.0F), PartPose.offset(x, HIP_Y, HIP_Z))
				.addOrReplaceChild("shin", CubeListBuilder.create().mirror(mirror)
						.texOffs(72, 17).addBox(-1.0F, 0.0F, -1.0F, 2.0F, SHIN, 2.0F), PartPose.offset(0.0F, THIGH, 0.0F))
				.addOrReplaceChild("foot", CubeListBuilder.create().mirror(mirror)
						.texOffs(56, 28).addBox(-1.5F, 0.0F, -3.5F, 3.0F, 1.0F, 5.0F), PartPose.offset(0.0F, SHIN, 0.0F));
	}

	@Override
	public void setupAnim(SwampLurkerRenderState state) {
		super.setupAnim(state);
		float t = state.actionTime;
		float age = state.ageInTicks;
		float walk = Math.min(state.walkAnimationSpeed, 1.0F);
		float phase = state.walkAnimationPos * 0.6662F;
		float swing = Mth.cos(phase);
		float step = Mth.sin(phase);

		// The torso's lean, breathing, and a crawl on diagonal pairs: the right hand moves with the left foot. The paw
		// coming forward lifts off the ground, its forearm folding back and its claws curling; so does the foot.
		Pose p = new Pose();
		p.lean = LEAN + Mth.sin(age * 0.08F) * 0.03F;
		p.sink = Math.abs(step) * walk;
		p.right.set(ARM_REST + swing * 0.8F * walk, 0.1F, FOREARM_REST, 1.0F, 0.0F, 0.0F);
		p.left.set(ARM_REST - swing * 0.8F * walk, -0.1F, FOREARM_REST, 1.0F, 0.0F, 0.0F);
		float rightLift = Math.max(step, 0.0F) * walk;
		float leftLift = Math.max(-step, 0.0F) * walk;
		p.right.forearm += rightLift * 1.1F;
		p.right.curl = rightLift * 0.8F;
		p.left.forearm += leftLift * 1.1F;
		p.left.curl = leftLift * 0.8F;
		p.rightThigh = THIGH_REST - swing * 0.6F * walk;
		p.leftThigh = THIGH_REST + swing * 0.6F * walk;
		p.rightShin = SHIN_REST + leftLift * 0.7F;
		p.leftShin = SHIN_REST + rightLift * 0.7F;
		p.jaw = 0.08F;
		p.stream = 0.0F;
		boolean idle = state.action == LurkerAction.NONE;

		switch (state.action) {
			case LEAP -> leap(p, t, state.verticalSpeed, age);
			case COMBO -> combo(p, t);
			case BURROW -> burrow(p, t);
			case NONE -> {
			}
		}
		root().yRot = p.twist;
		body.xRot = p.lean;
		body.y = HIP_Y + p.sink;
		body.zRot = step * 0.06F * walk;
		body.yRot = swing * 0.05F * walk;
		float cos = Mth.cos(p.lean);
		float sin = Mth.sin(p.lean);

		// The skull, carried round by the torso's lean, with a sudden twitch of the head now and then while it lurks
		// and its jaw chattering in some of those spells.
		head.y = HIP_Y + p.sink - NECK * cos + 0.5F;
		head.z = HIP_Z - NECK * sin - 1.0F - p.lunge;
		head.xRot = state.xRot * Mth.DEG_TO_RAD - 0.15F - p.sink * 0.05F + p.lunge * 0.15F;
		head.yRot = state.yRot * Mth.DEG_TO_RAD;
		if (idle) {
			float seg = age / 46.0F;
			int i = Mth.floor(seg);
			float snap = Mth.clamp((seg - i) * 8.0F, 0.0F, 1.0F);
			head.yRot += Mth.lerp(snap, noise(i - 1), noise(i)) * 0.35F;
			head.zRot = Mth.lerp(snap, noise(i + 99), noise(i + 100)) * 0.25F;
			if (noise(i + 50) > 0.3F) {
				p.jaw += Math.max(Mth.sin(age * 2.2F), 0.0F) * 0.15F;
			}
		}
		jaw.xRot = p.jaw;
		hoodTail.xRot = 0.5F - head.xRot + p.stream * 0.8F + Mth.sin(phase) * 0.15F * walk;

		float shoulderY = HIP_Y + p.sink - SHOULDER_UP * cos;
		float shoulderZ = HIP_Z - SHOULDER_UP * sin;
		rightArm.place(-SHOULDER_OUT, shoulderY, shoulderZ, p.right, phase, walk, age, p.stream);
		leftArm.place(SHOULDER_OUT, shoulderY, shoulderZ, p.left, phase + Mth.PI, walk, age, p.stream);
		rightLeg.place(HIP_Y + p.sink, p.rightThigh, p.rightShin, 0.12F, p.feet);
		leftLeg.place(HIP_Y + p.sink, p.leftThigh, p.leftShin, -0.12F, p.feet);

		// The strips are turned back by the lean so they hang straight down, each swaying out of step with the others
		// and streaming back through a leap.
		for (int i = 0; i < strips.length; i++) {
			strips[i].xRot = -p.lean + 0.1F + Mth.sin(phase + i * 1.3F) * 0.25F * walk + Mth.sin(age * 0.1F + i * 2.1F) * 0.05F
					+ p.stream * (1.0F + i * 0.1F);
		}
		skirtRight.xRot = -p.lean + 0.1F + Mth.sin(phase + 0.7F) * 0.2F * walk + p.stream;
		skirtLeft.xRot = -p.lean + 0.1F + Mth.sin(phase + 2.2F) * 0.2F * walk + p.stream;
		skirtRight.zRot = 0.1F + Math.max(-swing, 0.0F) * 0.25F * walk;
		skirtLeft.zRot = -0.1F - Math.max(swing, 0.0F) * 0.25F * walk;
	}

	/** Stretched out flat with the arms flung ahead and claws spread, legs trailing; it gathers to land as it falls. */
	private static void leap(Pose p, float t, float verticalSpeed, float age) {
		float stretch = Mth.clamp(t / 3.0F, 0.0F, 1.0F);
		float falling = Mth.clamp(-verticalSpeed * 2.0F, 0.0F, 1.0F);
		p.lean = Mth.lerp(stretch, LEAN, 1.4F);
		float reach = Mth.lerp(stretch, ARM_REST, Mth.lerp(falling, -1.9F, -0.9F));
		float forearm = Mth.lerp(stretch, FOREARM_REST, Mth.lerp(falling, -0.2F, 0.0F));
		p.right.set(reach, 0.1F + 0.35F * stretch, forearm, 1.0F - stretch, -0.4F * stretch, -0.5F * stretch);
		p.left.set(reach, -0.1F - 0.35F * stretch, forearm, 1.0F - stretch, -0.4F * stretch, -0.5F * stretch);
		p.rightThigh = p.leftThigh = Mth.lerp(stretch, THIGH_REST, Mth.lerp(falling, 0.9F, -1.3F));
		p.rightShin = p.leftShin = Mth.lerp(stretch, SHIN_REST, Mth.lerp(falling, 0.25F, 1.5F));
		p.feet = 0.6F * stretch * (1.0F - falling);
		p.jaw = Mth.lerp(stretch, 0.1F, 0.75F) * (1.0F - falling * 0.5F);
		p.stream = stretch * (1.0F - falling * 0.5F) + Mth.sin(age * 1.5F) * 0.15F * stretch;
		p.sink = 0.0F;
	}

	/** It rears up and rakes with one arm, the other, then the first again, the torso twisting into each swipe. */
	private static void combo(Pose p, float t) {
		float rear = Mth.clamp(t / 2.0F, 0.0F, 1.0F) * Mth.clamp((LurkerAction.COMBO.length() - t) / 3.0F, 0.0F, 1.0F);
		int[] hits = LurkerAction.COMBO_HITS;
		float r = rake(t, hits[0]) + rake(t, hits[2]);
		float l = rake(t, hits[1]);
		p.lean = Mth.lerp(rear, LEAN, 0.4F);
		swipe(p.right, r, rear, 1.0F);
		swipe(p.left, l, rear, -1.0F);
		p.twist = (r - l) * 0.22F * rear;
		float open = 0.0F;
		for (int hit : hits) {
			open = Math.max(open, Mth.clamp(1.0F - Math.abs(t - hit) / 2.5F, 0.0F, 1.0F));
		}
		p.jaw = 0.12F + open * 0.6F;
		p.lunge = open * rear;
		p.rightThigh = p.leftThigh = Mth.lerp(rear, THIGH_REST, -1.25F);
		p.rightShin = p.leftShin = Mth.lerp(rear, SHIN_REST, 1.55F);
		p.sink = 1.5F * rear;
	}

	/**
	 * One arm through a rake: held up bent at the elbow on guard, raised high with the forearm cocked back behind the
	 * head and the claws spread, then whipped down and across the body, the claws curling shut through the strike.
	 */
	private static void swipe(ArmPose arm, float rake, float rear, float side) {
		float raised = Math.max(rake, 0.0F);
		float strike = Math.max(-rake, 0.0F) / 0.4F;
		float x = -0.7F - raised * 2.2F + strike * 0.45F;
		float forearm = raised > 0.0F ? -0.7F - raised * 0.4F : Mth.lerp(strike, -0.7F, -0.1F);
		arm.set(Mth.lerp(rear, ARM_REST, x), side * (0.1F + rake * 0.6F), Mth.lerp(rear, FOREARM_REST, forearm), 1.0F - rear,
				-raised * 0.5F + strike * 0.4F, raised > 0.0F ? -raised * 0.5F : strike * 0.9F);
	}

	/** Digs with both arms in turn, nose down, scooping with curled claws as it sinks into the ground. */
	private static void burrow(Pose p, float t) {
		p.lean = 1.45F;
		float dig = Mth.sin(t * 1.6F);
		p.right.set(-1.3F + dig * 0.7F, 0.15F, FOREARM_REST + Math.max(dig, 0.0F) * 0.9F, 0.0F, 0.3F, 0.8F);
		p.left.set(-1.3F - dig * 0.7F, -0.15F, FOREARM_REST + Math.max(-dig, 0.0F) * 0.9F, 0.0F, 0.3F, 0.8F);
		p.jaw = 0.3F;
		p.sink = t / LurkerAction.BURROW.length() * 22.0F;
	}

	/** How far through the rake on {@code hit} the arm is: 0 at rest, rising to 1 raised, then the strike down to -0.4. */
	private static float rake(float t, int hit) {
		float start = hit - 3.0F;
		if (t < start || t > hit + 2.0F) {
			return 0.0F;
		}
		if (t < hit - 0.5F) {
			return (t - start) / 2.5F;
		}
		float s = (t - (hit - 0.5F)) / 2.5F;
		return s < 0.4F ? Mth.lerp(s / 0.4F, 1.0F, -0.4F) : Mth.lerp((s - 0.4F) / 0.6F, -0.4F, 0.0F);
	}

	/** A steady pseudo-random number in [-1, 1] for each step {@code i}. */
	private static float noise(int i) {
		double n = Math.sin(i * 78.233) * 43758.5453;
		return (float) (n - Math.floor(n)) * 2.0F - 1.0F;
	}

	/** The pose being built for this frame, before it is set on the parts. */
	private static final class Pose {
		final ArmPose right = new ArmPose();
		final ArmPose left = new ArmPose();
		float lean;
		float sink;
		float twist;
		float lunge;
		float jaw;
		float stream;
		float feet;
		float rightThigh;
		float leftThigh;
		float rightShin;
		float leftShin;
	}

	private static final class ArmPose {
		float x;
		float z;
		float forearm;
		/** 1 keeps the hand flat on the ground whatever the arm does, 0 lets it follow the forearm. */
		float level;
		float hand;
		float curl;

		void set(float x, float z, float forearm, float level, float hand, float curl) {
			this.x = x;
			this.z = z;
			this.forearm = forearm;
			this.level = level;
			this.hand = hand;
			this.curl = curl;
		}
	}

	private static final class Arm {
		private final ModelPart arm;
		private final ModelPart rag;
		private final ModelPart forearm;
		private final ModelPart hand;
		private final ModelPart fingers;

		Arm(ModelPart arm) {
			this.arm = arm;
			rag = arm.getChild("rag");
			forearm = arm.getChild("forearm");
			hand = forearm.getChild("hand");
			fingers = hand.getChild("fingers");
		}

		void place(float x, float y, float z, ArmPose pose, float phase, float walk, float age, float stream) {
			arm.x = x;
			arm.y = y;
			arm.z = z;
			arm.xRot = pose.x;
			arm.zRot = pose.z;
			forearm.xRot = pose.forearm;
			hand.xRot = -(pose.x + pose.forearm) * pose.level + pose.hand;
			fingers.xRot = pose.curl;
			rag.xRot = -pose.x + 0.2F + Mth.sin(phase + 0.8F) * 0.3F * walk + Mth.sin(age * 0.12F) * 0.05F + stream;
		}
	}

	private static final class Leg {
		private final ModelPart thigh;
		private final ModelPart shin;
		private final ModelPart foot;

		Leg(ModelPart thigh) {
			this.thigh = thigh;
			shin = thigh.getChild("shin");
			foot = shin.getChild("foot");
		}

		/** Places the leg; the foot stays flat on the ground unless {@code point} turns its toes back. */
		void place(float y, float thighX, float shinX, float splay, float point) {
			thigh.y = y;
			thigh.xRot = thighX;
			thigh.zRot = splay;
			shin.xRot = shinX;
			foot.xRot = -(thighX + shinX) + point;
		}
	}
}
