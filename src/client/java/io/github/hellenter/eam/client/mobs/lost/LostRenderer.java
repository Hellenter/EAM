package io.github.hellenter.eam.client.mobs.lost;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.client.mobs.fallenlord.FallenLordModel;
import io.github.hellenter.eam.client.mobs.fallenlord.FallenLordRenderState;
import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLordAction;
import io.github.hellenter.eam.mobs.entity.lost.Lost;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The Lost in their worn ancient metal, drawn by the vanilla armor layer on {@link LostModel}'s jointed armor models.
 * Sword and spear fighting plays the Fallen Lord's animations, like the skeleton knight; archers draw bow and longbow like
 * skeletons, and the shield wall's commander handles his crossbow like a player.
 */
public class LostRenderer extends HumanoidMobRenderer<Lost, FallenLordRenderState, FallenLordModel> {
	/** 2.25 blocks tall: the Fallen Lord's model is 2.4. */
	private static final float SCALE = 2.25F / 2.4F;
	private static final Identifier TEXTURE = Eam.id("textures/entity/lost/lost.png");

	public LostRenderer(EntityRendererProvider.Context context) {
		super(context, new LostModel(context.bakeLayer(LostModel.LAYER)), 0.6F * SCALE);
		addLayer(new HumanoidArmorLayer<>(this, ArmorModelSet.bake(LostModel.ARMOR, context.getModelSet(), LostModel::new),
				context.getEquipmentRenderer()));
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
	public void extractRenderState(Lost lost, FallenLordRenderState state, float partialTicks) {
		super.extractRenderState(lost, state, partialTicks);
		state.isHoldingBow = lost.getMainHandItem().getItem() instanceof BowItem;
		state.isAggressive = state.isHoldingBow && lost.isAggressive();
		state.isShaking = false;
		// The Lord's sword animations.
		state.phase = 2;
		state.action = lost.action().animation();
		state.actionTime = lost.actionTime(partialTicks);
		state.guard = lost.guard(partialTicks);
		state.shieldUp = state.guard > 0.0F;
		state.shieldKnocked = lost.shieldKnocked(partialTicks);
		boolean holdsShield = state.leftHandItemStack.has(DataComponents.BLOCKS_ATTACKS);
		state.shieldLowered = holdsShield && state.action == FallenLordAction.NONE
				? (1.0F - state.guard) * (1.0F - state.shieldKnocked) : 0.0F;
		// As for the skeleton knight: the shield is turned into the guard by the model, from its plain item model.
		itemModelResolver.updateForLiving(state.leftHandItemState, state.leftHandItemStack, ItemDisplayContext.THIRD_PERSON_LEFT_HAND, lost);
	}

	@Override
	protected void scale(FallenLordRenderState state, PoseStack poseStack) {
		super.scale(state, poseStack);
		poseStack.scale(SCALE, SCALE, SCALE);
	}

	/** A drawn bow and a crossbow are the vanilla poses; a shield is raised by the model itself. */
	@Override
	protected HumanoidModel.ArmPose getArmPose(Lost lost, HumanoidArm arm) {
		if (lost.getMainArm() == arm && lost.isAggressive() && lost.getMainHandItem().getItem() instanceof BowItem) {
			return HumanoidModel.ArmPose.BOW_AND_ARROW;
		}
		// The shield wall's commander loads his crossbow and holds it loaded at his shoulder, like a player.
		ItemStack held = lost.getItemHeldByArm(arm);
		if (held.is(Items.CROSSBOW)) {
			if (lost.isUsingItem() && lost.getUseItem().is(Items.CROSSBOW)) {
				return HumanoidModel.ArmPose.CROSSBOW_CHARGE;
			}
			if (CrossbowItem.isCharged(held)) {
				return HumanoidModel.ArmPose.CROSSBOW_HOLD;
			}
		}
		return lost.getItemHeldByArm(arm).isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
	}
}
