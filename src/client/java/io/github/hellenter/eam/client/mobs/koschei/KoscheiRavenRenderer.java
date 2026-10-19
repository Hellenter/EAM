package io.github.hellenter.eam.client.mobs.koschei;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.entity.koschei.KoscheiRaven;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

/** A phantom raven: a see-through black bird, its shape wavering a little, with pale glowing eyes. */
public class KoscheiRavenRenderer extends MobRenderer<KoscheiRaven, KoscheiRavenRenderState, KoscheiRavenModel> {
	private static final Identifier TEXTURE = Eam.id("textures/entity/koschei/koschei_raven.png");
	private static final RenderType EYES = RenderTypes.eyes(Eam.id("textures/entity/koschei/koschei_raven_eyes.png"));
	/** Wing beats per tick, in radians. */
	private static final float FLAP_SPEED = 1.1F;

	public KoscheiRavenRenderer(EntityRendererProvider.Context context) {
		super(context, new KoscheiRavenModel(context.bakeLayer(KoscheiRavenModel.LAYER)), 0.0F);
		addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return EYES;
			}
		});
	}

	@Override
	public Identifier getTextureLocation(KoscheiRavenRenderState state) {
		return TEXTURE;
	}

	@Override
	public KoscheiRavenRenderState createRenderState() {
		return new KoscheiRavenRenderState();
	}

	@Override
	public void extractRenderState(KoscheiRaven entity, KoscheiRavenRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.flap = state.ageInTicks * FLAP_SPEED + entity.getId() * 1.7F;
	}

	@Override
	protected void setupRotations(KoscheiRavenRenderState state, PoseStack poseStack, float bodyRot, float entityScale) {
		super.setupRotations(state, poseStack, bodyRot, entityScale);
		poseStack.rotateDegrees(Axis.XP, state.xRot);
	}

	@Override
	protected int getModelTint(KoscheiRavenRenderState state) {
		// Mostly there, wavering as a phantom does.
		float waver = 0.52F + 0.12F * Mth.sin(state.ageInTicks * 0.37F) * Mth.sin(state.ageInTicks * 0.11F + 2.1F);
		return ARGB.color(Mth.floor(255.0F * waver), 0xFF, 0xFF, 0xFF);
	}
}
