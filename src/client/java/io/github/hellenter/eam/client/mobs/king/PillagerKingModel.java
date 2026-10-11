package io.github.hellenter.eam.client.mobs.king;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.entity.king.KingAction;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

/**
 * The Pillager King in closed black plate: a bulky crested helm with cheek guards, a breastplate over a skirt of plates
 * with dark chainmail hanging from under it to the knees, big pauldrons on thin plated arms, and an axe in each hand.
 * Like the Fallen Lord his arms and legs bend at the elbows and knees. The chainmail is four hanging panels that the
 * thighs push aside, trail behind as he moves, swing out as he turns and flare in his spins; the plume of the crest
 * sways too. His attacks are posed here from the action and the ticks into it; the turns of the tornado are the
 * renderer's.
 */
public class PillagerKingModel extends HumanoidModel<PillagerKingRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Eam.id("pillager_king"), "main");
	/** Arms straight out to the sides. */
	private static final float ARMS_OUT = 1.45F;
	/** The elbow below the shoulder pivot and the knee below the hip, as on the Fallen Lord. */
	private static final float ELBOW = 4.0F;
	private static final float KNEE = 6.0F;
	private static final float SHIN = 6.0F;
	/** The hand at the end of the forearm, as far below the shoulder as on the vanilla arm. */
	private static final float HAND = 10.0F;
	/** The helm with its crest is drawn this much smaller than modelled, shrunk toward the neck. */
	private static final float HEAD_SCALE = 0.8F;
	/** The top of the chainmail panels, hidden under the skirt of plates. */
	private static final float MAIL_TOP = 10.0F;
	/** Share of the knee sink kept while he just walks; more makes him bob up and down. */
	private static final float WALK_SINK = 1.0F / 6.0F;

	private final ModelPart rightForearm;
	private final ModelPart leftForearm;
	private final ModelPart rightShin;
	private final ModelPart leftShin;
	private final ModelPart crest;
	private final ModelPart mailFront;
	private final ModelPart mailBack;
	private final ModelPart mailRight;
	private final ModelPart mailLeft;

	public PillagerKingModel(ModelPart root) {
		super(root);
		rightForearm = rightArm.getChild("right_forearm");
		leftForearm = leftArm.getChild("left_forearm");
		rightShin = rightLeg.getChild("right_shin");
		leftShin = leftLeg.getChild("left_shin");
		crest = head.getChild("crest");
		mailFront = body.getChild("mail_front");
		mailBack = body.getChild("mail_back");
		mailRight = body.getChild("mail_right");
		mailLeft = body.getChild("mail_left");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		CubeDeformation plate = new CubeDeformation(0.25F);
		// A Corinthian-style helm: the shell, its dome, the brow, the nasal, two cheek guards with the mouth slot between
		// them, and the flare over the nape.
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-5.0F, -10.0F, -5.0F, 10.0F, 10.0F, 10.0F)
				.texOffs(40, 0).addBox(-4.0F, -11.0F, -4.0F, 8.0F, 1.0F, 8.0F)
				.texOffs(72, 0).addBox(-5.5F, -1.5F, 0.5F, 11.0F, 2.0F, 5.0F)
				.texOffs(72, 8).addBox(-5.5F, -8.0F, -6.0F, 11.0F, 2.0F, 1.0F)
				.texOffs(96, 8).addBox(-1.0F, -8.0F, -6.5F, 2.0F, 4.0F, 1.0F)
				.texOffs(40, 10).addBox(-5.5F, -5.0F, -6.0F, 4.0F, 6.0F, 2.0F)
				.texOffs(52, 10).addBox(1.5F, -5.0F, -6.0F, 4.0F, 6.0F, 2.0F), PartPose.ZERO.withScale(HEAD_SCALE));
		head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
		// The crest: a ridge on the dome holding a plume that rises toward the back and hangs down behind.
		head.addOrReplaceChild("crest", CubeListBuilder.create()
				.texOffs(104, 0).addBox(-0.5F, 0.0F, -4.5F, 1.0F, 1.0F, 9.0F)
				.texOffs(0, 20).addBox(-1.0F, -4.0F, -6.0F, 2.0F, 4.0F, 12.0F)
				.texOffs(28, 20).addBox(-1.0F, -6.0F, -3.0F, 2.0F, 2.0F, 9.0F)
				.texOffs(50, 20).addBox(-1.0F, -4.0F, 6.0F, 2.0F, 6.0F, 2.0F), PartPose.offset(0.0F, -12.0F, 0.0F));
		// Breastplate and the skirt of plates over the hips.
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(0, 36).addBox(-4.5F, 0.0F, -2.5F, 9.0F, 12.0F, 5.0F)
				.texOffs(28, 36).addBox(-5.5F, 10.0F, -3.5F, 11.0F, 4.0F, 7.0F), PartPose.ZERO);
		// Chainmail hanging from under the plates to the knees, one panel on each side of the hips.
		body.addOrReplaceChild("mail_front", CubeListBuilder.create().texOffs(64, 20).addBox(-5.0F, 0.0F, -0.5F, 10.0F, 9.0F, 1.0F),
				PartPose.offset(0.0F, MAIL_TOP, -2.6F));
		body.addOrReplaceChild("mail_back", CubeListBuilder.create().texOffs(86, 20).addBox(-5.0F, 0.0F, -0.5F, 10.0F, 9.0F, 1.0F),
				PartPose.offset(0.0F, MAIL_TOP, 2.6F));
		body.addOrReplaceChild("mail_right", CubeListBuilder.create().texOffs(64, 30).addBox(-0.5F, 0.0F, -3.0F, 1.0F, 9.0F, 6.0F),
				PartPose.offset(-4.9F, MAIL_TOP, 0.0F));
		body.addOrReplaceChild("mail_left", CubeListBuilder.create().texOffs(78, 30).addBox(-0.5F, 0.0F, -3.0F, 1.0F, 9.0F, 6.0F),
				PartPose.offset(4.9F, MAIL_TOP, 0.0F));
		// Thin plated arms under big pauldrons, a gauntlet cuff at the wrist; they bend at the elbow.
		root.addOrReplaceChild("right_arm", CubeListBuilder.create()
				.texOffs(0, 56).addBox(-2.0F, -2.0F, -1.5F, 3.0F, 6.0F, 3.0F, plate)
				.texOffs(24, 56).addBox(-3.0F, -3.0F, -2.5F, 5.0F, 4.0F, 5.0F), PartPose.offset(-5.5F, 2.0F, 0.0F))
				.addOrReplaceChild("right_forearm", CubeListBuilder.create()
						.texOffs(12, 56).addBox(-2.0F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F, plate)
						.texOffs(44, 56).addBox(-2.5F, 3.0F, -2.0F, 4.0F, 3.0F, 4.0F), PartPose.offset(0.0F, ELBOW, 0.0F));
		root.addOrReplaceChild("left_arm", CubeListBuilder.create().mirror()
				.texOffs(0, 56).addBox(-1.0F, -2.0F, -1.5F, 3.0F, 6.0F, 3.0F, plate)
				.texOffs(24, 56).addBox(-2.0F, -3.0F, -2.5F, 5.0F, 4.0F, 5.0F), PartPose.offset(5.5F, 2.0F, 0.0F))
				.addOrReplaceChild("left_forearm", CubeListBuilder.create().mirror()
						.texOffs(12, 56).addBox(-1.0F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F, plate)
						.texOffs(44, 56).addBox(-1.5F, 3.0F, -2.0F, 4.0F, 3.0F, 4.0F), PartPose.offset(0.0F, ELBOW, 0.0F));
		// Plated legs bending at the knee, a knee guard on top of each shin.
		root.addOrReplaceChild("right_leg", CubeListBuilder.create()
				.texOffs(0, 66).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, plate), PartPose.offset(-2.0F, 12.0F, 0.0F))
				.addOrReplaceChild("right_shin", CubeListBuilder.create()
						.texOffs(16, 66).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, plate)
						.texOffs(32, 66).addBox(-2.0F, -1.0F, -3.0F, 4.0F, 3.0F, 1.0F), PartPose.offset(0.0F, KNEE, 0.0F));
		root.addOrReplaceChild("left_leg", CubeListBuilder.create().mirror()
				.texOffs(0, 66).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, plate), PartPose.offset(2.0F, 12.0F, 0.0F))
				.addOrReplaceChild("left_shin", CubeListBuilder.create().mirror()
						.texOffs(16, 66).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, plate)
						.texOffs(32, 66).addBox(-2.0F, -1.0F, -3.0F, 4.0F, 3.0F, 1.0F), PartPose.offset(0.0F, KNEE, 0.0F));
		return LayerDefinition.create(mesh, 128, 128);
	}

	@Override
	public void setupAnim(PillagerKingRenderState state) {
		super.setupAnim(state);
		walk(state);
		float t = state.actionTime;
		float restR = rightArm.xRot;
		float restL = leftArm.xRot;
		float restRz = rightArm.zRot;
		float restLz = leftArm.zRot;
		float restFr = rightForearm.xRot;
		float restFl = leftForearm.xRot;
		switch (state.action) {
			case SLASH -> {
				// Raised high with the axe cocked behind the head, then one cut down across the body.
				float[] times = {0, 5, 7, 10, 14};
				rightArm.xRot = keys(t, times, new float[] {restR, -2.8F, -0.3F, -0.3F, restR});
				rightArm.yRot = keys(t, times, new float[] {0.0F, -0.3F, 0.45F, 0.3F, 0.0F});
				rightForearm.xRot = keys(t, times, new float[] {restFr, -1.2F, -0.1F, -0.2F, restFr});
				body.yRot = keys(t, times, new float[] {0.0F, -0.25F, 0.35F, 0.3F, 0.0F});
				lunge(keys(t, times, new float[] {0.0F, 0.3F, 1.0F, 0.8F, 0.0F}), true);
			}
			case COMBO -> {
				// Both axes up, then four cuts, right and left in turn.
				float[] right = {0, 7, 8, 10, 13, 20, 22, 25, 32};
				float[] left = {0, 7, 14, 16, 19, 26, 28, 31, 34};
				rightArm.xRot = keys(t, right, new float[] {restR, -2.6F, -2.6F, -0.2F, -0.6F, -2.6F, -0.2F, -0.4F, restR});
				leftArm.xRot = keys(t, left, new float[] {restL, -2.6F, -2.6F, -0.2F, -0.6F, -2.6F, -0.2F, -0.4F, restL});
				rightForearm.xRot = keys(t, right, new float[] {restFr, -1.1F, -1.1F, -0.1F, -0.3F, -1.1F, -0.1F, -0.3F, restFr});
				leftForearm.xRot = keys(t, left, new float[] {restFl, -1.1F, -1.1F, -0.1F, -0.3F, -1.1F, -0.1F, -0.3F, restFl});
				body.yRot = keys(t, new float[] {0, 8, 10, 16, 22, 28, 34}, new float[] {0.0F, 0.0F, 0.3F, -0.3F, 0.3F, -0.3F, 0.0F});
				rightArm.yRot = body.yRot;
				leftArm.yRot = body.yRot;
				// Steps into each cut with the foot on its side.
				float step = keys(t, new float[] {0, 8, 10, 16, 22, 28, 34}, new float[] {0.0F, 0.0F, 1.0F, -1.0F, 1.0F, -1.0F, 0.0F});
				lunge(Math.abs(step) * 0.7F, step >= 0.0F);
			}
			case ONSLAUGHT -> onslaught(t, restR, restL, restFr, restFl);
			case CHARGE_WINDUP -> {
				// Crouched, both axes drawn back low, the right foot forward: about to run.
				float set = keys(t, new float[] {0, 5, 14}, new float[] {0.0F, 1.0F, 1.0F});
				rightArm.xRot = Mth.lerp(set, restR, 0.8F);
				leftArm.xRot = Mth.lerp(set, restL, 0.8F);
				rightForearm.xRot = Mth.lerp(set, restFr, -0.7F);
				leftForearm.xRot = Mth.lerp(set, restFl, -0.7F);
				body.xRot = 0.3F * set;
				head.xRot -= 0.25F * set;
				bendLeg(rightLeg, rightShin, set, -0.55F, 0.9F);
				bendLeg(leftLeg, leftShin, set, 0.3F, 0.7F);
			}
			case CHARGE -> {
				// Running bent forward, the axes held back, legs and arms pumping.
				float stride = Mth.sin(t * 1.3F);
				body.xRot = 0.35F;
				head.xRot -= 0.3F;
				rightArm.xRot = 0.5F + 0.6F * stride;
				leftArm.xRot = 0.5F - 0.6F * stride;
				rightForearm.xRot = -0.8F;
				leftForearm.xRot = -0.8F;
				bendLeg(rightLeg, rightShin, 1.0F, -0.9F * stride, 0.3F + 0.8F * Math.max(0.0F, stride));
				bendLeg(leftLeg, leftShin, 1.0F, 0.9F * stride, 0.3F + 0.8F * Math.max(0.0F, -stride));
			}
			case CHARGE_HITS -> {
				// Right, left, right, then a kick with the right foot.
				float[] right = {0, 1, 3, 5, 9, 11, 13, 22};
				rightArm.xRot = keys(t, right, new float[] {-2.0F, -2.6F, -0.3F, -0.6F, -2.6F, -0.3F, -0.5F, restR});
				rightForearm.xRot = keys(t, right, new float[] {-1.0F, -1.1F, -0.1F, -0.3F, -1.1F, -0.1F, -0.3F, restFr});
				float[] left = {0, 5, 7, 9, 22};
				leftArm.xRot = keys(t, left, new float[] {-1.0F, -2.6F, -0.3F, -0.5F, restL});
				leftForearm.xRot = keys(t, left, new float[] {-0.6F, -1.1F, -0.1F, -0.3F, restFl});
				body.yRot = keys(t, new float[] {0, 3, 7, 11, 15, 22}, new float[] {0.0F, 0.3F, -0.3F, 0.3F, 0.0F, 0.0F});
				rightArm.yRot = body.yRot;
				leftArm.yRot = body.yRot;
				kick(t, 14.0F, KingAction.CHARGE_KICK, 21.0F);
			}
			case BOOMERANG -> {
				// The left axe swung out flat to the side and slung forward in a curve.
				float[] times = {0, 6, 8, 10, 16};
				leftArm.xRot = keys(t, times, new float[] {restL, -1.2F, -1.4F, -1.3F, restL});
				leftArm.zRot = keys(t, times, new float[] {restLz, -1.2F, -0.3F, -0.2F, restLz});
				leftArm.yRot = keys(t, times, new float[] {0.0F, 0.9F, -0.6F, -0.8F, 0.0F});
				leftForearm.xRot = keys(t, times, new float[] {restFl, -0.4F, -0.1F, -0.1F, restFl});
				body.yRot = keys(t, times, new float[] {0.0F, 0.5F, -0.35F, -0.4F, 0.0F});
			}
			case PARRY_ARROW -> {
				// A flick of the right axe across the body.
				float out = keys(t, new float[] {0, 1.5F, 3, 6}, new float[] {0.0F, 1.0F, 1.0F, 0.0F});
				float sweep = keys(t, new float[] {0, 0.5F, 2.5F, 6}, new float[] {0.0F, 0.0F, 1.0F, 1.0F});
				rightArm.xRot = Mth.lerp(out, restR, -1.0F);
				rightArm.yRot = Mth.lerp(out, 0.0F, Mth.lerp(sweep, 0.7F, -0.6F));
				rightForearm.xRot = Mth.lerp(out, restFr, -0.2F);
				body.yRot = Mth.lerp(sweep, 0.25F, -0.3F) * out;
			}
			case TORNADO -> {
				float out = keys(t, new float[] {0, 6, 38, 42}, new float[] {0.0F, 1.0F, 1.0F, 0.0F});
				armsOut(out, restR, restL, restRz, restLz, restFr, restFl);
				stance(out);
				// The axes tip up and down as he spins.
				rightArm.xRot += Mth.sin(t * 1.3F) * 0.25F * out;
				leftArm.xRot -= Mth.sin(t * 1.3F) * 0.25F * out;
			}
			case THROW -> {
				// The left axe drawn back over the shoulder and thrown.
				float[] times = {0, 6, 8, 10, 16};
				leftArm.xRot = keys(t, times, new float[] {restL, -2.9F, -1.3F, -0.9F, restL});
				leftForearm.xRot = keys(t, times, new float[] {restFl, -1.4F, -0.2F, -0.1F, restFl});
				body.yRot = keys(t, times, new float[] {0.0F, 0.45F, -0.35F, -0.3F, 0.0F});
				leftArm.yRot = body.yRot;
				lunge(keys(t, times, new float[] {0.0F, 0.2F, 1.0F, 0.8F, 0.0F}), false);
			}
			case STAGGER -> {
				// Thrown off balance: arms flung out, leaning back.
				float off = keys(t, new float[] {0, 3, 18, 24}, new float[] {0.0F, 1.0F, 1.0F, 0.0F});
				rightArm.xRot = Mth.lerp(off, restR, -1.1F);
				leftArm.xRot = Mth.lerp(off, restL, -0.9F);
				rightArm.zRot = Mth.lerp(off, restRz, 0.9F);
				leftArm.zRot = Mth.lerp(off, restLz, -0.9F);
				rightForearm.xRot = Mth.lerp(off, restFr, -0.6F);
				leftForearm.xRot = Mth.lerp(off, restFl, -0.6F);
				body.xRot = -0.2F * off;
				head.xRot -= 0.45F * off;
				bendLeg(rightLeg, rightShin, off, -0.35F, 0.5F);
			}
			case TOTEM -> {
				// The totem held high in the left hand.
				float up = keys(t, new float[] {0, 4, 24, 30}, new float[] {0.0F, 1.0F, 1.0F, 0.0F});
				leftArm.xRot = Mth.lerp(up, restL, -2.8F);
				leftArm.zRot = Mth.lerp(up, restLz, -0.2F);
				leftForearm.xRot = Mth.lerp(up, restFl, -0.2F);
				rightArm.zRot = Mth.lerp(up, restRz, 0.6F);
				head.xRot = Mth.lerp(up, head.xRot, -0.5F);
			}
			case NONE -> {
			}
		}
		// Bent knees would lift his feet off the ground: he sinks instead. Only a sixth as much while just walking, a slight bob.
		float sink = Math.min(footLift(rightLeg.xRot, rightShin.xRot), footLift(leftLeg.xRot, leftShin.xRot));
		root().y += sink * (state.action == KingAction.NONE ? WALK_SINK : 1.0F);
		mail(state);
		crest(state);
	}

	/** Knees fold as each leg swings forward; elbows stay a little bent, more as the arm swings forward. */
	private void walk(PillagerKingRenderState state) {
		float speed = Math.min(state.walkAnimationSpeed, 1.0F);
		float phase = state.walkAnimationPos * 0.6662F;
		rightShin.xRot = (0.1F + 1.1F * Math.max(0.0F, Mth.sin(phase))) * speed;
		leftShin.xRot = (0.1F + 1.1F * Math.max(0.0F, -Mth.sin(phase))) * speed;
		rightForearm.xRot = -0.25F - 0.6F * Math.max(0.0F, -rightArm.xRot);
		leftForearm.xRot = -0.25F - 0.6F * Math.max(0.0F, -leftArm.xRot);
	}

	/**
	 * The chainmail panels hang from under the plates. The thigh swinging forward pushes the front panel out and the one
	 * swinging back the back panel; moving, they trail behind and bounce with his steps; turning, they lag to the
	 * outside of the turn; falling, they lift; in his spins they flare out all round.
	 */
	private void mail(PillagerKingRenderState state) {
		float walk = Math.min(state.walkAnimationSpeed, 1.0F);
		float bounce = Mth.sin(state.walkAnimationPos * 0.6662F * 2.0F) * 0.07F * walk;
		float sway = Mth.sin(state.ageInTicks * 0.09F) * 0.025F;
		float trail = 0.22F * walk + 0.45F * state.dash;
		float flare = Math.clamp(state.spinSpeed / 40.0F, 0.0F, 1.1F) + Math.clamp(-state.fallSpeed * 1.5F, 0.0F, 0.6F);
		float forward = Math.max(0.0F, -Math.min(rightLeg.xRot, leftLeg.xRot));
		float back = Math.max(0.0F, Math.max(rightLeg.xRot, leftLeg.xRot));
		float turn = Math.clamp(state.turnRate / 25.0F, -1.0F, 1.0F) * 0.35F;
		mailFront.xRot = -0.8F * forward + trail + bounce + sway - flare;
		mailBack.xRot = 0.8F * back + trail * 0.6F + bounce - sway + flare;
		mailFront.zRot = -turn;
		mailBack.zRot = -turn;
		// The sides lift with the legs set apart, swing with the turn, and swing a little with each step.
		float step = Mth.sin(state.walkAnimationPos * 0.6662F) * 0.08F * walk;
		mailRight.zRot = 0.9F * Math.max(0.0F, rightLeg.zRot) + flare + Math.max(0.0F, turn) + step + sway;
		mailLeft.zRot = 0.9F * Math.min(0.0F, leftLeg.zRot) - flare + Math.min(0.0F, turn) + step - sway;
		mailRight.xRot = trail * 0.5F;
		mailLeft.xRot = trail * 0.5F;
	}

	/** The plume streams back as he moves and sways with his steps. */
	private void crest(PillagerKingRenderState state) {
		float walk = Math.min(state.walkAnimationSpeed, 1.0F);
		crest.xRot = 0.12F * walk + 0.2F * state.dash + Mth.sin(state.walkAnimationPos * 0.6662F * 2.0F) * 0.04F * walk;
		crest.zRot = -Math.clamp(state.turnRate / 25.0F, -1.0F, 1.0F) * 0.12F;
	}

	/** Steps into a blow with the foot on its side: the hip forward, the knee bent, the other leg back. */
	private void lunge(float amount, boolean right) {
		if (amount <= 0.0F) {
			return;
		}
		bendLeg(right ? rightLeg : leftLeg, right ? rightShin : leftShin, amount, -0.45F, 0.55F);
		bendLeg(right ? leftLeg : rightLeg, right ? leftShin : rightShin, amount, 0.3F, 0.15F);
		body.xRot += 0.12F * amount;
	}

	/** Bends a leg, blending from its current pose: {@code hip} below zero swings it forward, {@code knee} folds the shin back. */
	private static void bendLeg(ModelPart leg, ModelPart shin, float amount, float hip, float knee) {
		leg.xRot = Mth.lerp(amount, leg.xRot, hip);
		shin.xRot = Mth.lerp(amount, shin.xRot, knee);
	}

	/** How far a leg with this hip and knee bend lifts its foot, in model pixels. */
	private static float footLift(float hip, float knee) {
		return KNEE * (1.0F - Mth.cos(hip)) + SHIN * (1.0F - Mth.cos(hip + knee));
	}

	/** Puts the item at the end of the forearm, like the Fallen Lord's sword. */
	@Override
	public void translateToHand(HumanoidRenderState state, HumanoidArm arm, PoseStack poseStack) {
		root().translateAndRotate(poseStack);
		getArm(arm).translateAndRotate(poseStack);
		(arm == HumanoidArm.LEFT ? leftForearm : rightForearm).translateAndRotate(poseStack);
		poseStack.translate(0.0F, (HAND - ELBOW) / 16.0F, 0.0F);
	}

	/**
	 * The onslaught: a rush bent forward, a kick, a crouch and a leap with both axes high over the head, brought down
	 * together as he lands.
	 */
	private void onslaught(float t, float restR, float restL, float restFr, float restFl) {
		float rush = keys(t, new float[] {0, 2, 6, 8}, new float[] {0.0F, 1.0F, 1.0F, 0.0F});
		body.xRot = 0.3F * rush;
		kick(t, 5.0F, KingAction.ONSLAUGHT_KICK, 9.5F);
		float[] times = {0, 6, 10, 16, 18, 20, 24, 30};
		rightArm.xRot = keys(t, times, new float[] {restR, 0.6F, -2.9F, -3.0F, -3.0F, -0.4F, -0.4F, restR});
		leftArm.xRot = keys(t, times, new float[] {restL, 0.6F, -2.9F, -3.0F, -3.0F, -0.4F, -0.4F, restL});
		rightForearm.xRot = keys(t, times, new float[] {restFr, -0.6F, -1.1F, -1.2F, -1.2F, -0.1F, -0.2F, restFr});
		leftForearm.xRot = keys(t, times, new float[] {restFl, -0.6F, -1.1F, -1.2F, -1.2F, -0.1F, -0.2F, restFl});
		// Knees drawn up in the air, a deep bend as he lands and strikes.
		float tuck = keys(t, new float[] {0, 9, 11, 17, 20, 24, 30}, new float[] {0.0F, 0.6F, 1.0F, 1.0F, 0.8F, 0.8F, 0.0F});
		bendLeg(rightLeg, rightShin, tuck, -0.7F, 1.2F);
		bendLeg(leftLeg, leftShin, tuck, -0.4F, 1.0F);
		body.xRot += keys(t, new float[] {0, 18, 20, 24, 30}, new float[] {0.0F, -0.1F, 0.4F, 0.4F, 0.0F});
	}

	/** A kick with the right foot: drawn up from {@code start}, out on {@code hit}, back down by {@code end}. */
	private void kick(float t, float start, float hit, float end) {
		float out = keys(t, new float[] {0, start, hit, end}, new float[] {0.0F, 0.0F, 1.0F, 0.0F});
		float cock = keys(t, new float[] {0, start, hit - 1.5F, hit, end}, new float[] {0.0F, 0.0F, 1.0F, 0.0F, 0.0F});
		rightLeg.xRot = Mth.lerp(Math.max(out, cock), rightLeg.xRot, -1.35F);
		rightShin.xRot = Mth.lerp(Math.max(out, cock), rightShin.xRot, 1.3F * cock + 0.05F * out);
		body.xRot -= 0.2F * out;
	}

	/** Both arms raised straight out to the sides by {@code amount}, the axes pointing ahead. */
	private void armsOut(float amount, float restR, float restL, float restRz, float restLz, float restFr, float restFl) {
		rightForearm.xRot = Mth.lerp(amount, restFr, -0.15F);
		leftForearm.xRot = Mth.lerp(amount, restFl, -0.15F);
		rightArm.xRot = Mth.lerp(amount, restR, 0.0F);
		leftArm.xRot = Mth.lerp(amount, restL, 0.0F);
		rightArm.zRot = Mth.lerp(amount, restRz, ARMS_OUT);
		leftArm.zRot = Mth.lerp(amount, restLz, -ARMS_OUT);
	}

	/** Feet set apart and knees bent by {@code amount}. */
	private void stance(float amount) {
		rightLeg.zRot += 0.2F * amount;
		leftLeg.zRot -= 0.2F * amount;
		bendLeg(rightLeg, rightShin, amount, -0.25F, 0.45F);
		bendLeg(leftLeg, leftShin, amount, -0.25F, 0.45F);
	}

	/** The value at {@code t} of a curve through the given keys, eased between them. */
	private static float keys(float t, float[] times, float[] values) {
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
