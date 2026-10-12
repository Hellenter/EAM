package io.github.hellenter.eam.client.mobs.guard;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.entity.guard.GuardAction;
import io.github.hellenter.eam.mobs.entity.guard.PillagerGuard;
import io.github.hellenter.eam.mobs.entity.guard.PillagerGuardLogic;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.monster.illager.IllagerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.IllagerRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * A pillager guard: helmet, cuirass and axe. Like a vindicator he shows the axe only when he fights; his eyes glow
 * yellow under the brim while a hit parries his onslaught, as the king's do.
 */
public class PillagerGuardRenderer extends IllagerRenderer<PillagerGuard, PillagerGuardRenderState> {
	private static final Identifier TEXTURE = Eam.id("textures/entity/pillager_guard/pillager_guard.png");
	private static final RenderType PARRY_EYES = RenderTypes.eyes(Eam.id("textures/entity/pillager_guard/pillager_guard_parry_eyes.png"));

	public PillagerGuardRenderer(EntityRendererProvider.Context context) {
		super(context, new PillagerGuardModel(context.bakeLayer(PillagerGuardModel.LAYER)), 0.5F);
		addLayer(new ItemInHandLayer<PillagerGuardRenderState, IllagerModel<PillagerGuardRenderState>>(this) {
			@Override
			public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, PillagerGuardRenderState state, float yRot,
					float xRot) {
				if (state.isAggressive || state.action != GuardAction.NONE) {
					super.submit(poseStack, collector, lightCoords, state, yRot, xRot);
				}
			}
		});
		addLayer(new ParryEyes(this));
	}

	@Override
	public Identifier getTextureLocation(PillagerGuardRenderState state) {
		return TEXTURE;
	}

	@Override
	public PillagerGuardRenderState createRenderState() {
		return new PillagerGuardRenderState();
	}

	@Override
	public void extractRenderState(PillagerGuard guard, PillagerGuardRenderState state, float partialTicks) {
		super.extractRenderState(guard, state, partialTicks);
		state.action = guard.action();
		state.actionTime = guard.actionTime(partialTicks);
		state.parryEyes = PillagerGuardLogic.parries(state.action, (int) state.actionTime);
	}

	/** Eyes glowing whatever the light around, while a hit parries him. */
	private static final class ParryEyes extends RenderLayer<PillagerGuardRenderState, IllagerModel<PillagerGuardRenderState>> {
		ParryEyes(RenderLayerParent<PillagerGuardRenderState, IllagerModel<PillagerGuardRenderState>> renderer) {
			super(renderer);
		}

		@Override
		public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, PillagerGuardRenderState state, float yRot,
				float xRot) {
			if (state.parryEyes) {
				collector.order(1).submitModel(getParentModel(), state, poseStack, PARRY_EYES, lightCoords, OverlayTexture.NO_OVERLAY,
						state.outlineColor);
			}
		}
	}
}
