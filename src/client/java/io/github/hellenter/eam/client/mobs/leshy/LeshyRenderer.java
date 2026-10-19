package io.github.hellenter.eam.client.mobs.leshy;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.entity.leshy.Leshy;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/** The leshy. Its eyes glow green deep in the sockets of the skull, whatever the light. */
public class LeshyRenderer extends MobRenderer<Leshy, LeshyRenderState, LeshyModel> {
	private static final Identifier TEXTURE = Eam.id("textures/entity/leshy/leshy.png");
	private static final RenderType EYES = RenderTypes.eyes(Eam.id("textures/entity/leshy/leshy_eyes.png"));

	public LeshyRenderer(EntityRendererProvider.Context context) {
		super(context, new LeshyModel(context.bakeLayer(LeshyModel.LAYER)), 0.8F);
		addLayer(new Eyes(this));
	}

	@Override
	public Identifier getTextureLocation(LeshyRenderState state) {
		return TEXTURE;
	}

	@Override
	public LeshyRenderState createRenderState() {
		return new LeshyRenderState();
	}

	@Override
	public void extractRenderState(Leshy entity, LeshyRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.swing = entity.getSwingAnimation(partialTicks);
		state.action = entity.action();
		state.actionTime = entity.actionAge() + partialTicks;
		state.angry = entity.isAngry();
	}

	private static final class Eyes extends RenderLayer<LeshyRenderState, LeshyModel> {
		Eyes(RenderLayerParent<LeshyRenderState, LeshyModel> renderer) {
			super(renderer);
		}

		@Override
		public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, LeshyRenderState state, float yRot,
				float xRot) {
			if (state.isInvisible) {
				return;
			}
			collector.order(1).submitModel(getParentModel(), state, poseStack, EYES, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
		}
	}
}
