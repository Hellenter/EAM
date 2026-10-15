package io.github.hellenter.eam.mixin.survival.spoilage;

import io.github.hellenter.eam.survival.spoilage.SpoilageModule;
import io.github.hellenter.eam.survival.spoilage.SpoilageStacks;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Food on the ground spoils, and dropped stacks that merge keep each item's spoil time. */
@Mixin(ItemEntity.class)
abstract class ItemEntitySpoilageMixin {
	@Inject(method = "tick", at = @At("TAIL"))
	private void eam$spoil(CallbackInfo ci) {
		SpoilageModule.tickItemEntity((ItemEntity) (Object) this);
	}

	@WrapMethod(method = "merge(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;I)Lnet/minecraft/world/item/ItemStack;")
	private static ItemStack eam$mergeFreshness(ItemStack to, ItemStack from, int maxCount, Operation<ItemStack> original) {
		return SpoilageStacks.mergeItemEntities(to, from, () -> original.call(to, from, maxCount));
	}
}
