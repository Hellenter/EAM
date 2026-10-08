package io.github.hellenter.eam.mixin.world.crafting;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import io.github.hellenter.eam.world.crafting.CraftingModule;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Clicks on a crafting result start the craft; closing the menu hands out finished crafts. */
@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {
	@WrapOperation(method = "doClick", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;quickMoveStack(Lnet/minecraft/world/entity/player/Player;I)Lnet/minecraft/world/item/ItemStack;",
			ordinal = 1))
	private ItemStack eam$oneCraftPerWait(AbstractContainerMenu menu, Player player, int slotIndex, Operation<ItemStack> original) {
		return CraftingModule.keepQuickMoving(menu, player, slotIndex) ? original.call(menu, player, slotIndex) : ItemStack.EMPTY;
	}

	@Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
	private void eam$startCraft(int slotIndex, int buttonNum, ContainerInput input, Player player, CallbackInfo ci) {
		if (CraftingModule.clickResult((AbstractContainerMenu) (Object) this, slotIndex, input, player)) {
			ci.cancel();
		}
	}

	@Inject(method = "removed", at = @At("HEAD"))
	private void eam$closed(Player player, CallbackInfo ci) {
		CraftingModule.closed((AbstractContainerMenu) (Object) this, player);
	}
}
