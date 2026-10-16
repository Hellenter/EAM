package io.github.hellenter.eam.client.mobs.koschei;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.entity.koschei.KoscheiAction;

import net.minecraft.client.model.HumanoidModel;
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
 * Koschei: a gaunt bald old man with a long bony face and deep-set eyes under a heavy brow, a tall spiked crown of gold
 * filigree, dark plate worked with filigree over his chest, a thick black fur mantle round his shoulders and up behind
 * his neck, a mail skirt split in front, a long dark cloak hanging behind, spiked pauldrons, vambraces and tall boots. His
 * long sword is part of the model, held in the left hand: the right is kept free for his burst of force. Arms and legs
 * bend at the elbows and knees; every blow is posed here from the action and the ticks into it.
 */
public class KoscheiModel extends HumanoidModel<KoscheiRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Eam.id("koschei"), "main");
	private static final float ELBOW = 4.0F;
	private static final float KNEE = 6.0F;
	private static final float SHIN = 6.0F;
	private static final float HIP = 12.0F;
	/** The sword's grip sits this far down the forearm, in the fist. */
	private static final float HAND = 6.0F;
	private static final float HEAD_SCALE = 0.765F;
	private static final float MAIL_TOP = 10.0F;
	private static final float CAPE_UPPER = 11.0F;
	/** A right angle: the blade lies along the forearm. */
	private static final float ALONG = Mth.HALF_PI;

	private final ModelPart rightForearm;
	private final ModelPart leftForearm;
	private final ModelPart rightShin;
	private final ModelPart leftShin;
	private final ModelPart sword;
	private final ModelPart mailFrontRight;
	private final ModelPart mailFrontLeft;
	private final ModelPart mailBack;
	private final ModelPart cape;
	private final ModelPart capeLower;

	public KoscheiModel(ModelPart root) {
		super(root);
		rightForearm = rightArm.getChild("right_forearm");
		leftForearm = leftArm.getChild("left_forearm");
		rightShin = rightLeg.getChild("right_shin");
		leftShin = leftLeg.getChild("left_shin");
		sword = leftForearm.getChild("sword");
		mailFrontRight = body.getChild("mail_front_right");
		mailFrontLeft = body.getChild("mail_front_left");
		mailBack = body.getChild("mail_back");
		cape = body.getChild("cape");
		capeLower = cape.getChild("lower");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		// He is thin: the torso, legs and the gear on them are drawn narrower than their texture boxes.
		CubeDeformation thinTorso = new CubeDeformation(-1.0F, 0.0F, -0.45F);
		CubeDeformation thinPlate = new CubeDeformation(-0.8F, 0.0F, 0.0F);
		CubeDeformation thinLeg = new CubeDeformation(-0.6F, 0.0F, -0.5F);
		CubeDeformation thinCuff = new CubeDeformation(-0.6F, 0.0F, -0.6F);
		// A long bald skull, a hooked nose, a heavy brow over sunken eyes, thin ears; under the cheekbones a narrow jaw with
		// hollow cheeks, so the face is gaunt. The crown: a ring of four plates round
		// the head and spikes rising from it, tallest in front.
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-4.0F, -9.0F, -4.0F, 8.0F, 6.0F, 8.0F)
				.texOffs(76, 0).addBox(-3.0F, -3.0F, -4.0F, 6.0F, 4.0F, 6.0F)
				.texOffs(32, 0).addBox(-0.5F, -5.5F, -5.0F, 1.0F, 2.0F, 1.0F)
				.texOffs(36, 0).addBox(-3.5F, -6.0F, -4.6F, 7.0F, 1.0F, 1.0F)
				.texOffs(32, 4).addBox(-4.6F, -6.0F, -0.5F, 1.0F, 2.0F, 1.0F)
				.texOffs(32, 4).mirror().addBox(3.6F, -6.0F, -0.5F, 1.0F, 2.0F, 1.0F).mirror(false)
				.texOffs(52, 0).addBox(-4.5F, -9.6F, -4.7F, 9.0F, 2.0F, 1.0F)
				.texOffs(52, 0).addBox(-4.5F, -9.6F, 3.7F, 9.0F, 2.0F, 1.0F)
				.texOffs(52, 4).addBox(-4.7F, -9.6F, -4.5F, 1.0F, 2.0F, 9.0F)
				.texOffs(52, 4).mirror().addBox(3.7F, -9.6F, -4.5F, 1.0F, 2.0F, 9.0F).mirror(false)
				.texOffs(72, 0).addBox(-0.5F, -13.6F, -4.7F, 1.0F, 4.0F, 1.0F)
				.texOffs(72, 0).addBox(-3.2F, -12.6F, -4.7F, 1.0F, 3.0F, 1.0F)
				.texOffs(72, 0).addBox(2.2F, -12.6F, -4.7F, 1.0F, 3.0F, 1.0F)
				.texOffs(72, 0).addBox(-4.7F, -12.1F, -2.5F, 1.0F, 3.0F, 1.0F)
				.texOffs(72, 0).addBox(3.7F, -12.1F, -2.5F, 1.0F, 3.0F, 1.0F)
				.texOffs(72, 0).addBox(-4.7F, -11.6F, 1.5F, 1.0F, 2.0F, 1.0F)
				.texOffs(72, 0).addBox(3.7F, -11.6F, 1.5F, 1.0F, 2.0F, 1.0F)
				.texOffs(72, 0).addBox(-0.5F, -12.1F, 3.7F, 1.0F, 3.0F, 1.0F), PartPose.ZERO.withScale(HEAD_SCALE));
		head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
		// Dark plate over a narrow chest, its breastplate worked with filigree; the fur mantle and its collar up behind the
		// neck; a belt with a heavy buckle.
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(0, 20).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, thinTorso)
				.texOffs(24, 20).addBox(-3.5F, 0.5F, -2.35F, 7.0F, 6.0F, 1.0F, thinPlate)
				.texOffs(0, 40).addBox(-6.0F, -1.5F, -3.5F, 12.0F, 4.0F, 7.0F, new CubeDeformation(-1.2F, 0.0F, -0.6F))
				.texOffs(40, 40).addBox(-5.0F, -4.0F, 1.2F, 10.0F, 3.0F, 2.0F, new CubeDeformation(-1.0F, 0.0F, 0.0F))
				.texOffs(64, 40).addBox(-4.5F, 9.0F, -2.5F, 9.0F, 2.0F, 5.0F, new CubeDeformation(-1.1F, 0.0F, -0.4F))
				.texOffs(92, 40).addBox(-1.5F, 8.5F, -2.6F, 3.0F, 3.0F, 1.0F), PartPose.ZERO);
		// The mail skirt from under the belt to the knees, split in front, and the long cloak behind.
		body.addOrReplaceChild("mail_front_right", CubeListBuilder.create().texOffs(0, 52).addBox(-2.5F, 0.0F, -0.5F, 5.0F, 8.0F, 1.0F, new CubeDeformation(-0.4F, 0.0F, 0.0F)),
				PartPose.offset(-1.8F, MAIL_TOP, -2.0F));
		body.addOrReplaceChild("mail_front_left", CubeListBuilder.create().texOffs(12, 52).addBox(-2.5F, 0.0F, -0.5F, 5.0F, 8.0F, 1.0F, new CubeDeformation(-0.4F, 0.0F, 0.0F)),
				PartPose.offset(1.8F, MAIL_TOP, -2.0F));
		body.addOrReplaceChild("mail_back", CubeListBuilder.create().texOffs(24, 52).addBox(-5.0F, 0.0F, -0.5F, 10.0F, 8.0F, 1.0F, new CubeDeformation(-1.0F, 0.0F, 0.0F)),
				PartPose.offset(0.0F, MAIL_TOP, 2.0F));
		body.addOrReplaceChild("mail_right", CubeListBuilder.create().texOffs(46, 52).addBox(-0.5F, 0.0F, -3.0F, 1.0F, 8.0F, 6.0F, new CubeDeformation(0.0F, 0.0F, -0.6F)),
				PartPose.offset(-3.6F, MAIL_TOP, 0.0F));
		body.addOrReplaceChild("mail_left", CubeListBuilder.create().texOffs(60, 52).addBox(-0.5F, 0.0F, -3.0F, 1.0F, 8.0F, 6.0F, new CubeDeformation(0.0F, 0.0F, -0.6F)),
				PartPose.offset(3.6F, MAIL_TOP, 0.0F));
		body.addOrReplaceChild("cape", CubeListBuilder.create().texOffs(74, 52).addBox(-5.5F, 0.0F, 0.0F, 11.0F, CAPE_UPPER, 1.0F, thinPlate),
				PartPose.offset(0.0F, 0.5F, 2.6F))
				.addOrReplaceChild("lower", CubeListBuilder.create().texOffs(74, 64).addBox(-5.5F, 0.0F, 0.0F, 11.0F, 11.0F, 1.0F, thinPlate),
						PartPose.offset(0.0F, CAPE_UPPER, 0.0F));
		// Thin arms under spiked pauldrons, vambraces over the forearms; they bend at the elbow.
		root.addOrReplaceChild("right_arm", CubeListBuilder.create()
				.texOffs(0, 80).addBox(-2.0F, -2.0F, -1.5F, 3.0F, 6.0F, 3.0F, new CubeDeformation(-0.25F, 0.0F, -0.25F))
				.texOffs(12, 80).addBox(-3.5F, -3.0F, -2.5F, 5.0F, 3.0F, 5.0F, new CubeDeformation(-0.4F)), PartPose.offset(-4.4F, 2.0F, 0.0F))
				.addOrReplaceChild("right_forearm", CubeListBuilder.create()
						.texOffs(32, 80).addBox(-2.0F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F, new CubeDeformation(-0.25F, 0.0F, -0.25F))
						.texOffs(44, 80).addBox(-2.5F, 0.5F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.4F, 0.0F, -0.4F)), PartPose.offset(0.0F, ELBOW, 0.0F));
		PartDefinition leftForearm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().mirror()
				.texOffs(0, 80).addBox(-1.0F, -2.0F, -1.5F, 3.0F, 6.0F, 3.0F, new CubeDeformation(-0.25F, 0.0F, -0.25F))
				.texOffs(12, 80).addBox(-1.5F, -3.0F, -2.5F, 5.0F, 3.0F, 5.0F, new CubeDeformation(-0.4F)), PartPose.offset(4.4F, 2.0F, 0.0F))
				.addOrReplaceChild("left_forearm", CubeListBuilder.create().mirror()
						.texOffs(32, 80).addBox(-1.0F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F, new CubeDeformation(-0.25F, 0.0F, -0.25F))
						.texOffs(44, 80).addBox(-1.5F, 0.5F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(-0.4F, 0.0F, -0.4F)), PartPose.offset(0.0F, ELBOW, 0.0F));
		// The sword in his fist, its blade along -Z: the grip, the pommel behind, the cross-guard, a long narrow blade and
		// its point.
		leftForearm.addOrReplaceChild("sword", CubeListBuilder.create()
				.texOffs(0, 104).addBox(-0.5F, -0.5F, -1.0F, 1.0F, 1.0F, 3.0F)
				.texOffs(8, 104).addBox(-1.0F, -1.0F, 2.0F, 2.0F, 2.0F, 1.0F)
				.texOffs(40, 104).addBox(-0.5F, -3.0F, -2.0F, 1.0F, 6.0F, 1.0F)
				.texOffs(0, 108).addBox(-0.5F, -1.0F, -18.0F, 1.0F, 2.0F, 16.0F, new CubeDeformation(-0.2F, 0.0F, 0.0F))
				.texOffs(46, 104).addBox(-0.5F, -0.5F, -19.5F, 1.0F, 1.0F, 2.0F, new CubeDeformation(-0.2F, 0.0F, 0.0F)),
				PartPose.offset(0.5F, HAND, 0.0F));
		// Legs bending at the knee, tall boots with a cuff and a toe.
		root.addOrReplaceChild("right_leg", CubeListBuilder.create()
				.texOffs(0, 92).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, thinLeg), PartPose.offset(-1.7F, HIP, 0.0F))
				.addOrReplaceChild("right_shin", CubeListBuilder.create()
						.texOffs(16, 92).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, thinLeg)
						.texOffs(32, 92).addBox(-2.5F, -0.5F, -2.5F, 5.0F, 3.0F, 5.0F, thinCuff)
						.texOffs(52, 92).addBox(-2.0F, 5.0F, -2.6F, 4.0F, 1.0F, 1.0F, new CubeDeformation(-0.6F, 0.0F, 0.0F)), PartPose.offset(0.0F, KNEE, 0.0F));
		root.addOrReplaceChild("left_leg", CubeListBuilder.create().mirror()
				.texOffs(0, 92).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, thinLeg), PartPose.offset(1.7F, HIP, 0.0F))
				.addOrReplaceChild("left_shin", CubeListBuilder.create().mirror()
						.texOffs(16, 92).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, thinLeg)
						.texOffs(32, 92).addBox(-2.5F, -0.5F, -2.5F, 5.0F, 3.0F, 5.0F, thinCuff)
						.texOffs(52, 92).addBox(-2.0F, 5.0F, -2.6F, 4.0F, 1.0F, 1.0F, new CubeDeformation(-0.6F, 0.0F, 0.0F)), PartPose.offset(0.0F, KNEE, 0.0F));
		return LayerDefinition.create(mesh, 128, 128);
	}

	@Override
	public void setupAnim(KoscheiRenderState state) {
		super.setupAnim(state);
		walk(state);
		// The guard: the sword held out before him, its point up toward the enemy's face; the free hand low at his side.
		leftArm.xRot = leftArm.xRot * 0.4F - 0.35F;
		leftArm.zRot = -0.12F;
		leftForearm.xRot = -0.75F;
		sword.xRot = 0.15F;
		sword.zRot = 0.0F;
		sword.yRot = 0.0F;
		rightArm.zRot = 0.12F;
		float breath = Mth.sin(state.ageInTicks * 0.06F);
		leftArm.xRot += breath * 0.03F;
		body.xRot = 0.04F + breath * 0.01F;
		head.xRot += 0.08F;
		float t = state.actionTime;
		float restL = leftArm.xRot;
		float restR = rightArm.xRot;
		float restLz = leftArm.zRot;
		float restRz = rightArm.zRot;
		float restFl = leftForearm.xRot;
		float restFr = rightForearm.xRot;
		float restS = sword.xRot;
		switch (state.action) {
			case STRIKE -> {
				// Raised back over the left shoulder, then one cut down and across.
				float[] times = {0, 5, 7, 10, 14};
				leftArm.xRot = keys(t, times, restL, -2.7F, -0.5F, -0.6F, restL);
				leftArm.yRot = keys(t, times, 0.0F, 0.45F, -0.5F, -0.45F, 0.0F);
				leftForearm.xRot = keys(t, times, restFl, -1.0F, -0.1F, -0.2F, restFl);
				sword.xRot = keys(t, times, restS, 0.4F, 1.2F, 1.1F, restS);
				body.yRot = keys(t, times, 0.0F, 0.3F, -0.35F, -0.3F, 0.0F);
				step(keys(t, times, 0.0F, 0.3F, 1.0F, 0.8F, 0.0F), false);
			}
			case DOUBLE -> {
				// A forehand cut down and across, then a backhand cut back the other way.
				float[] times = {0, 4, 6, 9, 12, 15, 20};
				leftArm.xRot = keys(t, times, restL, -2.4F, -0.6F, -1.3F, -1.25F, -1.1F, restL);
				leftArm.yRot = keys(t, times, 0.0F, 0.5F, -0.6F, -0.9F, 0.75F, 0.6F, 0.0F);
				leftForearm.xRot = keys(t, times, restFl, -0.9F, -0.1F, -0.5F, -0.15F, -0.3F, restFl);
				sword.xRot = keys(t, times, restS, 0.4F, 1.2F, 1.0F, 1.4F, 1.2F, restS);
				body.yRot = keys(t, times, 0.0F, 0.35F, -0.4F, -0.5F, 0.4F, 0.3F, 0.0F);
				step(keys(t, times, 0.0F, 0.2F, 0.9F, 0.6F, 0.9F, 0.5F, 0.0F), false);
			}
			case ORANGE -> {
				// Swung far out behind him to the left as he runs in, then one wide flat sweep across the front.
				float hit = KoscheiAction.ORANGE_HIT;
				float[] times = {0, hit - 4, hit - 1, hit, hit + 3, KoscheiAction.ORANGE.length()};
				leftArm.xRot = keys(t, times, restL, -1.3F, -1.35F, -1.4F, -1.3F, restL);
				leftArm.yRot = keys(t, times, 0.0F, 1.25F, 1.35F, -0.9F, -1.1F, 0.0F);
				leftArm.zRot = keys(t, times, restLz, -0.5F, -0.55F, -0.2F, -0.2F, restLz);
				leftForearm.xRot = keys(t, times, restFl, -0.3F, -0.3F, -0.05F, -0.2F, restFl);
				sword.xRot = keys(t, times, restS, 1.3F, 1.35F, 1.5F, 1.4F, restS);
				body.yRot = keys(t, times, 0.0F, 0.7F, 0.75F, -0.5F, -0.55F, 0.0F);
				rightArm.zRot = keys(t, times, restRz, 0.6F, 0.6F, 0.3F, 0.3F, restRz);
				stance(keys(t, times, 0.0F, 1.0F, 1.0F, 0.8F, 0.6F, 0.0F));
			}
			case RED -> {
				// Drawn low behind him, then a rising cut up and across.
				float[] times = {0, 9, 13, 14, 17, 24};
				leftArm.xRot = keys(t, times, restL, 0.9F, 1.0F, -1.6F, -1.9F, restL);
				leftArm.yRot = keys(t, times, 0.0F, 0.3F, 0.35F, -0.6F, -0.7F, 0.0F);
				leftForearm.xRot = keys(t, times, restFl, -0.3F, -0.3F, -0.2F, -0.3F, restFl);
				sword.xRot = keys(t, times, restS, 0.6F, 0.7F, 1.3F, 1.2F, restS);
				body.yRot = keys(t, times, 0.0F, 0.5F, 0.55F, -0.4F, -0.45F, 0.0F);
				body.xRot += keys(t, times, 0.0F, 0.25F, 0.25F, -0.05F, 0.0F, 0.0F);
				stance(keys(t, times, 0.0F, 1.0F, 1.0F, 0.6F, 0.4F, 0.0F));
			}
			case OVERHEAD -> overhead(t, restL, restR, restFl, restFr, restS);
			case LUNGE -> lunge(t, restL, restFl, restS);
			case FORCE -> {
				// The right hand drawn in and thrust out, palm open; the sword held aside.
				float[] times = {0, 5, 8, 10, 16};
				rightArm.xRot = keys(t, times, restR, -1.1F, -1.6F, -1.6F, restR);
				rightArm.yRot = keys(t, times, 0.0F, 0.2F, 0.0F, 0.0F, 0.0F);
				rightForearm.xRot = keys(t, times, restFr, -1.5F, 0.0F, -0.05F, restFr);
				body.yRot = keys(t, times, 0.0F, -0.3F, 0.3F, 0.25F, 0.0F);
				float aside = keys(t, times, 0.0F, 1.0F, 1.0F, 1.0F, 0.0F);
				leftArm.zRot = Mth.lerp(aside, restLz, -0.45F);
				leftArm.xRot = Mth.lerp(aside, restL, 0.1F);
				step(keys(t, times, 0.0F, 0.2F, 1.0F, 0.9F, 0.0F), true);
			}
			case BOLT -> {
				// The sword levelled at the target like a spear, the free hand drawn back; it trembles while the bolt holds.
				float[] times = {0, 10, 18, 48, 52};
				float aim = keys(t, times, 0.0F, 1.0F, 1.0F, 1.0F, 0.0F);
				float tremble = t >= KoscheiAction.BOLT_RELEASE && t < KoscheiAction.BOLT_END ? Mth.sin(t * 2.7F) * 0.05F : 0.0F;
				leftArm.xRot = Mth.lerp(aim, restL, -1.55F + tremble + state.xRot * Mth.DEG_TO_RAD * 0.6F);
				leftArm.yRot = Mth.lerp(aim, 0.0F, -0.25F);
				leftArm.zRot = Mth.lerp(aim, restLz, 0.0F);
				leftForearm.xRot = Mth.lerp(aim, restFl, 0.0F);
				sword.xRot = Mth.lerp(aim, restS, ALONG);
				rightArm.xRot = Mth.lerp(aim, restR, -0.6F);
				rightArm.zRot = Mth.lerp(aim, restRz, 0.5F);
				rightForearm.xRot = Mth.lerp(aim, restFr, -1.4F);
				body.yRot = -0.35F * aim;
				stance(0.7F * aim);
			}
			case TELEPORT -> {
				// Arms spread low, head bowed as his eyes flicker.
				float in = keys(t, new float[] {0, 4, 12}, 0.0F, 1.0F, 1.0F);
				leftArm.zRot = Mth.lerp(in, restLz, -0.7F);
				rightArm.zRot = Mth.lerp(in, restRz, 0.7F);
				head.xRot += 0.3F * in;
				stance(0.4F * in);
			}
			case HOP -> {
				float tuck = keys(t, new float[] {0, 2, 6, 9}, 0.0F, 1.0F, 0.6F, 0.0F);
				bendLeg(rightLeg, rightShin, tuck, -0.6F, 1.1F);
				bendLeg(leftLeg, leftShin, tuck, -0.4F, 1.0F);
				rightArm.zRot = Mth.lerp(tuck, restRz, 0.6F);
				leftArm.zRot = Mth.lerp(tuck, restLz, -0.4F);
			}
			case BLOCK -> {
				// The sword held across where the blow comes, tipped toward it; a jolt as it lands.
				float length = state.action.length();
				float out = keys(t, new float[] {0, 1.0F, length - 3, length}, 0.0F, 1.0F, 1.0F, 0.0F);
				float jolt = keys(t, new float[] {0, 1.0F, 2.5F, 4.0F}, 0.0F, 0.0F, 1.0F, 0.0F);
				float[] guard = guardPose(state.guardHeight, state.guardSide);
				leftArm.xRot = Mth.lerp(out, restL, guard[0] + 0.06F * jolt);
				leftArm.yRot = Mth.lerp(out, 0.0F, guard[1]);
				leftArm.zRot = Mth.lerp(out, restLz, guard[2]);
				leftForearm.xRot = Mth.lerp(out, restFl, guard[3]);
				sword.xRot = Mth.lerp(out, restS, guard[4]);
				sword.yRot = out * guard[5];
				sword.zRot = out * guard[6] + 0.05F * jolt;
				step(0.3F * out, false);
			}
			case KICK -> {
				// A step in, the knee drawn up, the boot driven out at the shield, leaning back; then the foot down again.
				float hit = KoscheiAction.KICK_HIT;
				float[] times = {0, hit - 3, hit, hit + 2, KoscheiAction.KICK.length()};
				rightLeg.xRot = keys(t, times, rightLeg.xRot, -1.1F, -1.5F, -1.4F, 0.0F);
				rightShin.xRot = keys(t, times, rightShin.xRot, 1.5F, 0.05F, 0.1F, 0.0F);
				body.xRot += keys(t, times, 0.0F, -0.1F, -0.25F, -0.2F, 0.0F);
				rightArm.zRot = keys(t, times, restRz, 0.5F, 0.7F, 0.6F, restRz);
				leftArm.yRot = keys(t, times, 0.0F, 0.3F, 0.4F, 0.4F, 0.0F);
				leftArm.zRot = keys(t, times, restLz, -0.3F, -0.4F, -0.35F, restLz);
			}
			case PARRY_ARROW -> {
				// A flick of the blade across the body.
				float length = state.action.length();
				float out = keys(t, new float[] {0, 1.5F, length - 3, length}, 0.0F, 1.0F, 1.0F, 0.0F);
				float sweep = keys(t, new float[] {0, 0.5F, 2.5F, length}, 0.0F, 0.0F, 1.0F, 1.0F);
				leftArm.xRot = Mth.lerp(out, restL, -1.2F);
				leftArm.yRot = Mth.lerp(out, 0.0F, Mth.lerp(sweep, -0.7F, 0.5F));
				leftForearm.xRot = Mth.lerp(out, restFl, -0.5F);
				sword.xRot = Mth.lerp(out, restS, 0.2F);
				sword.zRot = 0.4F * out;
				body.yRot = Mth.lerp(sweep, -0.25F, 0.3F) * out;
			}
			case STAGGER -> {
				// Thrown off balance: arms flung out, leaning back.
				float off = keys(t, new float[] {0, 3, 18, 24}, 0.0F, 1.0F, 1.0F, 0.0F);
				leftArm.xRot = Mth.lerp(off, restL, -1.0F);
				rightArm.xRot = Mth.lerp(off, restR, -0.9F);
				leftArm.zRot = Mth.lerp(off, restLz, -0.9F);
				rightArm.zRot = Mth.lerp(off, restRz, 0.9F);
				leftForearm.xRot = Mth.lerp(off, restFl, -0.5F);
				sword.xRot = Mth.lerp(off, restS, -0.6F);
				body.xRot = Mth.lerp(off, body.xRot, -0.25F);
				head.xRot -= 0.45F * off;
				bendLeg(rightLeg, rightShin, off, -0.35F, 0.5F);
			}
			case NONE -> {
			}
		}
		// Bent knees would lift his feet off the ground: he sinks instead, only a little while he just walks.
		float sink = Math.min(footLift(rightLeg.xRot, rightShin.xRot), footLift(leftLeg.xRot, leftShin.xRot));
		root().y += sink * (state.action == KoscheiAction.NONE ? 0.2F : 1.0F);
		cloth(state);
	}

	/** Both hands on the hilt raised high, the blade hanging back over his head; held; then brought down, slow at first. */
	private void overhead(float t, float restL, float restR, float restFl, float restFr, float restS) {
		float descent = KoscheiAction.OVERHEAD_DESCENT;
		float hit = KoscheiAction.OVERHEAD_HIT;
		float[] times = {0, 10, descent, descent + 3, hit, hit + 3, 34};
		float shake = t > 10 && t < descent ? Mth.sin(t * 2.2F) * 0.03F : 0.0F;
		leftArm.xRot = keys(t, times, restL, -3.0F, -3.05F, -2.75F, -0.6F, -0.6F, restL) + shake;
		rightArm.xRot = keys(t, times, restR, -2.9F, -2.95F, -2.65F, -0.6F, -0.6F, restR) + shake;
		leftArm.yRot = keys(t, times, 0.0F, -0.35F, -0.35F, -0.3F, -0.3F, -0.3F, 0.0F);
		rightArm.yRot = keys(t, times, 0.0F, 0.35F, 0.35F, 0.3F, 0.3F, 0.3F, 0.0F);
		leftForearm.xRot = keys(t, times, restFl, -1.0F, -1.0F, -0.8F, -0.1F, -0.15F, restFl);
		rightForearm.xRot = keys(t, times, restFr, -1.0F, -1.0F, -0.8F, -0.1F, -0.15F, restFr);
		sword.xRot = keys(t, times, restS, ALONG, ALONG, ALONG, 1.45F, 1.4F, restS);
		body.xRot += keys(t, times, 0.0F, -0.15F, -0.18F, -0.1F, 0.35F, 0.3F, 0.0F);
		step(keys(t, times, 0.0F, 0.0F, 0.0F, 0.3F, 1.0F, 0.9F, 0.0F), false);
	}

	/** Heights of the guard poses, as a share of his height: legs, chest, head. */
	private static final float[] GUARD_HEIGHTS = {0.3F, 0.6F, 0.88F};
	/**
	 * The guard against a blow at each height (legs, chest, head) and side (his right, the middle, his left): left arm x, y
	 * and z, forearm x, sword x, y and z. Worked out so the blade crosses the spot hit and leans toward it: point down
	 * before the legs, point up before the chest, flat across the face.
	 */
	private static final float[][][] GUARD_POSES = {
			{
					{-0.35F, 0.66F, 0.5F, 0.0F, 1.76F, 0.14F, -0.26F},
					{-0.3F, 0.08F, 0.5F, -0.01F, 1.53F, 0.0F, -0.49F},
					{-0.17F, -0.45F, 0.47F, -0.37F, 1.37F, -0.27F, -0.47F}
			},
			{
					{-0.36F, 0.75F, 0.5F, -0.18F, -0.31F, 0.36F, -0.16F},
					{-0.08F, 0.55F, 0.33F, -0.53F, -0.62F, 0.11F, -0.14F},
					{-0.18F, -0.05F, 0.37F, -0.26F, -0.93F, 0.22F, 0.06F}
			},
			{
					{-1.02F, -1.02F, -0.95F, -1.27F, 0.27F, 0.35F, 0.32F},
					{-0.76F, -0.85F, -0.82F, -1.64F, 0.37F, 0.46F, 0.0F},
					{-0.72F, -0.8F, -0.75F, -1.71F, 0.54F, 0.44F, -0.08F}
			}
	};

	/** The guard pose for a blow at {@code height} (share of his height) and {@code side} (-1 his right to 1 his left). */
	private static float[] guardPose(float height, float side) {
		int row = height < GUARD_HEIGHTS[1] ? 0 : 1;
		float across = Math.clamp((height - GUARD_HEIGHTS[row]) / (GUARD_HEIGHTS[row + 1] - GUARD_HEIGHTS[row]), 0.0F, 1.0F);
		float s = Math.clamp(side, -1.0F, 1.0F) + 1.0F;
		int col = Math.min((int) s, 1);
		float along = s - col;
		float[] pose = new float[7];
		for (int i = 0; i < pose.length; i++) {
			float near = Mth.lerp(along, GUARD_POSES[row][col][i], GUARD_POSES[row][col + 1][i]);
			float far = Mth.lerp(along, GUARD_POSES[row + 1][col][i], GUARD_POSES[row + 1][col + 1][i]);
			pose[i] = Mth.lerp(across, near, far);
		}
		return pose;
	}

	/** Drawn back with the point at the target, a run in, the stab at full stretch, and a hop back. */
	private void lunge(float t, float restL, float restFl, float restS) {
		float push = KoscheiAction.LUNGE_PUSH;
		float stab = KoscheiAction.LUNGE_STAB;
		// Drawn back by the hold, held still through it, then the rush.
		float[] times = {0, push - KoscheiAction.LUNGE_HOLD, push, stab - 2, stab, stab + 3, KoscheiAction.LUNGE.length()};
		leftArm.xRot = keys(t, times, restL, 0.5F, 0.5F, 0.3F, -1.55F, -1.5F, restL);
		leftArm.yRot = keys(t, times, 0.0F, 0.2F, 0.2F, 0.1F, -0.1F, -0.1F, 0.0F);
		leftForearm.xRot = keys(t, times, restFl, -1.8F, -1.8F, -1.6F, 0.0F, -0.1F, restFl);
		sword.xRot = keys(t, times, restS, 1.3F, 1.3F, 1.3F, ALONG, 1.5F, restS);
		body.yRot = keys(t, times, 0.0F, 0.5F, 0.5F, 0.4F, -0.35F, -0.3F, 0.0F);
		body.xRot += keys(t, times, 0.0F, 0.1F, 0.1F, 0.2F, 0.3F, 0.25F, 0.0F);
		rightArm.xRot = keys(t, times, rightArm.xRot, -0.3F, -0.3F, 0.3F, 0.7F, 0.6F, rightArm.xRot);
		rightArm.zRot = keys(t, times, rightArm.zRot, 0.4F, 0.4F, 0.4F, 0.5F, 0.5F, rightArm.zRot);
		if (t >= push && t < stab - 1) {
			// Running in.
			float stride = Mth.sin((t - push) * 1.6F);
			bendLeg(rightLeg, rightShin, 1.0F, -0.8F * stride, 0.3F + 0.8F * Math.max(0.0F, stride));
			bendLeg(leftLeg, leftShin, 1.0F, 0.8F * stride, 0.3F + 0.8F * Math.max(0.0F, -stride));
		} else {
			float crouch = keys(t, times, 0.0F, 0.6F, 0.6F, 0.6F, 1.0F, 1.0F, 0.0F);
			step(crouch, false);
		}
	}

	/** Knees fold as each leg swings forward; the free elbow stays a little bent. */
	private void walk(KoscheiRenderState state) {
		float speed = Math.min(state.walkAnimationSpeed, 1.0F);
		float phase = state.walkAnimationPos * 0.6662F;
		rightShin.xRot = (0.1F + 1.1F * Math.max(0.0F, Mth.sin(phase))) * speed;
		leftShin.xRot = (0.1F + 1.1F * Math.max(0.0F, -Mth.sin(phase))) * speed;
		rightForearm.xRot = -0.2F - 0.6F * Math.max(0.0F, -rightArm.xRot);
		rightArm.yRot = 0.0F;
		leftArm.yRot = 0.0F;
		body.yRot = 0.0F;
	}

	/** The mail and the cloak: they trail as he walks and swing out of the way of his thighs. */
	private void cloth(KoscheiRenderState state) {
		float walk = Math.min(state.walkAnimationSpeed, 1.0F);
		float sway = Mth.sin(state.ageInTicks * 0.08F) * 0.03F;
		float trail = 0.2F * walk + 0.5F * state.dash;
		float flare = Math.clamp(-state.fallSpeed * 1.5F, 0.0F, 0.6F);
		mailFrontRight.xRot = Math.min(sway - flare, rightLeg.xRot * 0.85F);
		mailFrontLeft.xRot = Math.min(-sway - flare, leftLeg.xRot * 0.85F);
		mailBack.xRot = Math.max(trail * 0.5F + flare - sway, Math.max(rightLeg.xRot, leftLeg.xRot) * 0.85F);
		cape.xRot = Math.max(0.05F + trail + flare + sway - body.xRot, Math.max(rightLeg.xRot, leftLeg.xRot) * 0.6F);
		cape.zRot = -Math.clamp(state.turnRate / 25.0F, -1.0F, 1.0F) * 0.25F;
		// The lower half lags behind the swing and hangs back toward the ground.
		capeLower.xRot = -0.5F * cape.xRot + Mth.sin(state.walkAnimationPos * 0.6662F - 1.1F) * 0.1F * walk + 0.3F * trail;
	}

	/** Steps into a blow: the hip forward on one side, the knee bent, the other leg back. */
	private void step(float amount, boolean right) {
		if (amount <= 0.0F) {
			return;
		}
		bendLeg(right ? rightLeg : leftLeg, right ? rightShin : leftShin, amount, -0.45F, 0.55F);
		bendLeg(right ? leftLeg : rightLeg, right ? leftShin : rightShin, amount, 0.3F, 0.15F);
		body.xRot += 0.1F * amount;
	}

	/** Feet set apart and knees bent by {@code amount}. */
	private void stance(float amount) {
		rightLeg.zRot += 0.18F * amount;
		leftLeg.zRot -= 0.18F * amount;
		bendLeg(rightLeg, rightShin, amount, -0.25F, 0.45F);
		bendLeg(leftLeg, leftShin, amount, -0.25F, 0.45F);
	}

	private static void bendLeg(ModelPart leg, ModelPart shin, float amount, float hip, float knee) {
		leg.xRot = Mth.lerp(amount, leg.xRot, hip);
		shin.xRot = Mth.lerp(amount, shin.xRot, knee);
	}

	private static float footLift(float hip, float knee) {
		return KNEE * (1.0F - Mth.cos(hip)) + SHIN * (1.0F - Mth.cos(hip + knee));
	}

	/** The value at {@code t} of a curve through the given keys, eased between them. */
	static float keys(float t, float[] times, float... values) {
		if (t <= times[0]) {
			return values[0];
		}
		for (int i = 1; i < times.length; i++) {
			if (t <= times[i]) {
				float x = (t - times[i - 1]) / (times[i] - times[i - 1]);
				float s = x * x * (3.0F - 2.0F * x);
				return Mth.lerp(s, values[i - 1], values[i]);
			}
		}
		return values[values.length - 1];
	}
}
