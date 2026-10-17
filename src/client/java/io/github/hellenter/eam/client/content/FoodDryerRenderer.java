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

/** Draws the food lying flat on the twine of a food dryer, one item in each quarter. */
public class FoodDryerRenderer implements BlockEntityRenderer<DryerBlockEntity, CampfireRenderState> {
	private static final float ITEM_SCALE = 0.375F;
	/** Just above the twine on top of the rack. */
	private static final float ITEM_Y = (float) (DryerBlock.RACK_HEIGHT + 1.0 + 0.25) / 16.0F;

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
	}

	@Override
	public void submit(CampfireRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		for (int slot = 0; slot < state.items.size(); slot++) {
			ItemStackRenderState item = state.items.get(slot);
			if (item.isEmpty()) {
				continue;
			}
			poseStack.pushPose();
			poseStack.translate(
					0.5F + DryerBlockEntity.slotX(slot) * DryerBlockEntity.SLOT_OFFSET,
					ITEM_Y,
					0.5F + DryerBlockEntity.slotZ(slot) * DryerBlockEntity.SLOT_OFFSET);
			poseStack.rotateDegrees(Axis.YP, -90.0F * slot);
			poseStack.rotateDegrees(Axis.XP, 90.0F);
			poseStack.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
			item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
			poseStack.popPose();
		}
	}
}
