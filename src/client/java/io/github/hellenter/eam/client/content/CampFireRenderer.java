package io.github.hellenter.eam.client.content;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import io.github.hellenter.eam.content.campfire.CampFireBlockEntity;

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

/** Draws the food lying on the stone ring of a camp fire, one item in each corner, like a vanilla campfire. */
public class CampFireRenderer implements BlockEntityRenderer<CampFireBlockEntity, CampfireRenderState> {
	private static final float ITEM_SCALE = 0.375F;
	/** Just above the stone ring, which is 2 pixels high. */
	private static final float ITEM_Y = 2.25F / 16.0F;

	private final ItemModelResolver itemModelResolver;

	public CampFireRenderer(BlockEntityRendererProvider.Context context) {
		this.itemModelResolver = context.itemModelResolver();
	}

	@Override
	public CampfireRenderState createRenderState() {
		return new CampfireRenderState();
	}

	@Override
	public void extractRenderState(CampFireBlockEntity fire, CampfireRenderState state, float partialTicks, Vec3 cameraPosition,
			ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(fire, state, partialTicks, cameraPosition, breakProgress);
		int seed = (int) fire.getBlockPos().asLong();
		List<ItemStackRenderState> items = new ArrayList<>();
		for (int slot = 0; slot < CampFireBlockEntity.SLOTS; slot++) {
			ItemStackRenderState item = new ItemStackRenderState();
			itemModelResolver.updateForTopItem(item, fire.getItems().get(slot), ItemDisplayContext.FIXED, fire.getLevel(), null, seed + slot);
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
					0.5F + CampFireBlockEntity.slotX(slot) * CampFireBlockEntity.SLOT_OFFSET,
					ITEM_Y,
					0.5F + CampFireBlockEntity.slotZ(slot) * CampFireBlockEntity.SLOT_OFFSET);
			poseStack.rotateDegrees(Axis.YP, -90.0F * slot);
			poseStack.rotateDegrees(Axis.XP, 90.0F);
			poseStack.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
			item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
			poseStack.popPose();
		}
	}
}
