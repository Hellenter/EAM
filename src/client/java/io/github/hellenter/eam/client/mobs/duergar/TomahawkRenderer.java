package io.github.hellenter.eam.client.mobs.duergar;

import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.content.tools.ThrownTomahawk;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import org.jspecify.annotations.Nullable;

/** Draws a thrown tomahawk as its item, spinning end over end along its flight, edge first. */
public class TomahawkRenderer extends EntityRenderer<ThrownTomahawk, TomahawkRenderer.State> {
	/** As big as in a hand. */
	private static final float SCALE = 0.8F;
	/** Degrees per tick it turns end over end. */
	private static final float SPIN = 45.0F;

	private final ItemModelResolver itemModelResolver;
	// Built on first use: item components are not bound yet when renderers are created.
	private @Nullable ItemStack tomahawk;

	public TomahawkRenderer(EntityRendererProvider.Context context) {
		super(context);
		itemModelResolver = context.getItemModelResolver();
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		poseStack.pushPose();
		poseStack.translate(0.0F, 0.2F, 0.0F);
		poseStack.rotateDegrees(Axis.YP, state.yRot + 90.0F);
		poseStack.rotateDegrees(Axis.ZP, state.spin);
		poseStack.scale(SCALE, SCALE, SCALE);
		state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
		poseStack.popPose();
		super.submit(state, poseStack, collector, camera);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(ThrownTomahawk entity, State state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.yRot = entity.getYRot(partialTicks);
		// Stuck, it rests head down in the block, as it went in.
		state.spin = entity.isStuck() ? -45.0F : (entity.tickCount + partialTicks) * SPIN;
		if (tomahawk == null) {
			tomahawk = new ItemStack(EamItems.TOMAHAWK);
		}
		itemModelResolver.updateForNonLiving(state.item, tomahawk, ItemDisplayContext.NONE, entity);
	}

	public static class State extends EntityRenderState {
		public final ItemStackRenderState item = new ItemStackRenderState();
		public float yRot;
		public float spin;
	}
}
