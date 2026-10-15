package io.github.hellenter.eam.mixin.survival.spoilage;

import io.github.hellenter.eam.survival.spoilage.SpoilageStacks;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Picked-up food brings its spoil times into the slot it stacks onto. */
@Mixin(Inventory.class)
abstract class InventorySpoilageMixin {
	@WrapOperation(method = "addResource(ILnet/minecraft/world/item/ItemStack;)I",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;grow(I)V"))
	private void eam$growWithFreshness(ItemStack target, int amount, Operation<Void> original,
			@Local(argsOnly = true) ItemStack source) {
		SpoilageStacks.growFrom(target, amount, source, original);
	}
}
