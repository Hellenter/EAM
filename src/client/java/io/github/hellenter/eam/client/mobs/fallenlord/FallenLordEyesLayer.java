package io.github.hellenter.eam.client.mobs.fallenlord;

import io.github.hellenter.eam.Eam;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Short red flash of the eyes before the dash, glowing like other mobs' eyes. */
public class FallenLordEyesLayer extends RenderLayer<FallenLordRenderState, FallenLordModel> {
	private static final RenderType EYES = RenderTypes.eyes(Eam.id("textures/entity/fallen_lord/fallen_lord_eyes.png"));

	private final FallenLordEyesModel model;

	public FallenLordEyesLayer(RenderLayerParent<FallenLordRenderState, FallenLordModel> renderer, EntityModelSet models) {
		super(renderer);
		model = new FallenLordEyesModel(models.bakeLayer(FallenLordEyesModel.LAYER));
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, FallenLordRenderState state, float yRot, float xRot) {
		if (state.eyeFrame >= 0) {
			collector.order(2).submitModel(model, state, poseStack, EYES, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
		}
	}
}
