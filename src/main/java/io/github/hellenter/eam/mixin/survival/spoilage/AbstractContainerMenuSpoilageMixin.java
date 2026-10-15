package io.github.hellenter.eam.mixin.survival.spoilage;

import io.github.hellenter.eam.survival.spoilage.SpoilageStacks;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Dragging food over slots keeps the spoil times of what was already in them. */
@Mixin(AbstractContainerMenu.class)
abstract class AbstractContainerMenuSpoilageMixin {
	@WrapOperation(method = "doClick", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/item/ItemStack;copyWithCount(I)Lnet/minecraft/world/item/ItemStack;"))
	private ItemStack eam$dragWithFreshness(ItemStack source, int count, Operation<ItemStack> original, @Local Slot slot) {
		return SpoilageStacks.dragCopy(source, count, slot.getItem(), () -> original.call(source, count));
	}
}
