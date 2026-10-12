package io.github.hellenter.eam.client.mobs.yaga;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.entity.yaga.BabaYaga;
import io.github.hellenter.eam.mobs.entity.yaga.BabaYagaModule;
import io.github.hellenter.eam.mobs.entity.yaga.BabaYagaSettings;
import io.github.hellenter.eam.mobs.entity.yaga.YagaGlow;

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
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;

/**
 * Baba Yaga. Her eyes always glow a sickly green; the skull on her staff lights up in the colour of whatever she casts
 * (green as she raises her creatures); and while a hit would parry her she glows yellow from head to foot.
 */
public class BabaYagaRenderer extends MobRenderer<BabaYaga, BabaYagaRenderState, BabaYagaModel> {
	private static final Identifier TEXTURE = Eam.id("textures/entity/baba_yaga/baba_yaga.png");
	private static final RenderType EYES = RenderTypes.eyes(Eam.id("textures/entity/baba_yaga/baba_yaga_eyes.png"));
	private static final RenderType STAFF_GLOW = RenderTypes.entityTranslucentEmissive(Eam.id("textures/entity/baba_yaga/baba_yaga_glow.png"));
	private static final RenderType AURA = RenderTypes.entityTranslucentEmissive(Eam.id("textures/entity/baba_yaga/baba_yaga_aura.png"));

	public BabaYagaRenderer(EntityRendererProvider.Context context) {
		super(context, new BabaYagaModel(context.bakeLayer(BabaYagaModel.LAYER)), 0.6F);
		addLayer(new Glow(this));
	}

	@Override
	public Identifier getTextureLocation(BabaYagaRenderState state) {
		return TEXTURE;
	}

	@Override
	public BabaYagaRenderState createRenderState() {
		return new BabaYagaRenderState();
	}

	@Override
	public void extractRenderState(BabaYaga entity, BabaYagaRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.inMortar = entity.inMortar();
		state.action = entity.action();
		state.actionTime = entity.actionTime(partialTicks);
		state.glow = entity.glow();
		BabaYagaSettings settings = BabaYagaModule.settings();
		state.strikeTick = settings.swoop().strikeTick();
		state.lunge = settings.claws().lunge();
		state.clawGap = settings.claws().gap();
		state.clawHits = settings.claws().hits();
		state.beamWindup = settings.beam().windup();
	}

	/** The eyes, the staff's glow and the parry glow, all lit whatever the light around. */
	private static final class Glow extends RenderLayer<BabaYagaRenderState, BabaYagaModel> {
		Glow(RenderLayerParent<BabaYagaRenderState, BabaYagaModel> renderer) {
			super(renderer);
		}

		@Override
		public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, BabaYagaRenderState state, float yRot, float xRot) {
			BabaYagaModel model = getParentModel();
			collector.order(1).submitModel(model, state, poseStack, EYES, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
			YagaGlow glow = state.glow;
			if (glow == YagaGlow.NONE) {
				return;
			}
			float pulse = 0.5F + 0.5F * Mth.sin(state.ageInTicks * (glow == YagaGlow.PARRY ? 0.9F : 0.5F));
			if (glow == YagaGlow.PARRY) {
				int tint = ARGB.color(Mth.floor(255.0F * (0.45F + 0.3F * pulse)), glow.color);
				collector.order(2).submitModel(model, state, poseStack, AURA, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, tint,
						null, state.outlineColor);
			} else {
				int tint = ARGB.color(Mth.floor(255.0F * (0.75F + 0.25F * pulse)), glow.color);
				collector.order(2).submitModel(model, state, poseStack, STAFF_GLOW, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
						tint, null, state.outlineColor);
			}
		}
	}
}
