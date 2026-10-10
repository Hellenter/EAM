package io.github.hellenter.eam.mixin.content.campfire;

import io.github.hellenter.eam.content.campfire.Overcooking;

import net.fabricmc.fabric.api.item.v1.FabricItem;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.PotionItem;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

/** A drinkable potion used in a crafting grid (water bottle + ash = black dye) leaves its glass bottle. */
@Mixin(PotionItem.class)
abstract class PotionItemMixin implements FabricItem {
	@Override
	public @Nullable ItemStackTemplate getCraftingRemainder(ItemStack stack) {
		return Overcooking.potionRemainder(stack);
	}
}
