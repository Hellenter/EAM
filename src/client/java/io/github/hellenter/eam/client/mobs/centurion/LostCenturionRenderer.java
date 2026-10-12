package io.github.hellenter.eam.client.mobs.centurion;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.client.mobs.fallenlord.FallenLordModel;
import io.github.hellenter.eam.client.mobs.fallenlord.FallenLordRenderState;
import io.github.hellenter.eam.client.mobs.lost.LostModel;
import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLordAction;
import io.github.hellenter.eam.mobs.entity.knight.KnightAction;
import io.github.hellenter.eam.mobs.entity.lost.LostCenturion;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;

/**
 * The Lost centurion: the body of the Lost under his own gear ({@link LostCenturionGearLayer}); the ancient metal armor
 * items he wears give his protection and are not drawn, and neither is the iron spear he holds: his gear model draws
 * his long bannered spear in its place. Spear and shield fighting plays the Fallen Lord's animations, like the rest of
 * the Lost. While he blows the horn his shield is not drawn.
 */
public class LostCenturionRenderer extends HumanoidMobRenderer<LostCenturion, FallenLordRenderState, FallenLordModel> {
	/** As tall as the rest of the Lost, 2.25 blocks: the Fallen Lord's model is 2.4. */
	private static final float SCALE = 2.25F / 2.4F;
	private static final Identifier TEXTURE = Eam.id("textures/entity/lost/lost.png");

	public LostCenturionRenderer(EntityRendererProvider.Context context) {
		super(context, new LostCenturionModel(context.bakeLayer(LostModel.LAYER)), 0.6F * SCALE);
		addLayer(new LostCenturionGearLayer(this, context.getModelSet()));
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
	public void extractRenderState(LostCenturion centurion, FallenLordRenderState state, float partialTicks) {
		super.extractRenderState(centurion, state, partialTicks);
		state.isAggressive = false;
		state.isShaking = false;
		state.isHoldingBow = false;
		state.phase = 2;
		state.action = centurion.action().animation();
		state.knightAction = centurion.action();
		state.actionTime = centurion.actionTime(partialTicks);
		state.guard = centurion.guard(partialTicks);
		state.shieldUp = state.guard > 0.0F;
		state.shieldKnocked = centurion.shieldKnocked(partialTicks);
		boolean holdsShield = state.leftHandItemStack.has(DataComponents.BLOCKS_ATTACKS);
		state.shieldLowered = holdsShield && state.action == FallenLordAction.NONE && state.knightAction == KnightAction.NONE
				? (1.0F - state.guard) * (1.0F - state.shieldKnocked) : 0.0F;
		// The gear model draws his spear.
		state.rightHandItemState.clear();
		if (state.knightAction == KnightAction.HORN) {
			state.leftHandItemState.clear();
		} else {
			// As for the skeleton knight: the model turns the shield into the guard, from its plain item model.
			itemModelResolver.updateForLiving(state.leftHandItemState, state.leftHandItemStack, ItemDisplayContext.THIRD_PERSON_LEFT_HAND,
					centurion);
		}
	}

	@Override
	protected void scale(FallenLordRenderState state, PoseStack poseStack) {
		super.scale(state, poseStack);
		poseStack.scale(SCALE, SCALE, SCALE);
	}

	/** The shield is not a vanilla block pose: the model raises it into the guard. */
	@Override
	protected HumanoidModel.ArmPose getArmPose(LostCenturion centurion, HumanoidArm arm) {
		return centurion.getItemHeldByArm(arm).isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
	}
}
