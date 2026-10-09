package io.github.hellenter.eam.client.mobs.fallenlord;

import io.github.hellenter.eam.Eam;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/** Short red flash of the eyes before the dash. */
public class FallenLordEyesLayer extends EyesLayer<FallenLordRenderState, FallenLordModel> {
	private static final RenderType EYES = RenderTypes.eyes(Eam.id("textures/entity/fallen_lord/fallen_lord_eyes.png"));

	public FallenLordEyesLayer(RenderLayerParent<FallenLordRenderState, FallenLordModel> renderer) {
		super(renderer);
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, FallenLordRenderState state, float yRot, float xRot) {
		if (state.eyesGlow) {
			super.submit(poseStack, collector, lightCoords, state, yRot, xRot);
		}
	}

	@Override
	public RenderType renderType() {
		return EYES;
	}
}
