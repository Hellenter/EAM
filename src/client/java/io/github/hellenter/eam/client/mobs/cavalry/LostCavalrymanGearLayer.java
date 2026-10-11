package io.github.hellenter.eam.client.mobs.cavalry;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.client.mobs.fallenlord.FallenLordGearModel;
import io.github.hellenter.eam.client.mobs.fallenlord.FallenLordModel;
import io.github.hellenter.eam.client.mobs.fallenlord.FallenLordRenderState;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.Identifier;

/**
 * The heavy cavalryman's full plate in worn ancient metal, holed through to his dried-up body, with a closed great
 * helm, a faded red plume and a torn red cape.
 */
public class LostCavalrymanGearLayer extends RenderLayer<FallenLordRenderState, FallenLordModel> {
	private static final Identifier TEXTURE = Eam.id("textures/entity/lost_cavalry/lost_cavalryman_gear.png");

	private final LostCavalrymanGearModel model;

	public LostCavalrymanGearLayer(RenderLayerParent<FallenLordRenderState, FallenLordModel> renderer, EntityModelSet models) {
		super(renderer);
		model = new LostCavalrymanGearModel(models.bakeLayer(FallenLordGearModel.LAYER));
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, FallenLordRenderState state, float yRot, float xRot) {
		coloredCutoutModelCopyLayerRender(model, TEXTURE, poseStack, collector, lightCoords, state, -1, 1);
	}
}
