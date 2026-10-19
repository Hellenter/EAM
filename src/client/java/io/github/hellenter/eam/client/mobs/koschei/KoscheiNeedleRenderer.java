package io.github.hellenter.eam.client.mobs.koschei;

import io.github.hellenter.eam.mobs.entity.koschei.KoscheiNeedle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;

/**
 * Koschei's needle in the air: the needle itself, glinting and spinning faster as it nears the burst, in a pillar of
 * white light running up into the sky and down to the bottom of the shaft it bores.
 */
public class KoscheiNeedleRenderer extends EntityRenderer<KoscheiNeedle, KoscheiNeedleRenderer.State> {
	/** The light reaches this far up. */
	private static final int BEAM_UP = 320;
	private static final int BEAM_COLOR = 0xFFEAF4FF;
	/** It leaves the hand and stops in the air before the light comes. */
	private static final int LIGHT_AFTER = 10;

	private final ItemModelResolver itemModelResolver;

	public KoscheiNeedleRenderer(EntityRendererProvider.Context context) {
		super(context);
		itemModelResolver = context.getItemModelResolver();
	}

	@Override
	protected int getBlockLightLevel(KoscheiNeedle entity, BlockPos pos) {
		return 15;
	}

	/** The pillar of light is far taller than the needle: draw it whenever it is in range at all. */
	@Override
	public boolean shouldRender(KoscheiNeedle entity, Frustum frustum, double camX, double camY, double camZ, float partialTicks) {
		return true;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(KoscheiNeedle needle, State state, float partialTicks) {
		super.extractRenderState(needle, state, partialTicks);
		itemModelResolver.updateForNonLiving(state.item, needle.getItem(), ItemDisplayContext.GROUND, needle);
		float age = needle.age() + partialTicks;
		state.age = age;
		// Faster and faster: a quarter turn a tick at first, a full turn and more by the end.
		state.spin = age * age * 0.9F;
		state.light = age >= LIGHT_AFTER;
		state.depth = needle.depth();
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		poseStack.pushPose();
		poseStack.rotate(Axis.YP.rotationDegrees(state.spin));
		poseStack.scale(1.4F, 1.4F, 1.4F);
		poseStack.translate(0.0F, -0.15F, 0.0F);
		state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
		poseStack.popPose();
		if (state.light) {
			poseStack.pushPose();
			poseStack.translate(-0.5, 0.0, -0.5);
			float time = state.age;
			BeaconRenderer.submitBeaconBeam(poseStack, collector, BeaconRenderer.BEAM_LOCATION, 1.0F, time, 0, BEAM_UP, BEAM_COLOR, 0.15F, 0.3F);
			if (state.depth > 0) {
				BeaconRenderer.submitBeaconBeam(poseStack, collector, BeaconRenderer.BEAM_LOCATION, 1.0F, time, -state.depth, state.depth, BEAM_COLOR,
						0.15F, 0.3F);
			}
			poseStack.popPose();
		}
		super.submit(state, poseStack, collector, camera);
	}

	public static final class State extends EntityRenderState {
		final ItemStackRenderState item = new ItemStackRenderState();
		float age;
		float spin;
		boolean light;
		int depth;
	}
}
