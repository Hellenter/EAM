package io.github.hellenter.eam.client.mobs.villager;

import io.github.hellenter.eam.Eam;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.VillagerLikeModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.npc.BabyVillagerModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

/**
 * The vanilla villager with his arms apart, hanging at his sides like a person's, so he walks swinging them and holds
 * items in his hands. Same texture layout as vanilla: each arm is the crossed-arms sleeve on top of a hand cut from the
 * crossed hands. Children keep their crossed arms (their hands hold nothing).
 */
public class VillagerFigureModel extends HumanoidModel<VillagerFigureRenderState> implements VillagerLikeModel<VillagerFigureRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Eam.id("villager"), "main");
	public static final ModelLayerLocation NO_HAT_LAYER = new ModelLayerLocation(Eam.id("villager"), "no_hat");
	public static final ModelLayerLocation BABY_LAYER = new ModelLayerLocation(Eam.id("villager_baby"), "main");
	public static final ModelLayerLocation BABY_NO_HAT_LAYER = new ModelLayerLocation(Eam.id("villager_baby"), "no_hat");

	public VillagerFigureModel(ModelPart root) {
		super(root);
	}

	public static LayerDefinition createLayer() {
		return LayerDefinition.create(createMesh(), 64, 64);
	}

	public static LayerDefinition createNoHatLayer() {
		MeshDefinition mesh = createMesh();
		mesh.getRoot().clearChild("head").clearRecursively();
		return LayerDefinition.create(mesh, 64, 64);
	}

	public static LayerDefinition createBabyLayer() {
		return LayerDefinition.create(createBabyMesh(), 64, 64);
	}

	public static LayerDefinition createBabyNoHatLayer() {
		MeshDefinition mesh = createBabyMesh();
		mesh.getRoot().clearChild("head").clearRecursively();
		return LayerDefinition.create(mesh, 64, 64);
	}

	private static MeshDefinition createMesh() {
		MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
		PartDefinition root = mesh.getRoot();
		PartDefinition head = root.addOrReplaceChild("head",
				CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F), PartPose.ZERO);
		PartDefinition hat = head.addOrReplaceChild("hat",
				CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F, new CubeDeformation(0.51F)), PartPose.ZERO);
		hat.addOrReplaceChild("hat_rim", CubeListBuilder.create().texOffs(30, 47).addBox(-8.0F, -8.0F, -6.0F, 16.0F, 16.0F, 1.0F),
				PartPose.rotation((float) (-Math.PI / 2), 0.0F, 0.0F));
		head.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(24, 0).addBox(-1.0F, -1.0F, -6.0F, 2.0F, 4.0F, 2.0F),
				PartPose.offset(0.0F, -2.0F, 0.0F));
		PartDefinition body = root.addOrReplaceChild("body",
				CubeListBuilder.create().texOffs(16, 20).addBox(-4.0F, 0.0F, -3.0F, 8.0F, 12.0F, 6.0F), PartPose.ZERO);
		body.addOrReplaceChild("jacket",
				CubeListBuilder.create().texOffs(0, 38).addBox(-4.0F, 0.0F, -3.0F, 8.0F, 20.0F, 6.0F, new CubeDeformation(0.5F)), PartPose.ZERO);
		// Sleeve: the 8-long crossed-arm box. Hand: a 4-cube of the crossed hands below it, so the arm is 12 long.
		root.addOrReplaceChild("right_arm", CubeListBuilder.create()
				.texOffs(44, 22).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F)
				.texOffs(44, 38).addBox(-3.0F, 6.0F, -2.0F, 4.0F, 4.0F, 4.0F), PartPose.offset(-5.0F, 2.0F, 0.0F));
		root.addOrReplaceChild("left_arm", CubeListBuilder.create().mirror()
				.texOffs(44, 22).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F)
				.texOffs(44, 38).addBox(-1.0F, 6.0F, -2.0F, 4.0F, 4.0F, 4.0F), PartPose.offset(5.0F, 2.0F, 0.0F));
		root.addOrReplaceChild("right_leg",
				CubeListBuilder.create().texOffs(0, 22).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(-2.0F, 12.0F, 0.0F));
		root.addOrReplaceChild("left_leg",
				CubeListBuilder.create().texOffs(0, 22).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.offset(2.0F, 12.0F, 0.0F));
		return mesh;
	}

	/** The vanilla child, with empty arm parts where its hands are so it fits the humanoid model. */
	private static MeshDefinition createBabyMesh() {
		MeshDefinition mesh = BabyVillagerModel.createBodyModel();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-2.0F, 17.5F, -2.0F));
		root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(2.0F, 17.5F, -2.0F));
		return mesh;
	}

	@Override
	public void setupAnim(VillagerFigureRenderState state) {
		super.setupAnim(state);
		if (state.isUnhappy) {
			head.zRot = 0.3F * Mth.sin(0.45F * state.ageInTicks);
			head.xRot = 0.4F;
		} else {
			head.zRot = 0.0F;
		}
	}

	@Override
	public void translateToArms(VillagerFigureRenderState state, PoseStack poseStack) {
		translateToHand(state, HumanoidArm.RIGHT, poseStack);
	}
}
