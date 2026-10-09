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
 * The red flash of his eyes before the dash: a small glint on each eye, in front of the helmet, one of {@link #FRAMES}
 * frames at a time. {@code fallen_lord_eyes.png} holds the frames 4x8 texels each, 8 texels apart; the glint's centre
 * is texel (1, 4) of its frame.
 */
public class FallenLordEyesModel extends FallenLordModel {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Eam.id("fallen_lord"), "eyes");
	public static final int FRAMES = 8;

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
		// Each frame is shrunk to 3x6 px (a texel is 0.75 px) and placed so its glint sits on an eye: the eyes are at
		// x -2 and 2, y -3.5, four pixels apart, so the two glints never meet.
		CubeDeformation shrink = new CubeDeformation(-0.5F, -1.0F, 0.0F);
		for (int i = 0; i < FRAMES; i++) {
			head.addOrReplaceChild("frame" + i, CubeListBuilder.create().texOffs(8 * i, 0)
					.addBox(-3.625F, -7.875F, -5.1F, 4.0F, 8.0F, 0.0F, shrink)
					.addBox(0.375F, -7.875F, -5.1F, 4.0F, 8.0F, 0.0F, shrink), PartPose.ZERO);
		}
		root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
		addEmptyLimbs(root);
		return LayerDefinition.create(mesh, 64, 8).apply(MeshTransformer.scaling(MODEL_SCALE));
	}

	@Override
	public void setupAnim(FallenLordRenderState state) {
		super.setupAnim(state);
		for (int i = 0; i < FRAMES; i++) {
			frames[i].visible = i == state.eyeFrame;
		}
	}
}
