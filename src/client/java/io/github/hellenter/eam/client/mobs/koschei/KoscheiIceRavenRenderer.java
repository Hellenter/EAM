package io.github.hellenter.eam.client.mobs.koschei;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.entity.koschei.KoscheiIceRaven;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

/**
 * A raven of ice: the phantom raven's shape in clear pale-blue ice with white glowing eyes. The one that knocks a shield
 * out is a little bigger, its ice darker and cracked, its eyes a deep glowing blue.
 */
public class KoscheiIceRavenRenderer extends MobRenderer<KoscheiIceRaven, KoscheiIceRavenRenderer.State, KoscheiRavenModel> {
	private static final Identifier TEXTURE = Eam.id("textures/entity/koschei/koschei_ice_raven.png");
	private static final Identifier BREAKER_TEXTURE = Eam.id("textures/entity/koschei/koschei_ice_raven_breaker.png");
	private static final RenderType EYES = RenderTypes.eyes(Eam.id("textures/entity/koschei/koschei_ice_raven_eyes.png"));
	private static final RenderType BREAKER_EYES = RenderTypes.eyes(Eam.id("textures/entity/koschei/koschei_ice_raven_breaker_eyes.png"));
	/** Wing beats per tick, in radians: quicker than the phantoms, it flies straight in. */
	private static final float FLAP_SPEED = 1.4F;
	private static final float BREAKER_SCALE = 1.3F;

	public KoscheiIceRavenRenderer(EntityRendererProvider.Context context) {
		super(context, new KoscheiRavenModel(context.bakeLayer(KoscheiRavenModel.LAYER)), 0.2F);
		addLayer(new Eyes(this));
	}

	@Override
	public Identifier getTextureLocation(State state) {
		return state.breaker ? BREAKER_TEXTURE : TEXTURE;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(KoscheiIceRaven entity, State state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.flap = state.ageInTicks * FLAP_SPEED + entity.getId() * 1.7F;
		state.breaker = entity.breaker();
	}

	@Override
	protected void scale(State state, PoseStack poseStack) {
		super.scale(state, poseStack);
		if (state.breaker) {
			poseStack.scale(BREAKER_SCALE, BREAKER_SCALE, BREAKER_SCALE);
		}
	}

	@Override
	protected void setupRotations(State state, PoseStack poseStack, float bodyRot, float entityScale) {
		super.setupRotations(state, poseStack, bodyRot, entityScale);
		poseStack.rotateDegrees(Axis.XP, state.xRot);
	}

	@Override
	protected int getModelTint(State state) {
		// Clear ice: mostly there, a little see-through.
		return ARGB.color(220, 0xFF, 0xFF, 0xFF);
	}

	public static class State extends KoscheiRavenRenderState {
		public boolean breaker;
	}

	private static final class Eyes extends RenderLayer<State, KoscheiRavenModel> {
		Eyes(RenderLayerParent<State, KoscheiRavenModel> renderer) {
			super(renderer);
		}

		@Override
		public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, State state, float yRot, float xRot) {
			collector.order(1).submitModel(getParentModel(), state, poseStack, state.breaker ? BREAKER_EYES : EYES, lightCoords,
					OverlayTexture.NO_OVERLAY, state.outlineColor);
		}
	}
}
