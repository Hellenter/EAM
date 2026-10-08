package io.github.hellenter.eam.mixin.world.crafting;

import io.github.hellenter.eam.world.crafting.CraftingModule;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The crafting result can only be taken once the craft has run; taking it starts the next craft. */
@Mixin(ResultSlot.class)
public abstract class ResultSlotMixin extends Slot {
	private ResultSlotMixin(Container container, int slot, int x, int y) {
		super(container, slot, x, y);
	}

	@Override
	public boolean mayPickup(Player player) {
		return super.mayPickup(player) && CraftingModule.mayTakeResult(player);
	}

	@Inject(method = "onTake", at = @At("TAIL"))
	private void eam$crafted(Player player, ItemStack carried, CallbackInfo ci) {
		CraftingModule.crafted(player);
	}
}
