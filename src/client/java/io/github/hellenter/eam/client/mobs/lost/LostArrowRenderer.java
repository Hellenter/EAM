package io.github.hellenter.eam.client.mobs.lost;

import io.github.hellenter.eam.mobs.entity.lost.LostArrow;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.projectile.ArrowModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;

/** The longbowman's arrow: a vanilla arrow, black for a marking arrow, red and lit up for the power arrow. */
public class LostArrowRenderer extends EntityRenderer<LostArrow, LostArrowRenderState> {
	private static final Identifier ARROW = Identifier.withDefaultNamespace("textures/entity/projectiles/arrow.png");
	private static final int WHITE = 0xFFFFFFFF;
	private static final int BLACK = 0xFF262626;
	private static final int RED = 0xFFFF3A22;

	private final ArrowModel model;

	public LostArrowRenderer(EntityRendererProvider.Context context) {
		super(context);
		model = new ArrowModel(context.bakeLayer(ModelLayers.ARROW));
	}

	@Override
	public void submit(LostArrowRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		int color = switch (state.kind) {
			case PLAIN -> WHITE;
			case MARK -> BLACK;
			case RED -> RED;
		};
		int light = state.kind == LostArrow.Kind.RED ? LightCoordsUtil.FULL_BRIGHT : state.lightCoords;
		poseStack.pushPose();
		poseStack.rotateDegrees(Axis.YP, state.yRot - 90.0F);
		poseStack.rotateDegrees(Axis.ZP, state.xRot);
		collector.submitModel(model, state, poseStack, RenderTypes.entityCutoutCull(ARROW), light, OverlayTexture.NO_OVERLAY, color, null,
				state.outlineColor);
		poseStack.popPose();
		super.submit(state, poseStack, collector, camera);
	}

	@Override
	public LostArrowRenderState createRenderState() {
		return new LostArrowRenderState();
	}

	@Override
	public void extractRenderState(LostArrow arrow, LostArrowRenderState state, float partialTicks) {
		super.extractRenderState(arrow, state, partialTicks);
		state.xRot = arrow.getXRot(partialTicks);
		state.yRot = arrow.getYRot(partialTicks);
		state.shake = arrow.shakeTime - partialTicks;
		state.kind = arrow.kind();
	}
}
