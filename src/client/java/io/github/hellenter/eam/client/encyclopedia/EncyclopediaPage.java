package io.github.hellenter.eam.client.encyclopedia;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.world.structures.VillageModule;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * One page of the encyclopedia: a stage of progression or a boss. Its title, subtitle and text are translations under
 * {@code encyclopedia.eam.<key>}; {@code icons} are the items the page is about, shown in a row with their tooltips.
 * {@code advancement}, when set, marks the page as reached once the player has earned it.
 */
record EncyclopediaPage(String key, Kind kind, ItemLike icon, List<ItemLike> icons, @Nullable Identifier advancement) {
	enum Kind {
		STAGE,
		BOSS
	}

	static final List<EncyclopediaPage> PAGES = List.of(
			stage("stone_age", EamItems.FLINT_KNIFE, Eam.id("flint_knife"),
					EamItems.PEBBLE, EamItems.FLINT_SHARD, EamItems.FLINT_KNIFE, EamItems.FLINT_HATCHET, Items.CRAFTING_TABLE,
					EamItems.CAMP_FIRE, EamItems.CLAY_OVEN, EamItems.LEATHER_FLASK),
			stage("copper_silver", Items.COPPER_PICKAXE, Eam.id("copper_pickaxe"),
					EamItems.SIEVE, Items.COPPER_NUGGET, Items.COPPER_PICKAXE, EamItems.COPPER_KNIFE, EamItems.COPPER_SAW,
					EamItems.COPPER_ANVIL, EamItems.SILVER_INGOT, EamItems.SILVER_BREWING_STAND),
			stage("iron", Items.IRON_CHESTPLATE, VillageModule.UNLOCK_ADVANCEMENT,
					Items.BLAST_FURNACE, EamItems.HOT_IRON_INGOT, EamItems.IRON_PLATE, Items.ANVIL, Items.IRON_PICKAXE,
					Items.IRON_CHESTPLATE, Items.EMERALD),
			stage("ancient_metal", EamItems.ANCIENT_METAL_INGOT, null,
					EamItems.ANCIENT_METAL_SWORD, EamItems.ANCIENT_METAL_CHAINMAIL_CHESTPLATE, EamItems.ANCIENT_METAL_INGOT,
					EamItems.FERMENTED_ENDER_EYE, EamItems.CRYPT_MAP),
			stage("mithril", EamItems.MITHRIL_INGOT, Eam.id("mithril"),
					Items.OBSIDIAN, EamItems.OBSIDIAN_BLAST_FURNACE, EamItems.MITHRIL_INGOT, EamItems.MITHRIL_PICKAXE,
					EamItems.MITHRIL_ANVIL, EamItems.MITHRIL_CHESTPLATE),
			stage("underworld", EamItems.BLACK_TALISMAN, null,
					EamItems.BLACK_TALISMAN, Items.OBSIDIAN, EamItems.DEEP_BEDROCK, EamItems.KOSCHEI_NEEDLE),
			stage("adamantium", EamItems.ADAMANTIUM_INGOT, Eam.id("adamantium"),
					EamItems.RAW_ADAMANTIUM, EamItems.ADAMANTIUM_INGOT, EamItems.ADAMANTIUM_PICKAXE, EamItems.ADAMANTIUM_ANVIL,
					EamItems.ADAMANTIUM_CHESTPLATE, Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, Items.NETHERITE_INGOT),
			boss("pillager_king", EamItems.PILLAGER_KING_SPAWN_EGG, null,
					EamItems.KING_CASTLE_MAP, EamItems.PILLAGER_KING_HELM, EamItems.CRYPT_MAP, Items.DIAMOND),
			boss("fallen_lord", EamItems.FALLEN_LORD_SPAWN_EGG, Eam.id("fallen_lord"),
					EamItems.CRYPT_MAP, EamItems.FERMENTED_ENDER_EYE, EamItems.BLACK_TALISMAN, EamItems.MITHRIL_HELMET),
			boss("koschei", EamItems.KOSCHEI_SPAWN_EGG, null,
					EamItems.KOSCHEI_NEEDLE, EamItems.DEEP_BEDROCK),
			boss("baba_yaga", EamItems.BABA_YAGA_SPAWN_EGG, null,
					Items.ENCHANTED_BOOK, EamItems.ASH, EamItems.YAGA_GRAVE_MAP)
	);

	private static EncyclopediaPage stage(String key, ItemLike icon, @Nullable Identifier advancement, ItemLike... icons) {
		return new EncyclopediaPage(key, Kind.STAGE, icon, List.of(icons), advancement);
	}

	private static EncyclopediaPage boss(String key, ItemLike icon, @Nullable Identifier advancement, ItemLike... icons) {
		return new EncyclopediaPage(key, Kind.BOSS, icon, List.of(icons), advancement);
	}

	String translationKey(String part) {
		return "encyclopedia.eam." + key + "." + part;
	}
}
