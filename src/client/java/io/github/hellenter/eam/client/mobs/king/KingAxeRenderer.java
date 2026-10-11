package io.github.hellenter.eam.client.mobs.king;

import io.github.hellenter.eam.mobs.entity.king.PillagerKing;
import io.github.hellenter.eam.mobs.entity.king.ThrownKingAxe;

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

/** Draws the thrown axe as its item, spinning end over end along its flight. */
public class KingAxeRenderer extends EntityRenderer<ThrownKingAxe, KingAxeRenderer.State> {
	/** The axe's 32x32 texture keeps the pixel size of a 16x16 item: this draws it as big as in his hand. */
	private static final float SCALE = 1.575F;
	/** Degrees per tick it turns end over end. */
	private static final float SPIN = 50.0F;

	private final ItemModelResolver itemModelResolver;
	// Built on first use: item components are not bound yet when renderers are created.
	private @Nullable ItemStack axe;

	public KingAxeRenderer(EntityRendererProvider.Context context) {
		super(context);
		itemModelResolver = context.getItemModelResolver();
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		poseStack.pushPose();
		poseStack.translate(0.0F, 0.25F, 0.0F);
		// The texture's left side, the edge, faces the way it flies, and the head turns over forward: the edge leads.
		poseStack.rotateDegrees(Axis.YP, state.yRot + 90.0F);
		poseStack.rotateDegrees(Axis.ZP, state.spin);
		poseStack.scale(SCALE, SCALE, SCALE);
		state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
		poseStack.popPose();
		super.submit(state, poseStack, collector, camera);
	}

	@Override
	protected AABB getBoundingBoxForCulling(ThrownKingAxe entity, float partialTicks) {
		return super.getBoundingBoxForCulling(entity, partialTicks).inflate(1.0);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(ThrownKingAxe entity, State state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.yRot = entity.getYRot(partialTicks);
		state.spin = entity.isStuck() ? 0.0F : (entity.tickCount + partialTicks) * SPIN;
		if (axe == null) {
			axe = PillagerKing.axeStack();
		}
		itemModelResolver.updateForNonLiving(state.item, axe, ItemDisplayContext.NONE, entity);
	}

	public static class State extends EntityRenderState {
		public final ItemStackRenderState item = new ItemStackRenderState();
		public float yRot;
		public float spin;
	}
}
