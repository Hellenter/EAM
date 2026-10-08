package io.github.hellenter.eam.mixin.world.crafting;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractContainerMenu.class)
public interface AbstractContainerMenuAccessor {
	@Invoker("addDataSlot")
	DataSlot eam$addDataSlot(DataSlot slot);

	@Invoker("moveItemStackTo")
	boolean eam$moveItemStackTo(ItemStack stack, int startSlot, int endSlot, boolean backwards);

	@Invoker("dropOrPlaceInInventory")
	static void eam$dropOrPlaceInInventory(Player player, ItemStack stack) {
		throw new AssertionError();
	}
}
