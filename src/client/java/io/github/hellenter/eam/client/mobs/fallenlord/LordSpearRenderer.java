package io.github.hellenter.eam.client.mobs.fallenlord;

import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLord;
import io.github.hellenter.eam.mobs.entity.fallenlord.ThrownLordSpear;

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
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import org.jspecify.annotations.Nullable;

/** Draws the thrown spear (or sword, or a Lost cavalryman's iron spear) as its item, point first along its flight. */
public class LordSpearRenderer extends EntityRenderer<ThrownLordSpear, LordSpearRenderer.State> {
	private static final float SCALE = 1.6F;

	private final ItemModelResolver itemModelResolver;
	// Built on first use: item components are not bound yet when renderers are created.
	private @Nullable ItemStack spear;
	private @Nullable ItemStack sword;
	private @Nullable ItemStack plain;

	public LordSpearRenderer(EntityRendererProvider.Context context) {
		super(context);
		itemModelResolver = context.getItemModelResolver();
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		poseStack.pushPose();
		poseStack.rotateDegrees(Axis.YP, state.yRot - 90.0F);
		poseStack.rotateDegrees(Axis.ZP, state.xRot);
		// The item texture points to its top right corner; turn that corner to the front.
		poseStack.rotateDegrees(Axis.ZP, -45.0F);
		poseStack.scale(SCALE, SCALE, SCALE);
		state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
		poseStack.popPose();
		super.submit(state, poseStack, collector, camera);
	}

	@Override
	protected AABB getBoundingBoxForCulling(ThrownLordSpear entity, float partialTicks) {
		return super.getBoundingBoxForCulling(entity, partialTicks).inflate(1.5);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(ThrownLordSpear entity, State state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.yRot = entity.getYRot(partialTicks);
		state.xRot = entity.getXRot(partialTicks);
		if (spear == null) {
			spear = FallenLord.spearStack();
			sword = new ItemStack(EamItems.MITHRIL_SWORD);
			plain = new ItemStack(Items.IRON_SPEAR);
		}
		ItemStack stack = entity.isPlain() ? plain : entity.isSword() ? sword : spear;
		itemModelResolver.updateForNonLiving(state.item, stack, ItemDisplayContext.NONE, entity);
	}

	public static class State extends EntityRenderState {
		public final ItemStackRenderState item = new ItemStackRenderState();
		public float yRot;
		public float xRot;
	}
}
