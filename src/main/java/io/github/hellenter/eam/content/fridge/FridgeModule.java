package io.github.hellenter.eam.content.fridge;

import io.github.hellenter.eam.Eam;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * Ice-cooled fridges: food inside does not age while the fridge has ice. The blocks are registered in {@code EamBlocks}
 * and how long one ice lasts is in the spoilage settings.
 */
public final class FridgeModule {
	/** What goes in a fridge's ice slots; each item keeps it cold for {@code days_per_ice}. */
	public static final TagKey<Item> ICE = TagKey.create(Registries.ITEM, Eam.id("fridge_ice"));

	public static final ExtendedMenuType<FridgeMenu, FridgeMenu.Size> MENU = Registry.register(BuiltInRegistries.MENU, Eam.id("fridge"),
			new ExtendedMenuType<>(FridgeMenu::new, FridgeMenu.Size.STREAM_CODEC));

	private FridgeModule() {
	}

	/** Loads the class, registering the menu. */
	public static void register() {
	}
}
