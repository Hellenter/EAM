package io.github.hellenter.eam.mixin.survival.spoilage;

import io.github.hellenter.eam.survival.spoilage.SpoilageStacks;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Perishable stacks stack across freshness and carry each item's spoil time through splits and merges. */
@Mixin(ItemStack.class)
abstract class ItemStackSpoilageMixin {
	@ModifyReturnValue(method = "isSameItemSameComponents", at = @At("RETURN"))
	private static boolean eam$stackAcrossFreshness(boolean same, ItemStack a, ItemStack b) {
		return SpoilageStacks.sameIgnoringFreshness(a, b, same);
	}

	@ModifyReturnValue(method = "matches", at = @At("RETURN"))
	private static boolean eam$matchFreshness(boolean matches, ItemStack a, ItemStack b) {
		return SpoilageStacks.matchesWithFreshness(a, b, matches);
	}

	@Inject(method = "setCount", at = @At("HEAD"))
	private void eam$moveFreshness(int count, CallbackInfo ci) {
		SpoilageStacks.onSetCount((ItemStack) (Object) this, count);
	}

	@WrapMethod(method = "copyWithCount")
	private ItemStack eam$copyOldest(int count, Operation<ItemStack> original) {
		return SpoilageStacks.copyWithCount((ItemStack) (Object) this, count, () -> original.call(count));
	}
}
