package io.github.hellenter.eam.mixin.world.crafting;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import io.github.hellenter.eam.world.crafting.CraftingModule;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Shift-click on a crafting result takes one craft, not a stack of them: the next one has to wait its time. */
@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {
	@WrapOperation(method = "doClick", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;quickMoveStack(Lnet/minecraft/world/entity/player/Player;I)Lnet/minecraft/world/item/ItemStack;",
			ordinal = 1))
	private ItemStack eam$oneCraftPerWait(AbstractContainerMenu menu, Player player, int slotIndex, Operation<ItemStack> original) {
		return CraftingModule.keepQuickMoving(menu, player, slotIndex) ? original.call(menu, player, slotIndex) : ItemStack.EMPTY;
	}
}
