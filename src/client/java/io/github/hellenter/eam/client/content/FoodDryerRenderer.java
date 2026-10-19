package io.github.hellenter.eam.client.content;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import io.github.hellenter.eam.content.dryer.DryerBlock;
import io.github.hellenter.eam.content.dryer.DryerBlockEntity;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.CampfireRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Draws the food hanging upright from the hooks of a food dryer, each item a little turned. */
public class FoodDryerRenderer implements BlockEntityRenderer<DryerBlockEntity, CampfireRenderState> {
	private static final float ITEM_SCALE = 0.3F;
	/** How far the hook goes into the food, in pixels. */
	private static final float PIERCE = 0.5F;
	/** Each item's turn about its hook, in degrees, so the row does not look stamped out. */
	private static final float[] TURN = {12.0F, -8.0F, -14.0F, 9.0F};

	private final ItemModelResolver itemModelResolver;

	public FoodDryerRenderer(BlockEntityRendererProvider.Context context) {
		this.itemModelResolver = context.itemModelResolver();
	}

	@Override
	public CampfireRenderState createRenderState() {
		return new CampfireRenderState();
	}

	@Override
	public void extractRenderState(DryerBlockEntity dryer, CampfireRenderState state, float partialTicks, Vec3 cameraPosition,
			ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(dryer, state, partialTicks, cameraPosition, breakProgress);
		int seed = (int) dryer.getBlockPos().asLong();
		List<ItemStackRenderState> items = new ArrayList<>();
		for (int slot = 0; slot < DryerBlockEntity.SLOTS; slot++) {
			ItemStackRenderState item = new ItemStackRenderState();
			itemModelResolver.updateForTopItem(item, dryer.getItems().get(slot), ItemDisplayContext.FIXED, dryer.getLevel(), null, seed + slot);
			items.add(item);
		}
		state.items = items;
		state.facing = dryer.getBlockState().getValue(DryerBlock.FACING);
	}

	@Override
	public void submit(CampfireRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		for (int slot = 0; slot < state.items.size(); slot++) {
			ItemStackRenderState item = state.items.get(slot);
			if (item.isEmpty()) {
				continue;
			}
			poseStack.pushPose();
			// The model faces south unrotated; turn the hooks with the block about its centre.
			poseStack.translate(0.5F, 0.0F, 0.5F);
			poseStack.rotateDegrees(Axis.YP, -90.0F * state.facing.get2DDataValue());
			float top = (float) DryerBlockEntity.hookBottom(slot) + PIERCE;
			poseStack.translate(
					(float) DryerBlockEntity.HOOK_X[slot] / 16.0F - 0.5F,
					top / 16.0F - ITEM_SCALE / 2.0F,
					(float) DryerBlockEntity.HOOK_Z[slot] / 16.0F - 0.5F);
			poseStack.rotateDegrees(Axis.YP, TURN[slot]);
			poseStack.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
			item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
			poseStack.popPose();
		}
	}
}
