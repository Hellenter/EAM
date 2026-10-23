package io.github.hellenter.eam.client.mobs.shambler;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.entity.shambler.MushroomShambler;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * Both kinds of shambler on one model: the red- and brown-capped one of the mushroom forest and the charred one of the
 * burnt forest. Its eyes glow while it is up; a smoldering one's cracks glow whatever it does.
 */
public class ShamblerRenderer extends HumanoidMobRenderer<MushroomShambler, ShamblerRenderState, ShamblerModel> {
	private static final Identifier TEXTURE = Eam.id("textures/entity/mushroom_shambler/mushroom_shambler.png");
	private static final Identifier SMOLDERING_TEXTURE = Eam.id("textures/entity/mushroom_shambler/smoldering_shambler.png");
	private static final RenderType EYES = RenderTypes.eyes(Eam.id("textures/entity/mushroom_shambler/mushroom_shambler_eyes.png"));
	private static final RenderType SMOLDERING_EYES = RenderTypes.eyes(Eam.id("textures/entity/mushroom_shambler/smoldering_shambler_eyes.png"));
	private static final RenderType EMBERS = RenderTypes.eyes(Eam.id("textures/entity/mushroom_shambler/smoldering_shambler_embers.png"));

	public ShamblerRenderer(EntityRendererProvider.Context context) {
		super(context, new ShamblerModel(context.bakeLayer(ShamblerModel.LAYER)), 0.5F);
		addLayer(new Glow(this));
	}

	@Override
	public Identifier getTextureLocation(ShamblerRenderState state) {
		return state.smoldering ? SMOLDERING_TEXTURE : TEXTURE;
	}

	@Override
	public ShamblerRenderState createRenderState() {
		return new ShamblerRenderState();
	}

	@Override
	public void extractRenderState(MushroomShambler shambler, ShamblerRenderState state, float partialTicks) {
		super.extractRenderState(shambler, state, partialTicks);
		state.disguise = shambler.disguiseAmount(partialTicks);
		state.smoldering = shambler.smoldering();
		state.isAggressive = shambler.isAggressive();
	}

	/** The eyes, lit while it is up, and a smoldering one's glowing cracks. */
	private static final class Glow extends RenderLayer<ShamblerRenderState, ShamblerModel> {
		Glow(RenderLayerParent<ShamblerRenderState, ShamblerModel> renderer) {
			super(renderer);
		}

		@Override
		public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, ShamblerRenderState state, float yRot,
				float xRot) {
			if (state.smoldering) {
				collector.order(1).submitModel(getParentModel(), state, poseStack, EMBERS, lightCoords, OverlayTexture.NO_OVERLAY,
						state.outlineColor);
			}
			if (state.disguise < 0.5F) {
				collector.order(2).submitModel(getParentModel(), state, poseStack, state.smoldering ? SMOLDERING_EYES : EYES, lightCoords,
						OverlayTexture.NO_OVERLAY, state.outlineColor);
			}
		}
	}
}
