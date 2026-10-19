package io.github.hellenter.eam.client.mobs.leshy;

import io.github.hellenter.eam.Eam;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** A gnarled root spike, thick at the ground and twisting to a point, with two rootlets clawing off its foot. */
public class LeshyRootModel extends EntityModel<LeshyRootRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Eam.id("leshy_root"), "main");

	private final ModelPart mid;
	private final ModelPart tip;

	public LeshyRootModel(ModelPart root) {
		super(root);
		ModelPart base = root.getChild("base");
		mid = base.getChild("mid");
		tip = mid.getChild("tip");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition base = root.addOrReplaceChild("base", CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -4.0F, -2.5F, 5.0F, 4.0F, 5.0F),
				PartPose.offset(0.0F, 24.0F, 0.0F));
		base.addOrReplaceChild("rootlet_a", CubeListBuilder.create().texOffs(24, 9).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 5.0F, 1.0F),
				PartPose.offsetAndRotation(2.0F, -1.0F, 1.0F, 0.4F, 0.0F, -0.9F));
		base.addOrReplaceChild("rootlet_b", CubeListBuilder.create().texOffs(24, 9).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 5.0F, 1.0F),
				PartPose.offsetAndRotation(-2.0F, -1.0F, -1.0F, -0.5F, 0.0F, 0.8F));
		PartDefinition mid = base.addOrReplaceChild("mid", CubeListBuilder.create().texOffs(0, 9).addBox(-1.5F, -7.0F, -1.5F, 3.0F, 7.0F, 3.0F),
				PartPose.offsetAndRotation(0.0F, -4.0F, 0.0F, 0.15F, 0.0F, 0.1F));
		PartDefinition tip = mid.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(12, 9).addBox(-1.0F, -6.0F, -1.0F, 2.0F, 6.0F, 2.0F),
				PartPose.offsetAndRotation(0.0F, -7.0F, 0.0F, -0.25F, 0.0F, -0.15F));
		tip.addOrReplaceChild("point", CubeListBuilder.create().texOffs(20, 9).addBox(-0.5F, -4.0F, -0.5F, 1.0F, 4.0F, 1.0F),
				PartPose.offsetAndRotation(0.0F, -6.0F, 0.0F, 0.2F, 0.0F, 0.2F));
		return LayerDefinition.create(mesh, 32, 32);
	}

	@Override
	public void setupAnim(LeshyRootRenderState state) {
		super.setupAnim(state);
		// It writhes a little while it stands.
		float writhe = Mth.sin(state.upTime * 0.7F) * 0.08F;
		mid.xRot = 0.15F + writhe;
		tip.zRot = -0.15F - writhe * 1.5F;
	}
}
