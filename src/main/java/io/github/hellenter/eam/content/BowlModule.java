package io.github.hellenter.eam.content;

import io.github.hellenter.eam.mixin.content.bucket.CauldronDispatcherInvoker;
import io.github.hellenter.eam.survival.thirst.CauldronBoiling;
import io.github.hellenter.eam.survival.thirst.RainWater;

import net.fabricmc.fabric.api.event.player.UseItemCallback;

import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteractions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * An empty bowl scoops water from a source or a water cauldron like a glass bottle, as in MITE. Rain water or boiled
 * water from a cauldron comes out boiled: it is safe to drink.
 */
public final class BowlModule {
	private BowlModule() {
	}

	public static void register() {
		((CauldronDispatcherInvoker) (Object) CauldronInteractions.WATER).eam$put(Items.BOWL,
				(state, level, pos, player, hand, itemInHand) -> RainWater.takeWater(state, level, pos, player, hand, itemInHand,
						new ItemStack(RainWater.isRainWater(level, pos) || CauldronBoiling.isBoiled(level, pos) ? EamItems.BOILED_WATER_BOWL : EamItems.WATER_BOWL)));
		UseItemCallback.EVENT.register((player, level, hand) -> {
			ItemStack stack = player.getItemInHand(hand);
			if (!stack.is(Items.BOWL)) {
				return InteractionResult.PASS;
			}

			Vec3 eye = player.getEyePosition();
			Vec3 end = eye.add(player.getViewVector(1.0F).scale(player.blockInteractionRange()));
			BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.SOURCE_ONLY, player));
			if (hit.getType() != HitResult.Type.BLOCK) {
				return InteractionResult.PASS;
			}
			BlockPos pos = hit.getBlockPos();
			if (!level.mayInteract(player, pos) || !level.getFluidState(pos).is(FluidTags.WATER)) {
				return InteractionResult.PASS;
			}

			level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.BOTTLE_FILL, SoundSource.NEUTRAL, 1.0F, 1.0F);
			level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
			// The callback's result skips vanilla's hand swap, so the filled bowl goes in the hand here.
			player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(EamItems.WATER_BOWL)));
			return InteractionResult.SUCCESS;
		});
	}
}
