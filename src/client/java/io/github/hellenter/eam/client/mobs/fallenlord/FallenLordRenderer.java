package io.github.hellenter.eam.client.mobs.fallenlord;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLord;
import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLordAction;
import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLordLogic;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.component.DataComponents;
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
	/** The blade stops glowing this many ticks into the heavy strike, once the cut is through. */
	private static final float GLOW_AFTER_HIT = 5.0F;
	/** The parry star's frames in order, and how long each shows. */
	private static final int[] PARRY_TWINKLE = {0, 1, 2, 3, 3, 2, 1};
	private static final float PARRY_FRAME_TICKS = 1.5F;

	// Built on first use: item components are not bound yet when renderers are created.
	private @Nullable ItemStack potion;
	private @Nullable ItemStack glowingSword;
	private @Nullable ItemStack parrySword;

	public FallenLordRenderer(EntityRendererProvider.Context context) {
		super(context, new FallenLordModel(context.bakeLayer(FallenLordModel.LAYER)), 0.6F);
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
		state.guard = entity.guard(partialTicks);
		state.shieldKnocked = entity.shieldKnocked(partialTicks);
		boolean eyesGlow = state.action == FallenLordAction.DASH_WINDUP && state.actionTime < FallenLordAction.EYE_FLASH_TICKS;
		state.eyeFrame = eyesGlow ? (int) (state.actionTime * FallenLordEyesModel.FRAMES / FallenLordAction.EYE_FLASH_TICKS) : -1;
		// A yellow star twinkles in his eyes while he can be parried: it grows over the four frames and shrinks back.
		state.parryEyeFrame = entity.isParryOpen()
				? PARRY_TWINKLE[(int) (entity.parryTime(partialTicks) / PARRY_FRAME_TICKS) % PARRY_TWINKLE.length] : -1;
		state.comboGap = FallenLordLogic.COMBO_GAP;
		if (state.action == FallenLordAction.DRINK) {
			// The weapon hand holds the regeneration potion while he drinks.
			if (potion == null) {
				potion = PotionContents.createItemStack(Items.POTION, Potions.REGENERATION);
			}
			itemModelResolver.updateForLiving(state.rightHandItemState, potion, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, entity);
			state.rightHandItemStack = potion;
		}
		if (entity.isParryOpen() && entity.tickCount % 4 != 3) {
			// The edge flickers yellow while a player may parry him.
			if (parrySword == null) {
				parrySword = new ItemStack(EamItems.MITHRIL_SWORD);
				parrySword.set(DataComponents.ITEM_MODEL, Eam.id("fallen_lord_parry_sword"));
			}
			itemModelResolver.updateForLiving(state.rightHandItemState, parrySword, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, entity);
			state.rightHandItemStack = parrySword;
		} else if (bladeGlows(state)) {
			// The edge of the sword glows red through the heavy strike's windup and the cut.
			if (glowingSword == null) {
				glowingSword = new ItemStack(EamItems.MITHRIL_SWORD);
				glowingSword.set(DataComponents.ITEM_MODEL, Eam.id("fallen_lord_heavy_sword"));
			}
			itemModelResolver.updateForLiving(state.rightHandItemState, glowingSword, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, entity);
			state.rightHandItemStack = glowingSword;
		}
		if (state.deathTime > 0.0F) {
			// His own death, not the vanilla roll onto the side in red.
			state.dying = state.deathTime;
			state.deathTime = 0.0F;
			state.hasRedOverlay = false;
			state.action = FallenLordAction.NONE;
			state.eyeFrame = -1;
			state.parryEyeFrame = -1;
			state.guard = 0.0F;
			state.shieldKnocked = 0.0F;
			state.walkAnimationSpeed = 0.0F;
		} else {
			state.dying = -1.0F;
		}
		if (state.action == FallenLordAction.WHIRL) {
			// One full turn of the whole body.
			float p = Mth.clamp((state.actionTime - 2.0F) / 10.0F, 0.0F, 1.0F);
			state.bodyRot += 360.0F * p * p * (3.0F - 2.0F * p);
			state.yRot = 0.0F;
		}
	}

	/** The skull and ribs go grey as they burn to ash. */
	@Override
	protected int getModelTint(FallenLordRenderState state) {
		if (state.dying < 0.0F) {
			return super.getModelTint(state);
		}
		float ash = Mth.clamp((state.dying - FallenLord.ASH_START) / (FallenLord.ASH_END - FallenLord.ASH_START), 0.0F, 1.0F);
		int grey = (int) Mth.lerp(ash, 255.0F, 120.0F);
		return 0xFF000000 | grey << 16 | grey << 8 | grey;
	}

	private static boolean bladeGlows(FallenLordRenderState state) {
		return state.action == FallenLordAction.HEAVY_WINDUP && state.actionTime >= FallenLordAction.HEAVY_GLOW
				|| state.action == FallenLordAction.HEAVY_STRIKE && state.actionTime < FallenLordAction.HEAVY_HIT + GLOW_AFTER_HIT;
	}

	/** The shield is not a vanilla block pose: {@link FallenLordModel} raises it into his own guard. */
	@Override
	protected HumanoidModel.ArmPose getArmPose(FallenLord mob, HumanoidArm arm) {
		return mob.getItemHeldByArm(arm).isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
	}
}
