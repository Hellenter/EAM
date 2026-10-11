package io.github.hellenter.eam.client.mobs.warlock;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.entity.warlock.UndeadWarlock;
import io.github.hellenter.eam.mobs.entity.warlock.WarlockSpell;

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

/** The undead warlock, 2.25 blocks tall like the Lost; his eyes flare red in the dark of his hood while he casts. */
public class UndeadWarlockRenderer extends MobRenderer<UndeadWarlock, UndeadWarlockRenderState, UndeadWarlockModel> {
	/** The model is 2 blocks tall. */
	private static final float SCALE = 2.25F / 2.0F;
	private static final Identifier TEXTURE = Eam.id("textures/entity/undead_warlock/undead_warlock.png");
	private static final RenderType EYES = RenderTypes.eyes(Eam.id("textures/entity/undead_warlock/undead_warlock_eyes.png"));

	public UndeadWarlockRenderer(EntityRendererProvider.Context context) {
		super(context, new UndeadWarlockModel(context.bakeLayer(UndeadWarlockModel.LAYER)), 0.5F * SCALE);
		addLayer(new Eyes(this));
	}

	@Override
	public Identifier getTextureLocation(UndeadWarlockRenderState state) {
		return TEXTURE;
	}

	@Override
	public UndeadWarlockRenderState createRenderState() {
		return new UndeadWarlockRenderState();
	}

	@Override
	public void extractRenderState(UndeadWarlock warlock, UndeadWarlockRenderState state, float partialTicks) {
		super.extractRenderState(warlock, state, partialTicks);
		state.spell = warlock.spell();
		state.castTime = warlock.castTime(partialTicks);
	}

	@Override
	protected void scale(UndeadWarlockRenderState state, PoseStack poseStack) {
		super.scale(state, poseStack);
		poseStack.scale(SCALE, SCALE, SCALE);
	}

	/** Two red embers under the hood, lit whatever the light around, only while a spell is in his hands. */
	private static final class Eyes extends RenderLayer<UndeadWarlockRenderState, UndeadWarlockModel> {
		Eyes(RenderLayerParent<UndeadWarlockRenderState, UndeadWarlockModel> renderer) {
			super(renderer);
		}

		@Override
		public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, UndeadWarlockRenderState state, float yRot,
				float xRot) {
			if (state.spell == WarlockSpell.NONE) {
				return;
			}
			collector.order(1).submitModel(getParentModel(), state, poseStack, EYES, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
		}
	}
}
