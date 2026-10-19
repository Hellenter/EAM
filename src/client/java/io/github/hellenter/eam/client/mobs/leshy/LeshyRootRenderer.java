package io.github.hellenter.eam.client.mobs.leshy;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.entity.leshy.LeshyRoot;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** A root of the leshy's, shooting up out of the ground, standing a moment and sinking back. */
public class LeshyRootRenderer extends EntityRenderer<LeshyRoot, LeshyRootRenderState> {
	private static final Identifier TEXTURE = Eam.id("textures/entity/leshy/leshy_root.png");
	/** Ticks it takes to shoot up, and to sink back. */
	private static final float RISE = 3.0F;
	private static final float SINK = 6.0F;
	/** How far below the ground it waits, in blocks: its full height. */
	private static final float DEPTH = 1.4F;

	private final LeshyRootModel model;

	public LeshyRootRenderer(EntityRendererProvider.Context context) {
		super(context);
		model = new LeshyRootModel(context.bakeLayer(LeshyRootModel.LAYER));
	}

	@Override
	public void submit(LeshyRootRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.rise <= 0.0F) {
			return;
		}
		poseStack.pushPose();
		poseStack.translate(0.0F, -(1.0F - state.rise) * DEPTH, 0.0F);
		poseStack.rotateDegrees(Axis.YP, 180.0F - state.yRot);
		poseStack.scale(-1.0F, -1.0F, 1.0F);
		poseStack.translate(0.0F, -1.501F, 0.0F);
		collector.submitModel(model, state, poseStack, TEXTURE, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
		poseStack.popPose();
		super.submit(state, poseStack, collector, camera);
	}

	@Override
	public LeshyRootRenderState createRenderState() {
		return new LeshyRootRenderState();
	}

	@Override
	public void extractRenderState(LeshyRoot entity, LeshyRootRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.yRot = entity.getYRot();
		float up = entity.tickCount + partialTicks - entity.burstTick();
		state.upTime = up;
		if (up < 0.0F) {
			state.rise = 0.0F;
		} else if (up < RISE) {
			state.rise = up / RISE;
		} else {
			state.rise = Mth.clamp((LeshyRoot.UP_TIME - up) / SINK, 0.0F, 1.0F);
		}
	}
}
