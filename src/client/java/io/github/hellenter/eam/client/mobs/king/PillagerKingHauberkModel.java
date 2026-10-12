package io.github.hellenter.eam.client.mobs.king;

import io.github.hellenter.eam.Eam;

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
 * The Pillager King in a closed visored helm and a blackened chainmail hauberk to the knees under a leather yoke, a scarf wound round his neck,
 * two belts at the waist, rounded steel pauldrons and vambraces, tall boots, and a long dark leather coat hanging behind
 * and at the hips down to his shins. The mail skirt is split in front, each half riding its own thigh with a leather flap
 * hanging on from its hem. The long coat flaps bend halfway down: the upper part swings with the legs, the lower part
 * lags behind and hangs back toward the ground. Mail and cloth swing out of the way of his knees and feet, trail as he
 * moves, swing out as he turns and flare in his spins.
 */
public class PillagerKingHauberkModel extends PillagerKingModel {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Eam.id("pillager_king"), "main");
	/** The top of the mail skirt, under the belts, and of the coat flaps. */
	private static final float MAIL_TOP = 10.0F;
	private static final float MAIL_LENGTH = 9.0F;
	private static final float COAT_TOP = 10.5F;
	/** The coat flaps bend this far down. */
	private static final float COAT_UPPER = 6.0F;
	private static final float COAT_LOWER = 7.0F;
	/** The front flaps below the mail. */
	private static final float FRONT_FLAP = 6.0F;
	/** How far in front of and behind the body the mail and the coat hang. */
	private static final float MAIL_Z = 2.6F;
	private static final float COAT_Z = 3.6F;
	/** Half the depth of a leg with its boot cuff, plus a little room. */
	private static final float LEG_ROOM = 3.0F;
	private static final float HIP = 12.0F;

	private final ModelPart mailFrontRight;
	private final ModelPart mailFrontLeft;
	private final ModelPart mailBack;
	private final ModelPart mailRight;
	private final ModelPart mailLeft;
	private final ModelPart coatBack;
	private final ModelPart coatRight;
	private final ModelPart coatLeft;

	public PillagerKingHauberkModel(ModelPart root) {
		super(root);
		mailFrontRight = body.getChild("mail_front_right");
		mailFrontLeft = body.getChild("mail_front_left");
		mailBack = body.getChild("mail_back");
		mailRight = body.getChild("mail_right");
		mailLeft = body.getChild("mail_left");
		coatBack = body.getChild("coat_back");
		coatRight = body.getChild("coat_right");
		coatLeft = body.getChild("coat_left");
	}

	/**
	 * A closed helm: a rounded dome with a ridge running from the brow over the top, and a visor over the face pointed
	 * down its middle, with an eye slit on each side and rows of breathing holes below them (in the texture). The flare
	 * over the nape is the same as on the old helm. It sits in the top left of the 128 x 128 texture, rows 0 to 29.
	 */
	private static void addHelm(PartDefinition root) {
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-5.0F, -10.0F, -5.0F, 10.0F, 10.0F, 10.0F)
				.texOffs(40, 0).addBox(-4.0F, -11.0F, -4.0F, 8.0F, 1.0F, 8.0F)
				.texOffs(72, 0).addBox(-0.5F, -12.0F, -4.0F, 1.0F, 1.0F, 8.0F)
				.texOffs(90, 0).addBox(-0.5F, -11.0F, -5.5F, 1.0F, 3.0F, 1.0F)
				.texOffs(0, 20).addBox(-5.0F, -8.0F, -6.0F, 10.0F, 8.0F, 1.0F)
				.texOffs(22, 20).addBox(-1.0F, -8.0F, -7.0F, 2.0F, 8.0F, 1.0F)
				.texOffs(28, 20).addBox(-5.5F, -1.5F, 0.5F, 11.0F, 2.0F, 5.0F), PartPose.ZERO.withScale(HEAD_SCALE));
		head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		CubeDeformation snug = new CubeDeformation(0.25F);
		addHelm(root);
		// The hauberk, the leather jerkin over the chest, the scarf round the neck with its end hanging in front, and the
		// upper belt.
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(0, 36).addBox(-4.5F, 0.0F, -2.5F, 9.0F, 12.0F, 5.0F)
				.texOffs(28, 36).addBox(-4.5F, 0.0F, -2.5F, 9.0F, 3.0F, 5.0F, new CubeDeformation(0.35F))
				.texOffs(56, 36).addBox(-4.0F, -0.5F, -3.0F, 8.0F, 3.0F, 6.0F, new CubeDeformation(0.2F))
				.texOffs(84, 36).addBox(-3.0F, 2.5F, -3.6F, 6.0F, 4.0F, 1.0F)
				.texOffs(28, 46).addBox(-5.0F, 7.0F, -3.0F, 10.0F, 2.0F, 6.0F), PartPose.ZERO);
		// The lower belt slung across the hips, a leather tongue with a steel tip hanging from it.
		body.addOrReplaceChild("belt", CubeListBuilder.create()
				.texOffs(60, 46).addBox(-5.0F, 0.0F, -3.0F, 10.0F, 1.0F, 6.0F, new CubeDeformation(0.3F))
				.texOffs(92, 46).addBox(0.5F, 0.5F, -3.7F, 2.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 9.4F, 0.0F, 0.0F, 0.0F, 0.1F));
		// The mail skirt from under the belts to below the knees, split in front.
		// In front a leather flap hangs on from under the hem of each half down to the shin.
		body.addOrReplaceChild("mail_front_right", CubeListBuilder.create().texOffs(0, 54).addBox(-2.5F, 0.0F, -0.5F, 5.0F, MAIL_LENGTH, 1.0F),
				PartPose.offset(-2.5F, MAIL_TOP, -MAIL_Z))
				.addOrReplaceChild("flap", CubeListBuilder.create().texOffs(74, 54).addBox(-2.0F, 0.0F, -0.5F, 4.0F, FRONT_FLAP, 1.0F),
						PartPose.offset(-0.3F, MAIL_LENGTH - 0.5F, 0.15F));
		body.addOrReplaceChild("mail_front_left", CubeListBuilder.create().texOffs(12, 54).addBox(-2.5F, 0.0F, -0.5F, 5.0F, MAIL_LENGTH, 1.0F),
				PartPose.offset(2.5F, MAIL_TOP, -MAIL_Z))
				.addOrReplaceChild("flap", CubeListBuilder.create().texOffs(84, 54).addBox(-2.0F, 0.0F, -0.5F, 4.0F, FRONT_FLAP, 1.0F),
						PartPose.offset(0.3F, MAIL_LENGTH - 0.5F, 0.15F));
		body.addOrReplaceChild("mail_back", CubeListBuilder.create().texOffs(24, 54).addBox(-5.0F, 0.0F, -0.5F, 10.0F, MAIL_LENGTH, 1.0F),
				PartPose.offset(0.0F, MAIL_TOP, MAIL_Z));
		body.addOrReplaceChild("mail_right", CubeListBuilder.create().texOffs(46, 54).addBox(-0.5F, 0.0F, -3.0F, 1.0F, MAIL_LENGTH, 6.0F),
				PartPose.offset(-4.9F, MAIL_TOP, 0.0F));
		body.addOrReplaceChild("mail_left", CubeListBuilder.create().texOffs(60, 54).addBox(-0.5F, 0.0F, -3.0F, 1.0F, MAIL_LENGTH, 6.0F),
				PartPose.offset(4.9F, MAIL_TOP, 0.0F));
		// The long coat: a wide flap behind and one on each hip.
		coatFlap(body, "coat_back", 90, 54, -0.5F, 9.0F, 1.0F, PartPose.offset(0.0F, COAT_TOP, COAT_Z));
		coatFlap(body, "coat_right", 0, 70, -2.0F, 1.0F, 4.0F, PartPose.offset(-5.6F, COAT_TOP, 0.0F));
		coatFlap(body, "coat_left", 10, 70, -2.0F, 1.0F, 4.0F, PartPose.offset(5.6F, COAT_TOP, 0.0F));
		// Thin arms in leather sleeves under rounded pauldrons of two plates, steel vambraces over leather gloves; they bend
		// at the elbow.
		root.addOrReplaceChild("right_arm", CubeListBuilder.create()
				.texOffs(0, 92).addBox(-2.0F, -2.0F, -1.5F, 3.0F, 6.0F, 3.0F, snug)
				.texOffs(12, 92).addBox(-3.5F, -3.25F, -2.5F, 5.0F, 4.0F, 5.0F)
				.texOffs(32, 92).addBox(-3.25F, 0.5F, -2.0F, 4.0F, 2.0F, 4.0F), PartPose.offset(-5.5F, 2.0F, 0.0F))
				.addOrReplaceChild("right_forearm", CubeListBuilder.create()
						.texOffs(48, 92).addBox(-2.0F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F, snug)
						.texOffs(60, 92).addBox(-2.5F, 0.5F, -2.0F, 4.0F, 4.0F, 4.0F), PartPose.offset(0.0F, ELBOW, 0.0F));
		root.addOrReplaceChild("left_arm", CubeListBuilder.create().mirror()
				.texOffs(0, 92).addBox(-1.0F, -2.0F, -1.5F, 3.0F, 6.0F, 3.0F, snug)
				.texOffs(12, 92).addBox(-1.5F, -3.25F, -2.5F, 5.0F, 4.0F, 5.0F)
				.texOffs(32, 92).addBox(-0.75F, 0.5F, -2.0F, 4.0F, 2.0F, 4.0F), PartPose.offset(5.5F, 2.0F, 0.0F))
				.addOrReplaceChild("left_forearm", CubeListBuilder.create().mirror()
						.texOffs(48, 92).addBox(-1.0F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F, snug)
						.texOffs(60, 92).addBox(-1.5F, 0.5F, -2.0F, 4.0F, 4.0F, 4.0F), PartPose.offset(0.0F, ELBOW, 0.0F));
		// Legs in dark leather bending at the knee, tall boots with a turned-down cuff over the knee and a toe.
		root.addOrReplaceChild("right_leg", CubeListBuilder.create()
				.texOffs(0, 102).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, snug), PartPose.offset(-2.0F, HIP, 0.0F))
				.addOrReplaceChild("right_shin", CubeListBuilder.create()
						.texOffs(16, 102).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, snug)
						.texOffs(32, 102).addBox(-2.5F, -0.5F, -2.5F, 5.0F, 2.0F, 5.0F)
						.texOffs(52, 102).addBox(-2.0F, 5.0F, -3.0F, 4.0F, 1.0F, 1.0F), PartPose.offset(0.0F, KNEE, 0.0F));
		root.addOrReplaceChild("left_leg", CubeListBuilder.create().mirror()
				.texOffs(0, 102).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, snug), PartPose.offset(2.0F, HIP, 0.0F))
				.addOrReplaceChild("left_shin", CubeListBuilder.create().mirror()
						.texOffs(16, 102).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, snug)
						.texOffs(32, 102).addBox(-2.5F, -0.5F, -2.5F, 5.0F, 2.0F, 5.0F)
						.texOffs(52, 102).addBox(-2.0F, 5.0F, -3.0F, 4.0F, 1.0F, 1.0F), PartPose.offset(0.0F, KNEE, 0.0F));
		return LayerDefinition.create(mesh, 128, 128);
	}

	/**
	 * A coat flap {@code width} wide and {@code depth} deep hanging from {@code pose}, with its lower part a child that
	 * bends at {@link #COAT_UPPER}; the lower part's texture sits right below the upper part's.
	 */
	private static void coatFlap(PartDefinition body, String name, int u, int v, float z, float width, float depth, PartPose pose) {
		body.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v).addBox(-width / 2.0F, 0.0F, z, width, COAT_UPPER, depth), pose)
				.addOrReplaceChild("lower", CubeListBuilder.create().texOffs(u, v + (int) (COAT_UPPER + depth))
						.addBox(-width / 2.0F, 0.0F, z, width, COAT_LOWER, depth), PartPose.offset(0.0F, COAT_UPPER, 0.0F));
	}

	@Override
	protected void cloth(PillagerKingRenderState state) {
		float walk = Math.min(state.walkAnimationSpeed, 1.0F);
		float phase = state.walkAnimationPos * 0.6662F;
		float bounce = Mth.sin(phase * 2.0F) * 0.06F * walk;
		float sway = Mth.sin(state.ageInTicks * 0.09F) * 0.025F;
		float trail = 0.2F * walk + 0.45F * state.dash;
		float flare = Math.clamp(state.spinSpeed / 40.0F, 0.0F, 1.1F) + Math.clamp(-state.fallSpeed * 1.5F, 0.0F, 0.6F);
		float turn = Math.clamp(state.turnRate / 25.0F, -1.0F, 1.0F) * 0.35F;
		float step = Mth.sin(phase) * 0.08F * walk;

		// Mail: each front half rides its own thigh, the back is pushed by whichever thigh swings back.
		mailFrontRight.xRot = Math.min(sway - flare + bounce, -clear(rightLeg, rightShin, MAIL_TOP, -MAIL_Z, MAIL_LENGTH));
		mailFrontLeft.xRot = Math.min(-sway - flare + bounce, -clear(leftLeg, leftShin, MAIL_TOP, -MAIL_Z, MAIL_LENGTH));
		mailBack.xRot = Math.max(trail * 0.6F + bounce - sway + flare,
				Math.max(clear(rightLeg, rightShin, MAIL_TOP, MAIL_Z, MAIL_LENGTH), clear(leftLeg, leftShin, MAIL_TOP, MAIL_Z, MAIL_LENGTH)));
		mailFrontRight.zRot = -turn + 0.4F * flare;
		mailFrontLeft.zRot = -turn - 0.4F * flare;
		mailBack.zRot = -turn;
		mailRight.zRot = 0.9F * Math.max(0.0F, rightLeg.zRot) + flare + Math.max(0.0F, turn) + step + sway;
		mailLeft.zRot = 0.9F * Math.min(0.0F, leftLeg.zRot) - flare + Math.min(0.0F, turn) + step - sway;
		mailRight.xRot = trail * 0.5F;
		mailLeft.xRot = trail * 0.5F;

		// Coat: the same pushes, a little further out, and the trail and flare stronger on the long cloth. The lower parts
		// lag half a step behind the swing and hang back toward the ground, less so the faster they flare, but not while a
		// knee or a foot holds the flap out.
		float coatLength = COAT_UPPER + COAT_LOWER;
		float hang = 0.5F * (1.0F - Math.min(flare, 1.0F));
		float lag = Mth.sin(phase - 1.1F) * 0.14F * walk;
		// The front flaps under the mail hang back toward the ground and lag behind its swing.
		ModelPart flapRight = mailFrontRight.getChild("flap");
		ModelPart flapLeft = mailFrontLeft.getChild("flap");
		flapRight.xRot = -hang * Math.min(0.0F, mailFrontRight.xRot) + lag + 0.15F * trail;
		flapLeft.xRot = -hang * Math.min(0.0F, mailFrontLeft.xRot) - lag + 0.15F * trail;
		flapRight.zRot = -hang * mailFrontRight.zRot;
		flapLeft.zRot = -hang * mailFrontLeft.zRot;
		flap(coatBack, trail + bounce - sway + 1.2F * flare,
				Math.max(clear(rightLeg, rightShin, COAT_TOP, COAT_Z, coatLength), clear(leftLeg, leftShin, COAT_TOP, COAT_Z, coatLength)),
				hang, Mth.sin(phase * 2.0F - 1.1F) * 0.08F * walk + 0.4F * trail);
		coatBack.zRot = -turn * 1.2F;
		flap(coatRight, trail * 0.6F, 0.0F, hang, lag + 0.3F * trail);
		flap(coatLeft, trail * 0.6F, 0.0F, hang, -lag + 0.3F * trail);
		coatRight.zRot = Math.max(0.0F, rightLeg.zRot) + 1.2F * flare + Math.max(0.0F, turn) + step + sway;
		coatLeft.zRot = Math.min(0.0F, leftLeg.zRot) - 1.2F * flare + Math.min(0.0F, turn) + step - sway;
		hangSideways(coatBack, hang);
		hangSideways(coatRight, hang);
		hangSideways(coatLeft, hang);
	}

	/**
	 * Swings a coat flap by {@code free}, or further out to {@code push} when a leg is in the way (below zero in front,
	 * above behind; zero for none). Its lower part swings back by {@code hang} of the free swing, plus {@code lag}.
	 */
	private static void flap(ModelPart flap, float free, float push, float hang, float lag) {
		boolean pushed = push < 0.0F ? push < free : push > free;
		flap.xRot = pushed ? push : free;
		flap.getChild("lower").xRot = (pushed ? 0.0F : -hang * free) + lag;
	}

	/** The lower part of a flap swung out sideways hangs back toward the ground by {@code hang}. */
	private static void hangSideways(ModelPart flap, float hang) {
		flap.getChild("lower").zRot = -hang * flap.zRot;
	}

	/**
	 * How far, in radians, cloth hanging {@code length} down from a pivot at height {@code y} and depth {@code z} (below
	 * zero in front) must swing out to clear this leg's knee and foot; zero when they are not in its way.
	 */
	private static float clear(ModelPart leg, ModelPart shin, float y, float z, float length) {
		float side = Math.signum(z);
		float kneeY = leg.y + KNEE * Mth.cos(leg.xRot);
		float kneeZ = leg.z + KNEE * Mth.sin(leg.xRot);
		float footY = kneeY + SHIN * Mth.cos(leg.xRot + shin.xRot);
		float footZ = kneeZ + SHIN * Mth.sin(leg.xRot + shin.xRot);
		return Math.max(Math.max(0.0F, push(kneeY, kneeZ, y, z, side, length)), push(footY, footZ, y, z, side, length));
	}

	/** The angle that clears a point of a leg {@link #LEG_ROOM} thick, if it lies within {@code length} of the pivot. */
	private static float push(float pointY, float pointZ, float y, float z, float side, float length) {
		float out = side * (pointZ - z) + LEG_ROOM;
		float down = pointY - y;
		if (out <= 0.0F || out * out + down * down > (length + LEG_ROOM) * (length + LEG_ROOM)) {
			return 0.0F;
		}
		return (float) Mth.atan2(out, Math.max(down, 0.5F));
	}
}
