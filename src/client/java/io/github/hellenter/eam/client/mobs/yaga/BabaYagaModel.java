package io.github.hellenter.eam.client.mobs.yaga;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.entity.yaga.YagaAction;
import io.github.hellenter.eam.mobs.entity.yaga.YagaDeath;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Baba Yaga: a hunched, corpse-grey crone. Long matted hair hangs from a balding skull down her back and past her
 * cheeks; deep black sockets hold two pinpoint green eyes over a long warty hooked nose; her lower jaw hangs loose and
 * drops open on a mouth of crooked teeth. Moth-eaten black rags with knobs of her spine showing through, a necklace of
 * finger bones, a torn skirt stiff with mud and old blood, one leg wrapped in filthy linen and a bast shoe, the other
 * bare bone ("костяная нога"). Long black claws on thin veined hands; her right hand holds a gnarled staff topped with
 * a skull. In phase one she stands in a flared, iron-hooped wooden mortar; the mortar is hidden once it has burst.
 * <p>
 * The whole rig turns about the ground point, so she leans into a dive mortar and all. Her staff is kept upright in the
 * world's frame whatever her arm does, then tilted for each action; her hair hangs down much the same way.
 */
public class BabaYagaModel extends EntityModel<BabaYagaRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Eam.id("baba_yaga"), "main");
	private static final float HUNCH = 0.4F;
	/** How far she stands up in her mortar, onto its floor. */
	private static final float MORTAR_LIFT = 3.0F;
	private static final float PI = Mth.PI;

	private final ModelPart rig;
	private final ModelPart mortar;
	private final ModelPart crone;
	private final ModelPart torso;
	private final ModelPart head;
	private final ModelPart jaw;
	private final ModelPart hair;
	private final ModelPart rightLock;
	private final ModelPart leftLock;
	private final ModelPart beads;
	private final ModelPart rightArm;
	private final ModelPart leftArm;
	private final ModelPart rightForearm;
	private final ModelPart leftForearm;
	private final ModelPart rightHand;
	private final ModelPart leftHand;
	private final ModelPart staff;
	private final ModelPart rightLeg;
	private final ModelPart leftLeg;
	private final ModelPart skirt;

	public BabaYagaModel(ModelPart root) {
		super(root);
		rig = root.getChild("rig");
		mortar = rig.getChild("mortar");
		crone = rig.getChild("crone");
		torso = crone.getChild("torso");
		head = torso.getChild("head");
		jaw = head.getChild("jaw");
		hair = head.getChild("hair");
		rightLock = head.getChild("right_lock");
		leftLock = head.getChild("left_lock");
		beads = torso.getChild("beads");
		rightArm = torso.getChild("right_arm");
		leftArm = torso.getChild("left_arm");
		rightForearm = rightArm.getChild("right_forearm");
		leftForearm = leftArm.getChild("left_forearm");
		rightHand = rightForearm.getChild("right_hand");
		leftHand = leftForearm.getChild("left_hand");
		staff = rightHand.getChild("staff");
		rightLeg = crone.getChild("right_leg");
		leftLeg = crone.getChild("left_leg");
		skirt = crone.getChild("skirt");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition rig = root.addOrReplaceChild("rig", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		// The mortar: a narrow foot, then staves flaring out to the rim, fourteen pixels up.
		rig.addOrReplaceChild("mortar", CubeListBuilder.create()
				.texOffs(80, 0).addBox(-5.0F, -3.0F, -5.0F, 10.0F, 3.0F, 10.0F)
				.texOffs(0, 29).addBox(-7.0F, -14.0F, -7.0F, 14.0F, 11.0F, 2.0F)
				.texOffs(0, 29).addBox(-7.0F, -14.0F, 5.0F, 14.0F, 11.0F, 2.0F)
				.texOffs(4, 0).addBox(-7.0F, -14.0F, -5.0F, 2.0F, 11.0F, 10.0F)
				.texOffs(4, 0).mirror().addBox(5.0F, -14.0F, -5.0F, 2.0F, 11.0F, 10.0F), PartPose.ZERO);

		// The crone, in model coordinates: the ground at y 24.
		PartDefinition crone = rig.addOrReplaceChild("crone", CubeListBuilder.create(), PartPose.offset(0.0F, -24.0F, 0.0F));
		PartDefinition skirt = crone.addOrReplaceChild("skirt", CubeListBuilder.create()
				.texOffs(28, 0).addBox(-4.0F, 0.0F, -3.0F, 8.0F, 10.0F, 6.0F)
				.texOffs(68, 42).addBox(-4.0F, -1.0F, -3.0F, 8.0F, 1.0F, 6.0F, new CubeDeformation(0.2F)),
				PartPose.offset(0.0F, 11.0F, 0.0F));
		skirt.addOrReplaceChild("apron", CubeListBuilder.create().texOffs(16, 52)
				.addBox(-2.5F, 0.0F, 0.0F, 5.0F, 7.0F, 0.0F), PartPose.offset(0.0F, 0.5F, -3.15F));
		// The right leg is bare bone down to a bony foot; the left is wrapped in linen and shod in a bast shoe.
		PartDefinition boneLeg = crone.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(52, 42)
				.addBox(-1.0F, 0.0F, -1.0F, 2.0F, 7.0F, 2.0F), PartPose.offset(-1.6F, 16.0F, 0.0F));
		boneLeg.addOrReplaceChild("foot", CubeListBuilder.create().texOffs(56, 52)
				.addBox(-1.0F, 0.0F, -3.0F, 2.0F, 1.0F, 4.0F), PartPose.offset(0.0F, 7.0F, 0.0F));
		PartDefinition leg = crone.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(28, 42).mirror()
				.addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F), PartPose.offset(1.6F, 16.0F, 0.0F));
		leg.addOrReplaceChild("lapot", CubeListBuilder.create().texOffs(0, 52).mirror()
				.addBox(-1.5F, 0.0F, -3.5F, 3.0F, 2.0F, 5.0F), PartPose.offset(0.0F, 6.0F, 0.0F));

		// Her back, with the knobs of her spine standing out above the shawl and through it.
		PartDefinition torso = crone.addOrReplaceChild("torso", CubeListBuilder.create()
				.texOffs(38, 29).addBox(-3.5F, -8.0F, -2.0F, 7.0F, 8.0F, 4.0F)
				.texOffs(26, 52).addBox(-2.5F, -7.5F, 1.5F, 5.0F, 4.0F, 2.0F)
				.texOffs(72, 29).addBox(-4.5F, -8.5F, -3.0F, 9.0F, 5.0F, 6.0F)
				.texOffs(112, 52).addBox(-0.5F, -9.3F, 1.8F, 1.0F, 1.0F, 1.0F)
				.texOffs(112, 52).addBox(-0.5F, -7.8F, 3.2F, 1.0F, 1.0F, 1.0F)
				.texOffs(112, 52).addBox(-0.5F, -5.9F, 3.3F, 1.0F, 1.0F, 1.0F)
				.texOffs(112, 52).addBox(-0.5F, -4.0F, 3.2F, 1.0F, 1.0F, 1.0F),
				PartPose.offsetAndRotation(0.0F, 11.0F, 0.0F, HUNCH, 0.0F, 0.0F));
		// A string of finger bones and teeth round her neck.
		torso.addOrReplaceChild("beads", CubeListBuilder.create().texOffs(80, 52)
				.addBox(-3.0F, 0.0F, 0.0F, 6.0F, 4.0F, 0.0F), PartPose.offset(0.0F, -7.0F, -3.2F));

		PartDefinition head = torso.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(56, 0).addBox(-3.0F, -8.0F, -4.0F, 6.0F, 8.0F, 6.0F)
				// A long hooked nose, warty, its tip hanging down over her lip.
				.texOffs(92, 52).addBox(-0.5F, -4.5F, -7.0F, 1.0F, 1.0F, 3.0F)
				.texOffs(108, 52).addBox(-0.5F, -3.5F, -7.0F, 1.0F, 2.0F, 1.0F)
				// Thin wild hair over the skull, bald in patches.
				.texOffs(0, 42).addBox(-3.5F, -8.5F, -4.5F, 7.0F, 3.0F, 7.0F),
				PartPose.offset(0.0F, -8.0F, -0.5F));
		// The lower jaw, hinged at the back: it hangs loose and drops open.
		head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(96, 42)
				.addBox(-2.5F, 0.0F, -5.2F, 5.0F, 2.0F, 5.0F), PartPose.offset(0.0F, 0.0F, 1.0F));
		// Long hair down her back and a matted lock past each cheek.
		head.addOrReplaceChild("hair", CubeListBuilder.create().texOffs(60, 29)
				.addBox(-2.5F, 0.0F, 0.0F, 5.0F, 11.0F, 1.0F), PartPose.offset(0.0F, -7.0F, 1.6F));
		head.addOrReplaceChild("right_lock", CubeListBuilder.create().texOffs(32, 29)
				.addBox(-0.5F, 0.0F, -1.0F, 1.0F, 11.0F, 2.0F), PartPose.offset(-3.6F, -6.5F, 0.0F));
		head.addOrReplaceChild("left_lock", CubeListBuilder.create().texOffs(32, 29).mirror()
				.addBox(-0.5F, 0.0F, -1.0F, 1.0F, 11.0F, 2.0F), PartPose.offset(3.6F, -6.5F, 0.0F));

		PartDefinition rightArm = addArm(torso, "right", -4.5F, false);
		PartDefinition rightHand = rightArm.getChild("right_forearm").getChild("right_hand");
		addArm(torso, "left", 4.5F, true);

		// The staff, centred in her fist: a gnarled shaft, a burl near the top, a skull on it and two twigs.
		PartDefinition staff = rightHand.addOrReplaceChild("staff", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-0.5F, -20.0F, -0.5F, 1.0F, 28.0F, 1.0F)
				.texOffs(68, 52).addBox(-1.0F, -21.0F, -1.0F, 2.0F, 3.0F, 2.0F)
				.texOffs(40, 52).addBox(-1.5F, -24.0F, -1.5F, 3.0F, 3.0F, 3.0F), PartPose.offset(0.0F, 1.0F, 0.0F));
		staff.addOrReplaceChild("right_twig", CubeListBuilder.create().texOffs(52, 52)
				.addBox(-0.5F, -5.0F, -0.5F, 1.0F, 5.0F, 1.0F), PartPose.offsetAndRotation(-0.5F, -20.0F, 0.0F, 0.0F, 0.0F, -0.55F));
		staff.addOrReplaceChild("left_twig", CubeListBuilder.create().texOffs(52, 52).mirror()
				.addBox(-0.5F, -5.0F, -0.5F, 1.0F, 5.0F, 1.0F), PartPose.offsetAndRotation(0.5F, -19.0F, 0.0F, 0.2F, 0.0F, 0.6F));
		return LayerDefinition.create(mesh, 128, 64);
	}

	private static PartDefinition addArm(PartDefinition torso, String side, float x, boolean mirror) {
		PartDefinition arm = torso.addOrReplaceChild(side + "_arm", CubeListBuilder.create().texOffs(40, 42).mirror(mirror)
				.addBox(-1.5F, -1.0F, -1.5F, 3.0F, 6.0F, 3.0F), PartPose.offset(x, -7.0F, 0.0F));
		PartDefinition forearm = arm.addOrReplaceChild(side + "_forearm", CubeListBuilder.create().texOffs(60, 42).mirror(mirror)
				.addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F), PartPose.offset(0.0F, 5.0F, 0.0F));
		// A bony hand, three long black claws and a hooked thumb claw.
		forearm.addOrReplaceChild(side + "_hand", CubeListBuilder.create()
				.texOffs(100, 52).mirror(mirror).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F)
				.texOffs(76, 52).addBox(-1.0F, 1.5F, -1.1F, 1.0F, 4.0F, 1.0F)
				.texOffs(76, 52).addBox(0.0F, 1.5F, -1.1F, 1.0F, 4.0F, 1.0F)
				.texOffs(76, 52).addBox(-0.5F, 1.5F, 0.1F, 1.0F, 4.0F, 1.0F)
				.texOffs(76, 52).addBox(mirror ? -1.6F : 0.6F, 0.5F, -0.5F, 1.0F, 4.0F, 1.0F), PartPose.offset(0.0F, 6.0F, 0.0F));
		return arm;
	}

	@Override
	public void setupAnim(BabaYagaRenderState state) {
		super.setupAnim(state);
		float age = state.ageInTicks;
		float t = state.actionTime;
		mortar.visible = state.inMortar;

		// Head: looks about, droops a little, nods as she mutters.
		head.yRot = state.yRot * Mth.DEG_TO_RAD;
		head.xRot = state.xRot * Mth.DEG_TO_RAD - HUNCH * 0.6F + Mth.sin(age * 0.09F) * 0.04F;

		// At rest: the left arm hangs a little forward with claws ready, the right holds the staff out at her side.
		float rightX = -HUNCH - 0.35F;
		float rightFore = -0.55F;
		float leftX = -HUNCH - 0.15F + Mth.sin(age * 0.067F) * 0.05F;
		float leftFore = -0.35F;
		float rightZ = 0.1F;
		float leftZ = -0.1F;
		float staffTilt = 0.0F;
		float lean = 0.0F;

		if (state.inMortar) {
			// Bobbing on the air.
			float bob = Mth.sin(age * 0.1F) * 1.0F;
			rig.y += bob;
			crone.y -= MORTAR_LIFT;
			rig.zRot = Mth.sin(age * 0.05F) * 0.04F;
		} else {
			// Hobbling about on foot; she runs bent low.
			float walk = state.walkAnimationPos * 0.6662F;
			float stride = Math.min(1.0F, state.walkAnimationSpeed);
			rightLeg.xRot = Mth.cos(walk) * 1.1F * stride;
			leftLeg.xRot = Mth.cos(walk + PI) * 1.1F * stride;
			torso.xRot += stride * 0.3F;
			leftX += Mth.cos(walk) * 0.6F * stride;
			skirt.xRot = -Math.abs(Mth.cos(walk)) * 0.08F * stride;
			rig.y -= Math.abs(Mth.sin(walk)) * 0.6F * stride;
		}

		YagaAction action = state.action;
		switch (action) {
			case SUMMON -> {
				// Staff thrust up over her head, shaking as her creatures rise.
				float up = smooth(t / 8.0F);
				rightX = Mth.lerp(up, rightX, -2.75F);
				rightFore = Mth.lerp(up, rightFore, -0.1F);
				rightZ = Mth.lerp(up, rightZ, 0.2F);
				leftX = Mth.lerp(up, leftX, -1.2F);
				leftZ = Mth.lerp(up, leftZ, -0.6F);
				head.xRot -= up * 0.4F;
				if (t >= YagaAction.SUMMON_RAISE - 4 && t < YagaAction.SUMMON_RAISE + 4) {
					rightX += Mth.sin(t * 3.1F) * 0.12F;
				}
			}
			case THROW -> {
				// An overhand throw with the left hand.
				float release = YagaAction.THROW_RELEASE;
				if (t < release) {
					float back = smooth(t / release);
					leftX = Mth.lerp(back, leftX, -2.9F);
					leftFore = Mth.lerp(back, leftFore, -0.9F);
					torso.xRot -= back * 0.15F;
				} else {
					float follow = smooth((t - release) / 4.0F);
					float settle = smooth((t - release - 4.0F) / 4.0F);
					leftX = Mth.lerp(settle, Mth.lerp(follow, -2.9F, -0.9F), leftX);
					leftFore = Mth.lerp(follow, -0.9F, -0.2F);
					torso.xRot += follow * (1.0F - settle) * 0.25F;
				}
			}
			case SPELL, ACID, THREADS -> {
				// The staff swung forward to point at its target, a jolt as the magic leaves it.
				int release = action == YagaAction.SPELL ? YagaAction.SPELL_RELEASE : YagaAction.BOLT_RELEASE;
				float aim = smooth(t / (release - 2.0F));
				float recoil = t >= release ? Math.max(0.0F, 1.0F - (t - release) / 5.0F) : 0.0F;
				rightX = Mth.lerp(aim, rightX, -1.45F) + recoil * 0.3F;
				rightFore = Mth.lerp(aim, rightFore, -0.2F);
				staffTilt = -aim * 1.25F + recoil * 0.35F;
				leftX = Mth.lerp(aim, leftX, -0.7F);
				leftZ = Mth.lerp(aim, leftZ, -0.5F);
			}
			case GLYPH -> {
				// Both arms spread and raised, the staff drawn round in a circle.
				float up = smooth(t / 6.0F);
				rightX = Mth.lerp(up, rightX, -1.9F);
				leftX = Mth.lerp(up, leftX, -1.9F);
				rightZ = Mth.lerp(up, rightZ, 0.7F);
				leftZ = Mth.lerp(up, leftZ, -0.7F);
				staffTilt = Mth.sin(t * 0.6F) * 0.5F * up;
				head.xRot -= up * 0.3F;
			}
			case SWOOP -> {
				// Leaning into the dive, the staff raised back like a club, brought down low on the strike: she bends over
				// her mortar's rim so it reaches below her feet, at a player on the ground.
				float strike = state.strikeTick;
				float raise = smooth(t / 8.0F);
				float swing = smooth((t - strike + 3.0F) / 3.0F);
				lean = 0.35F + swing * 0.2F;
				rightX = Mth.lerp(swing, Mth.lerp(raise, rightX, -2.8F), -0.6F);
				rightFore = Mth.lerp(raise, rightFore, -0.5F);
				staffTilt = Mth.lerp(swing, raise * 0.9F, -2.1F);
				leftX = -HUNCH - 0.9F;
				leftZ = -0.4F;
			}
			case RETREAT -> lean = -0.12F;
			case STAGGER -> {
				// Knocked back: arms flung out, the staff swinging loose.
				float hit = 1.0F - smooth(t / 20.0F);
				torso.xRot -= hit * 0.5F;
				lean = -0.35F * hit;
				rightZ += hit * 1.0F;
				leftZ -= hit * 1.0F;
				rightX += hit * 0.4F;
				leftX += hit * 0.4F;
				staffTilt = hit * 0.6F * Mth.sin(t * 0.8F);
				head.xRot -= hit * 0.4F;
			}
			case SHATTER -> {
				// Falling from her burst mortar, flailing.
				float flail = Mth.sin(t * 0.9F);
				rightX = -1.4F + flail * 0.8F;
				leftX = -1.4F - flail * 0.8F;
				rightZ = 0.8F;
				leftZ = -0.8F;
				rightLeg.xRot = flail * 0.5F;
				leftLeg.xRot = -flail * 0.5F;
				staffTilt = flail * 0.7F;
			}
			case BEAM -> {
				// The staff held out at the player, the other hand steadying it; trembling while it burns.
				float aim = smooth(t / 6.0F);
				float shake = t > state.beamWindup ? Mth.sin(t * 2.7F) * 0.04F : 0.0F;
				rightX = Mth.lerp(aim, rightX, -1.5F) + shake;
				rightFore = Mth.lerp(aim, rightFore, -0.15F);
				staffTilt = -aim * 1.35F + shake;
				leftX = Mth.lerp(aim, leftX, -1.35F);
				leftZ = Mth.lerp(aim, leftZ, -0.55F);
				leftFore = Mth.lerp(aim, leftFore, -0.3F);
			}
			case CLAWS -> {
				int lunge = state.lunge;
				if (t < lunge) {
					// Crouched to spring, claws drawn back.
					float crouch = smooth(t / 5.0F);
					torso.xRot += crouch * 0.35F;
					rightX = Mth.lerp(crouch, rightX, -HUNCH + 0.7F);
					leftX = Mth.lerp(crouch, leftX, -HUNCH + 0.7F);
					rightZ = Mth.lerp(crouch, rightZ, 0.45F);
					leftZ = Mth.lerp(crouch, leftZ, -0.45F);
					staffTilt = crouch * 0.9F;
				} else {
					// Rakes, one hand then the other, one per gap.
					float since = t - lunge;
					int gap = Math.max(1, state.clawGap);
					int rake = (int) (since / gap);
					float through = rake < state.clawHits ? smooth((since - rake * gap) / gap) : 1.0F;
					float up = -2.4F;
					float down = -0.2F;
					boolean right = rake % 2 == 0;
					float active = Mth.lerp(through, up, down);
					float idle = Mth.lerp(through, down, up);
					rightX = right ? active : idle;
					leftX = right ? idle : active;
					rightZ = 0.3F;
					leftZ = -0.3F;
					torso.xRot += 0.25F;
					staffTilt = 0.9F;
				}
				leftFore = -0.2F;
				rightFore = -0.2F;
			}
			case EAT_RUN, ESCAPE -> {
				// Scuttling bent double, claws out after her meal or arms back as she flees.
				torso.xRot += 0.35F;
				if (action == YagaAction.EAT_RUN) {
					rightX = -1.5F;
					leftX = -1.5F;
					staffTilt = -0.9F;
				} else {
					rightX = 0.4F;
					leftX = 0.6F;
					staffTilt = 1.0F;
				}
			}
			case EAT -> {
				// Bent over her meal, tearing at it.
				torso.xRot += 0.55F;
				head.xRot += 0.3F + Mth.sin(t * 1.3F) * 0.25F;
				leftX = -1.3F + Mth.sin(t * 1.1F) * 0.3F;
				rightX = -1.0F;
				leftFore = -0.8F;
				staffTilt = 0.6F;
			}
			default -> {
			}
		}

		if (state.dying >= 0.0F) {
			// Dying: burning, she writhes in fits, arms thrashing and head thrown about, sinks to her knees and crumbles
			// into a heap of ash.
			float d = state.dying;
			float fit = YagaDeath.convulsion(d);
			float k = d * 0.55F;
			int step = Mth.floor(k);
			float snap = smooth((k - step) * 2.5F);
			rightX = -1.3F + jerk(step, snap, 0) * 1.4F * fit;
			leftX = -1.3F + jerk(step, snap, 1) * 1.4F * fit;
			rightZ = 0.5F + jerk(step, snap, 2) * 0.7F * fit;
			leftZ = -0.5F + jerk(step, snap, 3) * 0.7F * fit;
			rightFore = -0.6F + jerk(step, snap, 4) * 0.5F * fit;
			leftFore = -0.6F + jerk(step, snap, 5) * 0.5F * fit;
			torso.xRot = -0.2F + jerk(step, snap, 6) * 0.4F * fit;
			head.xRot = -0.6F + jerk(step, snap, 7) * 0.6F * fit;
			head.yRot = jerk(step, snap, 8) * 0.8F * fit;
			rightLeg.xRot = jerk(step, snap, 9) * 0.5F * fit;
			leftLeg.xRot = jerk(step, snap, 10) * 0.5F * fit;
			staffTilt = jerk(step, snap, 11) * 1.2F * fit;
			lean = Mth.sin(d * 2.9F) * 0.06F * fit;
			rig.zRot = Mth.sin(d * 3.7F) * 0.08F * fit;
			rig.y += smooth((d - 20.0F) / 30.0F) * 3.0F;
			float crumble = YagaDeath.crumble(d);
			rig.yScale = 1.0F - 0.85F * crumble;
			rig.xScale = 1.0F + 0.5F * crumble;
			rig.zScale = 1.0F + 0.5F * crumble;
		}

		// Her jaw: muttering, cackling, shrieking and chewing; her head now and then twitches to one side.
		jaw.xRot = mouth(state, action, t, age);
		if (state.dying < 0.0F) {
			head.zRot = twitch(age);
		}
		// Her hair and her bone necklace hang down whatever her back does, swinging a little as she moves.
		float speed = state.inMortar ? 0.0F : Math.min(1.0F, state.walkAnimationSpeed);
		float sway = Mth.sin(age * 0.07F) * 0.05F + speed * 0.35F;
		float hang = -(torso.xRot + head.xRot);
		hair.xRot = hang + 0.05F + sway;
		hair.zRot = -head.zRot * 0.8F;
		rightLock.xRot = hang * 0.9F + sway * 0.5F;
		leftLock.xRot = hang * 0.9F + sway * 0.5F;
		rightLock.zRot = 0.06F - head.zRot * 0.8F;
		leftLock.zRot = -0.06F - head.zRot * 0.8F;
		beads.xRot = -torso.xRot * 0.85F + speed * 0.2F;

		rig.xRot = lean;
		rightArm.xRot = rightX;
		rightArm.zRot = rightZ;
		rightForearm.xRot = rightFore;
		leftArm.xRot = leftX;
		leftArm.zRot = leftZ;
		leftForearm.xRot = leftFore;
		// Claws flex now and then.
		leftHand.xRot = -0.2F + Mth.sin(age * 0.21F) * 0.1F;
		// The staff stands upright in the world, whatever her arm and back do, then tilts with the action.
		staff.xRot = -(torso.xRot + rightArm.xRot + rightForearm.xRot + rightHand.xRot) + staffTilt;
		staff.zRot = -rightArm.zRot;
	}

	/** How far her jaw hangs open, in radians, for what she is doing. */
	private static float mouth(BabaYagaRenderState state, YagaAction action, float t, float age) {
		if (state.dying >= 0.0F) {
			// Screaming as she burns, her jaw jerking with each fit.
			return 0.35F + 0.5F * YagaDeath.convulsion(state.dying) * (0.6F + 0.4F * Mth.sin(state.dying * 1.9F));
		}
		// At rest she mutters to herself, in bursts.
		float mutter = Mth.sin(age * 0.045F) > 0.2F ? 0.05F + 0.07F * Math.abs(Mth.sin(age * 0.6F)) : 0.03F;
		return switch (action) {
			// A howl as her creatures rise.
			case SUMMON -> 0.75F * smooth(t / 6.0F) + Mth.sin(t * 2.3F) * 0.05F;
			// A cackle, then her mouth hangs open as the flask flies.
			case THROW -> t < YagaAction.THROW_RELEASE ? 0.25F + 0.2F * Math.abs(Mth.sin(t * 1.4F)) : 0.35F;
			// Chanting under her breath, then a hiss as the spell leaves the staff.
			case SPELL, ACID, THREADS -> {
				int release = action == YagaAction.SPELL ? YagaAction.SPELL_RELEASE : YagaAction.BOLT_RELEASE;
				yield t < release ? 0.12F + 0.12F * Math.abs(Mth.sin(t * 1.1F)) : 0.6F * Math.max(0.0F, 1.0F - (t - release) / 8.0F);
			}
			case GLYPH -> 0.2F + 0.18F * Math.abs(Mth.sin(t * 0.9F));
			// The shriek of the dive, the jaw wide until the blow.
			case SWOOP -> t < state.strikeTick ? 0.8F * smooth(t / 3.0F) : 0.45F;
			case BEAM -> 0.25F + 0.4F * smooth((t - state.beamWindup + 6.0F) / 6.0F) + (t > state.beamWindup ? Mth.sin(t * 2.1F) * 0.06F : 0.0F);
			// The screech behind the player's back, then snapping with every rake.
			case CLAWS -> t < state.lunge ? 0.85F * smooth(t / 2.0F) : 0.3F + 0.3F * Math.abs(Mth.sin(t * 0.9F));
			case EAT -> 0.1F + 0.55F * Math.abs(Mth.sin(t * 0.8F));
			case EAT_RUN, ESCAPE, SHATTER -> 0.6F;
			case STAGGER -> 0.65F * (1.0F - smooth(t / 20.0F));
			default -> mutter;
		};
	}

	/** Now and then her head jerks to one side and holds there a moment, then snaps back. */
	private static float twitch(float age) {
		float period = 43.0F;
		int slot = Mth.floor(age / period);
		if (noise(slot, 20) < 0.45F) {
			return 0.0F;
		}
		float into = age - slot * period;
		float hold = into < 1.5F ? into / 1.5F : into < 9.0F ? 1.0F : Math.max(0.0F, 1.0F - (into - 9.0F) / 2.0F);
		return (noise(slot, 21) > 0.5F ? 1.0F : -1.0F) * (0.3F + 0.25F * noise(slot, 22)) * hold;
	}

	/** A fit's pose for one limb, -1 to 1: a new random one every step, snapped to from the last. */
	private static float jerk(int step, float snap, int limb) {
		return Mth.lerp(snap, noise(step, limb), noise(step + 1, limb)) * 2.0F - 1.0F;
	}

	private static float noise(int step, int limb) {
		return Mth.frac(Mth.sin(step * 12.9898F + limb * 78.233F) * 43758.547F);
	}

	private static float smooth(float x) {
		float c = Mth.clamp(x, 0.0F, 1.0F);
		return c * c * (3.0F - 2.0F * c);
	}
}
