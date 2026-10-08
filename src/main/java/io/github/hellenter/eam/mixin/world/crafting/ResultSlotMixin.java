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

/** Only finished crafts can be taken from the result slot, and taking them does not craft again. */
@Mixin(ResultSlot.class)
public abstract class ResultSlotMixin extends Slot {
	private ResultSlotMixin(Container container, int slot, int x, int y) {
		super(container, slot, x, y);
	}

	@Override
	public boolean mayPickup(Player player) {
		return super.mayPickup(player) && CraftingModule.mayTakeResult(player);
	}

	@Inject(method = "onTake", at = @At("HEAD"), cancellable = true)
	private void eam$takeOutput(Player player, ItemStack carried, CallbackInfo ci) {
		if (CraftingModule.takeOutput(player, (ResultSlot) (Object) this)) {
			ci.cancel();
		}
	}
}
