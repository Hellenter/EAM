package io.github.hellenter.eam.client.mobs.cavalry;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.client.mobs.fallenlord.FallenLordModel;
import io.github.hellenter.eam.client.mobs.fallenlord.FallenLordRenderState;
import io.github.hellenter.eam.client.mobs.lost.LostModel;
import io.github.hellenter.eam.mobs.entity.cavalry.LostCavalryman;
import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLordAction;
import io.github.hellenter.eam.mobs.entity.knight.KnightAction;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;

/**
 * The heavy Lost cavalryman: the body of the Lost under his own plate ({@link LostCavalrymanGearLayer}); the ancient
 * metal armor items he wears give his protection and are not drawn. A tenth smaller than the skeleton knight. He fights
 * on foot with the knight's animations and his own ({@link CavalryPoses}); for the axe sweep the renderer turns him
 * full circle.
 */
public class LostCavalrymanRenderer extends HumanoidMobRenderer<LostCavalryman, FallenLordRenderState, FallenLordModel> {
	/** 1.94 blocks tall: the Fallen Lord's model is 2.4. */
	private static final float SCALE = 1.94F / 2.4F;
	private static final Identifier TEXTURE = Eam.id("textures/entity/lost/lost.png");

	public LostCavalrymanRenderer(EntityRendererProvider.Context context) {
		super(context, new LostCavalrymanModel(context.bakeLayer(LostModel.LAYER)), 0.6F * SCALE);
		addLayer(new LostCavalrymanGearLayer(this, context.getModelSet()));
	}

	@Override
	public Identifier getTextureLocation(FallenLordRenderState state) {
		return TEXTURE;
	}

	@Override
	public FallenLordRenderState createRenderState() {
		return new FallenLordRenderState();
	}

	@Override
	public void extractRenderState(LostCavalryman man, FallenLordRenderState state, float partialTicks) {
		super.extractRenderState(man, state, partialTicks);
		state.isAggressive = false;
		state.isShaking = false;
		state.isHoldingBow = false;
		state.phase = 2;
		state.action = man.action().animation();
		state.knightAction = man.action();
		state.actionTime = man.actionTime(partialTicks);
		state.guard = man.guard(partialTicks);
		state.shieldUp = state.guard > 0.0F;
		state.shieldKnocked = man.shieldKnocked(partialTicks);
		boolean holdsShield = state.leftHandItemStack.has(DataComponents.BLOCKS_ATTACKS);
		state.shieldLowered = holdsShield && state.action == FallenLordAction.NONE && state.knightAction == KnightAction.NONE
				&& !state.isPassenger ? (1.0F - state.guard) * (1.0F - state.shieldKnocked) : 0.0F;
		state.mountSpeed = man.getVehicle() instanceof LivingEntity horse ? Math.min(1.0F, horse.walkAnimation.speed(partialTicks)) : 0.0F;
		// As for the skeleton knight: the model turns the shield into the guard, from its plain item model.
		itemModelResolver.updateForLiving(state.leftHandItemState, state.leftHandItemStack, ItemDisplayContext.THIRD_PERSON_LEFT_HAND, man);
		if (state.knightAction == KnightAction.AXE_SWEEP) {
			// One full turn of the whole body with the axe out.
			float p = Mth.clamp((state.actionTime - KnightAction.AXE_SWEEP_WINDUP) / 6.0F, 0.0F, 1.0F);
			state.bodyRot += 360.0F * p * p * (3.0F - 2.0F * p);
			state.yRot = 0.0F;
		}
	}

	@Override
	protected void scale(FallenLordRenderState state, PoseStack poseStack) {
		super.scale(state, poseStack);
		poseStack.scale(SCALE, SCALE, SCALE);
	}

	/** The shield is not a vanilla block pose: the model raises it into the guard. */
	@Override
	protected HumanoidModel.ArmPose getArmPose(LostCavalryman man, HumanoidArm arm) {
		return man.getItemHeldByArm(arm).isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
	}
}
