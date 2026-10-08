package io.github.hellenter.eam.mixin.world.crafting;

import io.github.hellenter.eam.world.crafting.CraftingModule;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The shared result update of the inventory grid and the crafting table: tool check and craft timer restart. */
@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin {
	@ModifyVariable(method = "slotChangedCraftingGrid", ordinal = 0, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/inventory/ResultContainer;setItem(ILnet/minecraft/world/item/ItemStack;)V"))
	private static ItemStack eam$needsTool(ItemStack result, AbstractContainerMenu menu) {
		return CraftingModule.result(menu, result);
	}

	@Inject(method = "slotChangedCraftingGrid", at = @At("TAIL"))
	private static void eam$restartTimer(AbstractContainerMenu menu, ServerLevel level, Player player, CraftingContainer container,
			ResultContainer resultSlots, @Nullable RecipeHolder<CraftingRecipe> recipeHint, CallbackInfo ci) {
		CraftingModule.resultChanged(menu, player, container, resultSlots);
	}
}
