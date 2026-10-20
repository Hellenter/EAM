package io.github.hellenter.eam.client.mobs.ghosthorse;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.entity.ghosthorse.GhostHorse;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.animal.equine.AbstractEquineModel;
import net.minecraft.client.model.animal.equine.HorseModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.AbstractHorseRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.renderer.entity.state.EquineRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;

/**
 * The ghost horse ({@link GhostHorseModel}): its hide stretched over ribs, spine and hips, a bare skull for a head. Its
 * eyes glow red whatever the light, and it wears horse armor shrunk to its thin frame.
 */
public class GhostHorseRenderer extends AbstractHorseRenderer<GhostHorse, EquineRenderState, AbstractEquineModel<EquineRenderState>> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Eam.id("ghost_horse"), "main");
	public static final ModelLayerLocation ARMOR_LAYER = new ModelLayerLocation(Eam.id("ghost_horse"), "armor");
	private static final Identifier TEXTURE = Eam.id("textures/entity/ghost_horse/ghost_horse.png");
	/** Its hitbox is 1.85 blocks tall where a horse's is 1.6. */
	private static final float SCALE = 1.85F / 1.6F;
	private static final RenderType EYES = RenderTypes.eyes(Eam.id("textures/entity/ghost_horse/ghost_horse_eyes.png"));

	public GhostHorseRenderer(EntityRendererProvider.Context context) {
		super(context, new GhostHorseModel(context.bakeLayer(LAYER)), new GhostHorseModel(context.bakeLayer(LAYER)));
		shadowRadius *= SCALE;
		addLayer(new Eyes(this));
		addLayer(new SimpleEquipmentLayer<>(this, context.getEquipmentRenderer(), EquipmentClientInfo.LayerType.HORSE_BODY,
				state -> state.bodyArmorItem, new HorseModel(context.bakeLayer(ARMOR_LAYER)), null, 2));
	}

	@Override
	public Identifier getTextureLocation(EquineRenderState state) {
		return TEXTURE;
	}

	/** A quarter block taller than a horse, armor and all. */
	@Override
	protected void scale(EquineRenderState state, PoseStack poseStack) {
		super.scale(state, poseStack);
		poseStack.scale(SCALE, SCALE, SCALE);
	}

	@Override
	public EquineRenderState createRenderState() {
		return new EquineRenderState();
	}

	/** The red eye points, lit whatever the light around. */
	private static final class Eyes extends RenderLayer<EquineRenderState, AbstractEquineModel<EquineRenderState>> {
		Eyes(RenderLayerParent<EquineRenderState, AbstractEquineModel<EquineRenderState>> renderer) {
			super(renderer);
		}

		@Override
		public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, EquineRenderState state, float yRot,
				float xRot) {
			collector.order(1).submitModel(getParentModel(), state, poseStack, EYES, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
		}
	}
}
