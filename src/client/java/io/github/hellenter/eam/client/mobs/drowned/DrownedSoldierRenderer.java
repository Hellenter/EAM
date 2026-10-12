package io.github.hellenter.eam.client.mobs.drowned;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.entity.drowned.DrownedSoldier;

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

/** The drowned soldier, in the pieces of armor he wears; his eyes glow a dim marsh-light green whatever the light. */
public class DrownedSoldierRenderer extends HumanoidMobRenderer<DrownedSoldier, DrownedSoldierRenderState, DrownedSoldierModel> {
	private static final Identifier TEXTURE = Eam.id("textures/entity/drowned_soldier/drowned_soldier.png");
	private static final RenderType EYES = RenderTypes.eyes(Eam.id("textures/entity/drowned_soldier/drowned_soldier_eyes.png"));

	public DrownedSoldierRenderer(EntityRendererProvider.Context context) {
		super(context, new DrownedSoldierModel(context.bakeLayer(DrownedSoldierModel.LAYER)), 0.5F);
		addLayer(new Eyes(this));
	}

	@Override
	public Identifier getTextureLocation(DrownedSoldierRenderState state) {
		return TEXTURE;
	}

	@Override
	public DrownedSoldierRenderState createRenderState() {
		return new DrownedSoldierRenderState();
	}

	@Override
	public void extractRenderState(DrownedSoldier soldier, DrownedSoldierRenderState state, float partialTicks) {
		super.extractRenderState(soldier, state, partialTicks);
		state.gear = soldier.gear();
	}

	/** The two eye points, lit whatever the light around. */
	private static final class Eyes extends RenderLayer<DrownedSoldierRenderState, DrownedSoldierModel> {
		Eyes(RenderLayerParent<DrownedSoldierRenderState, DrownedSoldierModel> renderer) {
			super(renderer);
		}

		@Override
		public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, DrownedSoldierRenderState state, float yRot,
				float xRot) {
			collector.order(1).submitModel(getParentModel(), state, poseStack, EYES, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
		}
	}
}
