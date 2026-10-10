package io.github.hellenter.eam.client.mobs.knight;

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
 * The knight's armor: the Fallen Lord's gear model with an ancient metal texture, a closed great helm instead of his
 * open barbute and a dark cape.
 */
public class SkeletonKnightGearLayer extends RenderLayer<FallenLordRenderState, FallenLordModel> {
	private static final Identifier TEXTURE = Eam.id("textures/entity/skeleton_knight/skeleton_knight_gear.png");

	private final FallenLordGearModel model;

	public SkeletonKnightGearLayer(RenderLayerParent<FallenLordRenderState, FallenLordModel> renderer, EntityModelSet models) {
		super(renderer);
		model = new FallenLordGearModel(models.bakeLayer(FallenLordGearModel.LAYER));
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, FallenLordRenderState state, float yRot, float xRot) {
		coloredCutoutModelCopyLayerRender(model, TEXTURE, poseStack, collector, lightCoords, state, -1, 1);
	}
}
