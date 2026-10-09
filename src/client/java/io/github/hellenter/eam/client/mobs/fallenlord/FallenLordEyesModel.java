package io.github.hellenter.eam.client.mobs.fallenlord;

import io.github.hellenter.eam.Eam;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The red flash of his eyes before the dash: a small sparkle on each eye, in front of the helmet, one of
 * {@link #FRAMES} frames at a time. {@code fallen_lord_eyes.png} holds the frames 5x5 texels each, 10 texels apart.
 */
public class FallenLordEyesModel extends FallenLordModel {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Eam.id("fallen_lord"), "eyes");
	public static final int FRAMES = 5;

	private final ModelPart[] frames = new ModelPart[FRAMES];

	public FallenLordEyesModel(ModelPart root) {
		super(root);
		for (int i = 0; i < FRAMES; i++) {
			frames[i] = head.getChild("frame" + i);
		}
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
		head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
		// Each sparkle is 5 texels shrunk to 3.75 px and centred on an eye; the eyes are 4 px apart, so they never meet.
		CubeDeformation shrink = new CubeDeformation(-0.625F, -0.625F, 0.0F);
		for (int i = 0; i < FRAMES; i++) {
			head.addOrReplaceChild("frame" + i, CubeListBuilder.create().texOffs(10 * i, 0)
					.addBox(-4.5F, -6.0F, -5.1F, 5.0F, 5.0F, 0.0F, shrink)
					.addBox(-0.5F, -6.0F, -5.1F, 5.0F, 5.0F, 0.0F, shrink), PartPose.ZERO);
		}
		for (String part : new String[] {"body", "right_arm", "left_arm", "right_leg", "left_leg"}) {
			root.addOrReplaceChild(part, CubeListBuilder.create(), PartPose.ZERO);
		}
		return LayerDefinition.create(mesh, 64, 8).apply(MeshTransformer.scaling(1.2F));
	}

	@Override
	public void setupAnim(FallenLordRenderState state) {
		super.setupAnim(state);
		for (int i = 0; i < FRAMES; i++) {
			frames[i].visible = i == state.eyeFrame;
		}
	}
}
