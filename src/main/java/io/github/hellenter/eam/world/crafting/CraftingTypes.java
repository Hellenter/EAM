package io.github.hellenter.eam.world.crafting;

import io.github.hellenter.eam.Eam;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Set;

/** The crafting table's block entity and menu. Registered whatever the config says, so client and server match. */
public final class CraftingTypes {
	public static final BlockEntityType<CraftingTableBlockEntity> CRAFTING_TABLE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, Eam.id("crafting_table"),
			new BlockEntityType<>(CraftingTableBlockEntity::new, Set.of(Blocks.CRAFTING_TABLE)));

	public static final MenuType<CraftingTableMenu> MENU = Registry.register(
			BuiltInRegistries.MENU, Eam.id("crafting_table"),
			new MenuType<>(CraftingTableMenu::new, FeatureFlags.VANILLA_SET));

	private CraftingTypes() {
	}

	static void register() {
		// Loads the class, which registers the types above.
	}
}
