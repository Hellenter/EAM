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
 * The Pillager King's first look (#166 to #178), kept for later: closed black plate, a breastplate over a skirt of
 * plates with dark chainmail hanging from under it to the knees, big pauldrons on thin plated arms. The chainmail is four
 * hanging panels that the thighs push aside, trail behind as he moves, swing out as he turns and flare in his spins.
 * {@link PillagerKingLook#PLATE} draws him in it.
 */
public class PillagerKingPlateModel extends PillagerKingModel {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Eam.id("pillager_king"), "plate");
	/** The top of the chainmail panels, hidden under the skirt of plates. */
	private static final float MAIL_TOP = 10.0F;

	private final ModelPart mailFront;
	private final ModelPart mailBack;
	private final ModelPart mailRight;
	private final ModelPart mailLeft;

	public PillagerKingPlateModel(ModelPart root) {
		super(root);
		mailFront = body.getChild("mail_front");
		mailBack = body.getChild("mail_back");
		mailRight = body.getChild("mail_right");
		mailLeft = body.getChild("mail_left");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		CubeDeformation plate = new CubeDeformation(0.25F);
		addHelm(root);
		// Breastplate and the skirt of plates over the hips, ending where the chainmail begins.
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(0, 36).addBox(-4.5F, 0.0F, -2.5F, 9.0F, 12.0F, 5.0F)
				.texOffs(28, 36).addBox(-5.5F, MAIL_TOP - 4.0F, -3.5F, 11.0F, 4.0F, 7.0F), PartPose.ZERO);
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

	/**
	 * The chainmail panels hang from under the plates. The thigh swinging forward pushes the front panel out and the one
	 * swinging back the back panel; moving, they trail behind and bounce with his steps; turning, they lag to the
	 * outside of the turn; falling, they lift; in his spins they flare out all round.
	 */
	@Override
	protected void cloth(PillagerKingRenderState state) {
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
}
