package io.github.hellenter.eam.client.mobs.centurion;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.client.mobs.fallenlord.FallenLordModel;
import io.github.hellenter.eam.client.mobs.fallenlord.FallenLordRenderState;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.Identifier;

/** The centurion's battered plate, crest, bannered spear and horn ({@link LostCenturionGearModel}). */
public class LostCenturionGearLayer extends RenderLayer<FallenLordRenderState, FallenLordModel> {
	private static final Identifier TEXTURE = Eam.id("textures/entity/lost_centurion/lost_centurion_gear.png");

	private final LostCenturionGearModel model;

	public LostCenturionGearLayer(RenderLayerParent<FallenLordRenderState, FallenLordModel> renderer, EntityModelSet models) {
		super(renderer);
		model = new LostCenturionGearModel(models.bakeLayer(LostCenturionGearModel.LAYER));
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, FallenLordRenderState state, float yRot, float xRot) {
		coloredCutoutModelCopyLayerRender(model, TEXTURE, poseStack, collector, lightCoords, state, -1, 1);
	}
}
