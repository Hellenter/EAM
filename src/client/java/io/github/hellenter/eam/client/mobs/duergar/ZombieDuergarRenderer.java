package io.github.hellenter.eam.client.mobs.duergar;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.entity.duergar.ZombieDuergar;

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

/** The zombie duergar, in one of three rotten skins; his clouded eyes glow a dull yellow in the dark of the mines. */
public class ZombieDuergarRenderer extends HumanoidMobRenderer<ZombieDuergar, DuergarRenderState, DuergarModel> {
	private static final Identifier[] TEXTURES = new Identifier[ZombieDuergar.SKINS];
	private static final RenderType EYES = RenderTypes.eyes(Eam.id("textures/entity/duergar/zombie_duergar_eyes.png"));

	static {
		for (int i = 0; i < TEXTURES.length; i++) {
			TEXTURES[i] = Eam.id("textures/entity/duergar/zombie_duergar_" + i + ".png");
		}
	}

	public ZombieDuergarRenderer(EntityRendererProvider.Context context) {
		super(context, new DuergarModel(context.bakeLayer(DuergarModel.LAYER)), 0.5F);
		addLayer(new Eyes(this));
	}

	@Override
	public Identifier getTextureLocation(DuergarRenderState state) {
		return TEXTURES[state.skin];
	}

	@Override
	public DuergarRenderState createRenderState() {
		return new DuergarRenderState();
	}

	@Override
	public void extractRenderState(ZombieDuergar duergar, DuergarRenderState state, float partialTicks) {
		super.extractRenderState(duergar, state, partialTicks);
		state.gear = duergar.gear();
		state.skin = duergar.skin();
	}

	/** The two eye points, lit whatever the light around. */
	private static final class Eyes extends RenderLayer<DuergarRenderState, DuergarModel> {
		Eyes(RenderLayerParent<DuergarRenderState, DuergarModel> renderer) {
			super(renderer);
		}

		@Override
		public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, DuergarRenderState state, float yRot,
				float xRot) {
			collector.order(1).submitModel(getParentModel(), state, poseStack, EYES, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
		}
	}
}
