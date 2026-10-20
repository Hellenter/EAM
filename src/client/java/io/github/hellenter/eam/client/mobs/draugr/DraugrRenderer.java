package io.github.hellenter.eam.client.mobs.draugr;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.entity.draugr.Draugr;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;

/** The draugr, a little taller than a man; his eyes glow a cold blue whatever the light */
public class DraugrRenderer extends HumanoidMobRenderer<Draugr, DraugrRenderState, DraugrModel> {
	/** The model is about 1.95 blocks tall; the draugr 2.1. */
	private static final float SCALE = 1.08F;
	private static final Identifier TEXTURE = Eam.id("textures/entity/draugr/draugr.png");
	private static final RenderType EYES = RenderTypes.eyes(Eam.id("textures/entity/draugr/draugr_eyes.png"));

	public DraugrRenderer(EntityRendererProvider.Context context) {
		super(context, new DraugrModel(context.bakeLayer(DraugrModel.LAYER)), 0.5F * SCALE);
		addLayer(new Eyes(this));
	}

	@Override
	public Identifier getTextureLocation(DraugrRenderState state) {
		return TEXTURE;
	}

	@Override
	public DraugrRenderState createRenderState() {
		return new DraugrRenderState();
	}

	@Override
	public void extractRenderState(Draugr draugr, DraugrRenderState state, float partialTicks) {
		super.extractRenderState(draugr, state, partialTicks);
		state.gear = draugr.gear();
		state.rise = draugr.riseProgress(partialTicks);
	}

	@Override
	protected void scale(DraugrRenderState state, PoseStack poseStack) {
		super.scale(state, poseStack);
		poseStack.scale(SCALE, SCALE, SCALE);
	}

	/** A shield he holds up, in a row of shields Koschei keeps him in, is raised as a player raises one. */
	@Override
	protected HumanoidModel.ArmPose getArmPose(Draugr draugr, HumanoidArm arm) {
		if (draugr.isUsingItem() && draugr.getUseItem().has(DataComponents.BLOCKS_ATTACKS) && draugr.getItemHeldByArm(arm) == draugr.getUseItem()) {
			return HumanoidModel.ArmPose.BLOCK;
		}
		return super.getArmPose(draugr, arm);
	}

	/** The two eye points, lit whatever the light around. */
	private static final class Eyes extends RenderLayer<DraugrRenderState, DraugrModel> {
		Eyes(RenderLayerParent<DraugrRenderState, DraugrModel> renderer) {
			super(renderer);
		}

		@Override
		public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, DraugrRenderState state, float yRot,
				float xRot) {
			collector.order(1).submitModel(getParentModel(), state, poseStack, EYES, lightCoords,
					OverlayTexture.NO_OVERLAY, state.outlineColor);
		}
	}
}
