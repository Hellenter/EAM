package io.github.hellenter.eam.client.mobs.fallenlord;

import io.github.hellenter.eam.Eam;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.Identifier;

/** His armor and cape, following his animation. */
public class FallenLordGearLayer extends RenderLayer<FallenLordRenderState, FallenLordModel> {
	private static final Identifier TEXTURE = Eam.id("textures/entity/fallen_lord/fallen_lord_gear.png");

	private final FallenLordGearModel model;

	public FallenLordGearLayer(RenderLayerParent<FallenLordRenderState, FallenLordModel> renderer, EntityModelSet models) {
		super(renderer);
		model = new FallenLordGearModel(models.bakeLayer(FallenLordGearModel.LAYER));
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, FallenLordRenderState state, float yRot, float xRot) {
		coloredCutoutModelCopyLayerRender(model, TEXTURE, poseStack, collector, lightCoords, state, -1, 1);
	}
}
