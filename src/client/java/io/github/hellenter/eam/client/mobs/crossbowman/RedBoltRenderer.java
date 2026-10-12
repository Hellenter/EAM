package io.github.hellenter.eam.client.mobs.crossbowman;

import io.github.hellenter.eam.mobs.entity.crossbowman.RedBolt;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.projectile.ArrowModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;

/** The red bolt: a vanilla arrow tinted red and lit up whatever the light around. */
public class RedBoltRenderer extends EntityRenderer<RedBolt, ArrowRenderState> {
	private static final Identifier ARROW = Identifier.withDefaultNamespace("textures/entity/projectiles/arrow.png");
	private static final int RED = 0xFFFF3A22;

	private final ArrowModel model;

	public RedBoltRenderer(EntityRendererProvider.Context context) {
		super(context);
		model = new ArrowModel(context.bakeLayer(ModelLayers.ARROW));
	}

	@Override
	public void submit(ArrowRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		poseStack.pushPose();
		poseStack.rotateDegrees(Axis.YP, state.yRot - 90.0F);
		poseStack.rotateDegrees(Axis.ZP, state.xRot);
		collector.submitModel(model, state, poseStack, RenderTypes.entityCutoutCull(ARROW), LightCoordsUtil.FULL_BRIGHT,
				OverlayTexture.NO_OVERLAY, RED, null, state.outlineColor);
		poseStack.popPose();
		super.submit(state, poseStack, collector, camera);
	}

	@Override
	public ArrowRenderState createRenderState() {
		return new ArrowRenderState();
	}

	@Override
	public void extractRenderState(RedBolt bolt, ArrowRenderState state, float partialTicks) {
		super.extractRenderState(bolt, state, partialTicks);
		state.xRot = bolt.getXRot(partialTicks);
		state.yRot = bolt.getYRot(partialTicks);
		state.shake = bolt.shakeTime - partialTicks;
	}
}
