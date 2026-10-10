package io.github.hellenter.eam.client.mobs.wight;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.entity.wight.Wight;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

/**
 * The wight. Invisible, it is not quite gone: up close its outline shimmers faintly in the air, like heat haze, and
 * fades out within a few more blocks.
 */
public class WightRenderer extends MobRenderer<Wight, WightRenderState, WightModel> {
	private static final Identifier TEXTURE = Eam.id("textures/entity/wight/wight.png");
	/** The shimmer is at its strongest this close, in blocks, and gone this far. */
	private static final double SHIMMER_NEAR = 5.0;
	private static final double SHIMMER_FAR = 12.0;

	public WightRenderer(EntityRendererProvider.Context context) {
		super(context, new WightModel(context.bakeLayer(WightModel.LAYER)), 0.45F);
	}

	@Override
	public Identifier getTextureLocation(WightRenderState state) {
		return TEXTURE;
	}

	@Override
	public WightRenderState createRenderState() {
		return new WightRenderState();
	}

	@Override
	public void extractRenderState(Wight entity, WightRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.swing = entity.getSwingAnimation(partialTicks);
		double distance = Math.sqrt(state.distanceToCameraSq);
		float fade = (float) Mth.clamp((SHIMMER_FAR - distance) / (SHIMMER_FAR - SHIMMER_NEAR), 0.0, 1.0);
		// A slow flicker, never steady enough to make out.
		float flicker = 0.55F + 0.45F * Mth.sin(state.ageInTicks * 0.31F) * Mth.sin(state.ageInTicks * 0.13F + 1.7F);
		// Spectators see it the vanilla way; everyone else only as the shimmer.
		state.shimmer = state.isInvisible && state.isInvisibleToPlayer ? fade * flicker : 0.0F;
		if (state.shimmer > 0.0F) {
			// Drawn through the translucent path vanilla uses for invisible teammates, at a fraction of its alpha.
			state.isInvisibleToPlayer = false;
		}
	}

	@Override
	protected int getModelTint(WightRenderState state) {
		if (state.shimmer <= 0.0F) {
			return -1;
		}
		// Vanilla draws it at 15% alpha; the shimmer is at most two fifths of that, a pale grey haze.
		return ARGB.color(Mth.floor(255.0F * state.shimmer * 0.4F), 0xD8, 0xDC, 0xD0);
	}
}
