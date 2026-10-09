package io.github.hellenter.eam.client.mobs.fallenlord;

import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLord;
import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLordAction;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

import org.jspecify.annotations.Nullable;

/** A skeleton the size of a wither skeleton (the vanilla 1.2 scaled model) in his own mithril armor and a red cape. */
public class FallenLordRenderer extends HumanoidMobRenderer<FallenLord, FallenLordRenderState, FallenLordModel> {
	private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/entity/skeleton/skeleton.png");

	// Built on first use: item components are not bound yet when renderers are created.
	private @Nullable ItemStack potion;

	public FallenLordRenderer(EntityRendererProvider.Context context) {
		super(context, new FallenLordModel(context.bakeLayer(ModelLayers.WITHER_SKELETON)), 0.6F);
		addLayer(new FallenLordGearLayer(this, context.getModelSet()));
		addLayer(new FallenLordEyesLayer(this, context.getModelSet()));
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
	public void extractRenderState(FallenLord entity, FallenLordRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.isAggressive = false;
		state.isShaking = false;
		state.isHoldingBow = false;
		state.phase = entity.phase();
		state.action = entity.action();
		state.actionTime = entity.actionTime(partialTicks);
		state.shieldUp = entity.isShieldUp();
		boolean eyesGlow = state.action == FallenLordAction.DASH_WINDUP && state.actionTime < FallenLordAction.EYE_FLASH_TICKS;
		state.eyeFrame = eyesGlow ? (int) (state.actionTime * FallenLordEyesModel.FRAMES / FallenLordAction.EYE_FLASH_TICKS) : -1;
		if (state.action == FallenLordAction.DRINK) {
			// The weapon hand holds the regeneration potion while he drinks.
			if (potion == null) {
				potion = PotionContents.createItemStack(Items.POTION, Potions.REGENERATION);
			}
			itemModelResolver.updateForLiving(state.rightHandItemState, potion, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, entity);
			state.rightHandItemStack = potion;
		}
		if (state.action == FallenLordAction.WHIRL) {
			// One full turn of the whole body.
			float p = Mth.clamp((state.actionTime - 2.0F) / 10.0F, 0.0F, 1.0F);
			state.bodyRot += 360.0F * p * p * (3.0F - 2.0F * p);
			state.yRot = 0.0F;
		}
	}

	@Override
	protected HumanoidModel.ArmPose getArmPose(FallenLord mob, HumanoidArm arm) {
		if (mob.getItemHeldByArm(arm).isEmpty()) {
			return HumanoidModel.ArmPose.EMPTY;
		}
		if (arm != mob.getMainArm() && mob.getItemHeldByArm(arm).is(Items.SHIELD) && mob.isShieldUp()) {
			return HumanoidModel.ArmPose.BLOCK;
		}
		return HumanoidModel.ArmPose.ITEM;
	}
}
