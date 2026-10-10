package io.github.hellenter.eam.content.campfire;

import io.github.hellenter.eam.content.EamItems;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;

import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Food left on a camp fire after it is cooked: it gets burnt, then turns to ash. Cooked food without a burnt
 * variant (dried kelp) goes straight to ash; what is not food (boiled water) stays as it is.
 */
public final class Overcooking {
	/** Cooked food and what it becomes when it burns. */
	public static final Map<Item, Item> BURNT = Map.of(
			Items.COOKED_BEEF, EamItems.BURNT_BEEF,
			Items.COOKED_PORKCHOP, EamItems.BURNT_PORKCHOP,
			Items.COOKED_MUTTON, EamItems.BURNT_MUTTON,
			Items.COOKED_CHICKEN, EamItems.BURNT_CHICKEN,
			Items.COOKED_RABBIT, EamItems.BURNT_RABBIT,
			Items.COOKED_COD, EamItems.BURNT_COD,
			Items.COOKED_SALMON, EamItems.BURNT_SALMON,
			Items.BAKED_POTATO, EamItems.BURNT_POTATO
	);

	private Overcooking() {
	}

	/** What cooked food becomes when it burns, or null when it does not burn. */
	public static @Nullable ItemStack burn(ItemStack cooked) {
		Item burnt = BURNT.get(cooked.getItem());
		if (burnt != null) {
			return new ItemStack(burnt);
		}
		return cooked.has(DataComponents.FOOD) ? new ItemStack(EamItems.ASH) : null;
	}

	public static ItemStack ash() {
		return new ItemStack(EamItems.ASH);
	}

	/**
	 * A drinkable potion used in crafting (a water bottle for black dye) leaves its glass bottle, like a honey bottle.
	 * Splash and lingering potions keep the vanilla behaviour, so tipped arrows give nothing back.
	 */
	public static @Nullable ItemStackTemplate potionRemainder(ItemStack potion) {
		return potion.is(Items.POTION) ? new ItemStackTemplate(Items.GLASS_BOTTLE) : null;
	}
}
