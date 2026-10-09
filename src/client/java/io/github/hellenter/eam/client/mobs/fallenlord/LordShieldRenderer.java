package io.github.hellenter.eam.client.mobs.fallenlord;

import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLord;
import io.github.hellenter.eam.mobs.entity.fallenlord.LordShield;

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
import net.minecraft.world.phys.AABB;

import org.jspecify.annotations.Nullable;

/** Draws the dropped shield lying flat on its back, as big as it looked in the Fallen Lord's hand. */
public class LordShieldRenderer extends EntityRenderer<LordShield, LordShieldRenderer.State> {
	/** The Fallen Lord's own size: his 1.2 model scale times his 1.1 scale attribute. */
	private static final float SCALE = 1.32F;

	private final ItemModelResolver itemModelResolver;
	// Built on first use: item components are not bound yet when renderers are created.
	private @Nullable ItemStack shield;

	public LordShieldRenderer(EntityRendererProvider.Context context) {
		super(context);
		itemModelResolver = context.getItemModelResolver();
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		poseStack.pushPose();
		poseStack.rotateDegrees(Axis.YP, -state.yRot);
		// The plate's back rests just above the ground; the handle under it sinks out of sight.
		poseStack.translate(0.0F, 0.06F, 0.0F);
		poseStack.rotateDegrees(Axis.XP, -90.0F);
		poseStack.scale(SCALE, SCALE, SCALE);
		// Centres the plate: the shield model sits half a block off the item origin, like the vanilla shield.
		poseStack.translate(0.5F, 0.5F, 0.40625F);
		state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
		poseStack.popPose();
		super.submit(state, poseStack, collector, camera);
	}

	@Override
	protected AABB getBoundingBoxForCulling(LordShield entity, float partialTicks) {
		return super.getBoundingBoxForCulling(entity, partialTicks).inflate(1.0);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(LordShield entity, State state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.yRot = entity.getYRot();
		if (shield == null) {
			shield = FallenLord.shieldStack();
		}
		itemModelResolver.updateForNonLiving(state.item, shield, ItemDisplayContext.NONE, entity);
	}

	public static class State extends EntityRenderState {
		public final ItemStackRenderState item = new ItemStackRenderState();
		public float yRot;
	}
}
