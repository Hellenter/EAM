package io.github.hellenter.eam.client.mobs.guard;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.entity.guard.GuardAction;
import io.github.hellenter.eam.mobs.entity.king.KingAction;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.monster.illager.IllagerModel;
import net.minecraft.util.Mth;

/**
 * A pillager in a round brimmed helmet (a kettle hat) and an iron cuirass: the vanilla illager body with the helmet on
 * the head and raised breast and back plates on the body. The texture is twice as wide as the pillager's: its left half
 * is the pillager's layout, the right half holds the helmet and the plates. Poses his onslaught, throw and stagger.
 */
public class PillagerGuardModel extends IllagerModel<PillagerGuardRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Eam.id("pillager_guard"), "main");

	private final ModelPart head;
	private final ModelPart body;
	private final ModelPart arms;
	private final ModelPart rightArm;
	private final ModelPart leftArm;
	private final ModelPart rightLeg;
	private final ModelPart leftLeg;

	public PillagerGuardModel(ModelPart root) {
		super(root);
		head = root.getChild("head");
		body = root.getChild("body");
		arms = root.getChild("arms");
		rightArm = root.getChild("right_arm");
		leftArm = root.getChild("left_arm");
		rightLeg = root.getChild("right_leg");
		leftLeg = root.getChild("left_leg");
	}

	/** The vanilla illager mesh ({@link IllagerModel#createBodyLayer()}) plus the helmet and the cuirass's plates. */
	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition head = root.addOrReplaceChild("head",
				CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F), PartPose.ZERO);
		head.addOrReplaceChild("hat",
				CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 12.0F, 8.0F, new CubeDeformation(0.45F)), PartPose.ZERO);
		head.addOrReplaceChild("nose",
				CubeListBuilder.create().texOffs(24, 0).addBox(-1.0F, -1.0F, -6.0F, 2.0F, 4.0F, 2.0F), PartPose.offset(0.0F, -2.0F, 0.0F));
		// The kettle hat: a dome down to the brow, a flat round top and a wide brim just above the eyes.
		head.addOrReplaceChild("helmet", CubeListBuilder.create()
				.texOffs(64, 0).addBox(-4.5F, -11.0F, -4.5F, 9.0F, 4.0F, 9.0F)
				.texOffs(64, 13).addBox(-3.5F, -12.0F, -3.5F, 7.0F, 1.0F, 7.0F)
				.texOffs(64, 22).addBox(-6.5F, -7.0F, -6.5F, 13.0F, 1.0F, 13.0F), PartPose.ZERO);
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(16, 20).addBox(-4.0F, 0.0F, -3.0F, 8.0F, 12.0F, 6.0F)
				.texOffs(0, 38).addBox(-4.0F, 0.0F, -3.0F, 8.0F, 20.0F, 6.0F, new CubeDeformation(0.5F)), PartPose.ZERO);
		// The cuirass is painted on the body; these plates stand out of it in front and behind.
		body.addOrReplaceChild("cuirass", CubeListBuilder.create()
				.texOffs(104, 0).addBox(-3.5F, 0.5F, -3.75F, 7.0F, 9.0F, 1.0F)
				.texOffs(104, 10).addBox(-3.5F, 0.5F, 2.75F, 7.0F, 9.0F, 1.0F), PartPose.ZERO);
		PartDefinition arms = root.addOrReplaceChild("arms", CubeListBuilder.create()
				.texOffs(44, 22).addBox(-8.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F)
				.texOffs(40, 38).addBox(-4.0F, 2.0F, -2.0F, 8.0F, 4.0F, 4.0F),
				PartPose.offsetAndRotation(0.0F, 3.0F, -1.0F, -0.75F, 0.0F, 0.0F));
		arms.addOrReplaceChild("left_shoulder",
				CubeListBuilder.create().texOffs(44, 22).mirror().addBox(4.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F), PartPose.ZERO);
		root.addOrReplaceChild("right_leg",
				CubeListBuilder.create().texOffs(0, 22).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(-2.0F, 12.0F, 0.0F));
		root.addOrReplaceChild("left_leg",
				CubeListBuilder.create().texOffs(0, 22).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(2.0F, 12.0F, 0.0F));
		root.addOrReplaceChild("right_arm",
				CubeListBuilder.create().texOffs(40, 46).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(-5.0F, 2.0F, 0.0F));
		root.addOrReplaceChild("left_arm",
				CubeListBuilder.create().texOffs(40, 46).mirror().addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(5.0F, 2.0F, 0.0F));
		return LayerDefinition.create(mesh, 128, 64);
	}

	@Override
	public void setupAnim(PillagerGuardRenderState state) {
		super.setupAnim(state);
		if (state.action == GuardAction.NONE) {
			return;
		}
		arms.visible = false;
		rightArm.visible = true;
		leftArm.visible = true;
		float t = state.actionTime;
		switch (state.action) {
			case ONSLAUGHT -> onslaught(t);
			case THROW -> axeThrow(t);
			case STAGGER -> stagger(t);
			default -> {
			}
		}
	}

	/** Axe up as he rushes, a crouch, both hands on the haft over his head in the leap, and the blow down. */
	private void onslaught(float t) {
		float leap = KingAction.ONSLAUGHT_LEAP;
		float hit = KingAction.ONSLAUGHT_HIT;
		float crouchStart = KingAction.ONSLAUGHT_RUSH_END;
		float raise;
		if (t < crouchStart) {
			raise = Mth.lerp(t / crouchStart, 0.6F, -2.4F);
		} else if (t < hit) {
			raise = -2.9F;
		} else {
			// The blow: down in two ticks, then back up slowly.
			raise = Mth.lerp(Math.min((t - hit) / 2.0F, 1.0F), -2.9F, -0.3F) + Math.max(0.0F, t - hit - 4.0F) * -0.05F;
		}
		rightArm.xRot = raise;
		rightArm.yRot = 0.0F;
		rightArm.zRot = 0.0F;
		if (t >= crouchStart) {
			// The left hand joins the right on the haft.
			leftArm.xRot = raise;
			leftArm.yRot = 0.0F;
			leftArm.zRot = -0.35F;
			rightArm.zRot = 0.2F;
		}
		float crouch = t >= crouchStart && t < leap ? Mth.sin((t - crouchStart) / (leap - crouchStart) * Mth.PI) : 0.0F;
		boolean airborne = t >= leap && t < hit;
		root().y += crouch * 2.0F;
		body.xRot = crouch * 0.35F + (t >= hit ? 0.3F : 0.0F);
		if (crouch > 0.0F) {
			rightLeg.xRot = -crouch * 0.6F;
			leftLeg.xRot = -crouch * 0.6F;
		} else if (airborne) {
			rightLeg.xRot = -0.7F;
			leftLeg.xRot = 0.35F;
		}
	}

	/** The axe drawn back over the shoulder, then the arm swings through as it flies. */
	private void axeThrow(float t) {
		float release = GuardAction.THROW_RELEASE;
		if (t < release) {
			rightArm.xRot = Mth.lerp(Math.min(t / (release - 2.0F), 1.0F), -0.6F, -3.0F);
			rightArm.zRot = 0.25F;
		} else {
			rightArm.xRot = Mth.lerp(Math.min((t - release) / 2.0F, 1.0F), -3.0F, -0.5F);
			rightArm.zRot = 0.0F;
		}
		rightArm.yRot = 0.0F;
		leftArm.xRot = t < release ? -0.9F : -0.2F;
		leftArm.yRot = 0.0F;
		leftArm.zRot = -0.2F;
	}

	/** Knocked off balance: arms thrown out, head back. */
	private void stagger(float t) {
		float sway = Mth.sin(t * 0.5F) * 0.1F;
		rightArm.xRot = -0.5F;
		rightArm.zRot = 1.0F + sway;
		leftArm.xRot = -0.5F;
		leftArm.zRot = -1.0F - sway;
		head.xRot = -0.35F;
		body.xRot = -0.15F;
	}
}
