package io.github.hellenter.eam.client.mobs.warlock;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.entity.warlock.MagicMissile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.projectile.ArrowModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** The warlock's missile: a blood-red arrow, lit by its own glow, pointing the way it flies as it curves after its target. */
public class MagicMissileRenderer extends EntityRenderer<MagicMissile, ArrowRenderState> {
	private static final Identifier TEXTURE = Eam.id("textures/entity/undead_warlock/magic_missile.png");
	private final ArrowModel model;

	public MagicMissileRenderer(EntityRendererProvider.Context context) {
		super(context);
		model = new ArrowModel(context.bakeLayer(ModelLayers.ARROW));
	}

	@Override
	public ArrowRenderState createRenderState() {
		return new ArrowRenderState();
	}

	@Override
	public void extractRenderState(MagicMissile missile, ArrowRenderState state, float partialTicks) {
		super.extractRenderState(missile, state, partialTicks);
		// Arrow angles from its flight, so it never lags behind on a turn.
		Vec3 v = missile.getDeltaMovement();
		state.yRot = (float) (Mth.atan2(v.x, v.z) * Mth.RAD_TO_DEG);
		state.xRot = (float) (Mth.atan2(v.y, v.horizontalDistance()) * Mth.RAD_TO_DEG);
		state.shake = 0.0F;
	}

	@Override
	public void submit(ArrowRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		poseStack.pushPose();
		poseStack.rotateDegrees(Axis.YP, state.yRot - 90.0F);
		poseStack.rotateDegrees(Axis.ZP, state.xRot);
		collector.submitModel(model, state, poseStack, TEXTURE, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, state.outlineColor);
		poseStack.popPose();
		super.submit(state, poseStack, collector, camera);
	}
}
