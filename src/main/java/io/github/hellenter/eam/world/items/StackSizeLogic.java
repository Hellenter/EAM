package io.github.hellenter.eam.world.items;

import java.util.List;

/**
 * MITE-style stack limits. Pure logic: decides a new max stack size from an item's id path and a few
 * facts about it, so it also covers items from other mods that follow vanilla naming. Limits only ever
 * go down, never above the vanilla value.
 */
public final class StackSizeLogic {
	/** Heavy blocks: stone, ores, logs, earth, metal and mineral blocks. */
	public static final int HEAVY_BLOCK = 4;
	/** Every other placeable block. */
	public static final int BLOCK = 16;
	/** Food. */
	public static final int FOOD = 8;
	/** Ingots, gems and raw ore chunks. */
	public static final int INGOT = 16;
	/** Nuggets and other small metal bits. */
	public static final int NUGGET = 32;

	private static final List<String> HEAVY_PARTS = List.of(
			"stone", "cobble", "deepslate", "_ore", "_log", "_wood", "_stem", "_hyphae", "obsidian",
			"granite", "diorite", "andesite", "tuff", "calcite", "basalt", "netherrack", "brick",
			"terracotta", "concrete", "prismarine", "purpur", "quartz", "copper", "anvil",
			"sand", "gravel", "dirt", "mud", "clay", "podzol", "mycelium", "grass_block", "_block"
	);

	/** Blocks that match a heavy name but are small fittings. */
	private static final List<String> LIGHT_PARTS = List.of(
			"button", "pressure_plate", "torch", "lantern", "chain", "bars", "rail", "lever", "trapdoor",
			"door", "sign", "fence", "pane", "bulb", "grate", "carpet", "sapling", "fungus", "roots",
			"dust", "candle", "golem", "head", "skull"
	);

	private static final List<String> INGOT_PARTS = List.of(
			"_ingot", "raw_", "diamond", "emerald", "netherite_scrap", "amethyst_shard", "quartz", "lapis_lazuli"
	);

	private StackSizeLogic() {
	}

	/**
	 * @param path        item id path, e.g. {@code cobblestone}
	 * @param placesBlock the item places a block with the same id (seeds, string, redstone and other
	 *                    items that place a differently named block do not count)
	 * @param food        the item has the {@code food} component
	 * @param vanillaMax  the item's current max stack size
	 * @return the new max stack size, never above {@code vanillaMax}
	 */
	public static int maxStackSize(String path, boolean placesBlock, boolean food, int vanillaMax) {
		if (vanillaMax <= 1) {
			return vanillaMax;
		}
		return Math.min(vanillaMax, limit(path, placesBlock, food, vanillaMax));
	}

	private static int limit(String path, boolean placesBlock, boolean food, int vanillaMax) {
		if (food) {
			return FOOD;
		}
		if (placesBlock) {
			return isHeavyBlock(path) ? HEAVY_BLOCK : BLOCK;
		}
		if (path.endsWith("_nugget")) {
			return NUGGET;
		}
		if (containsAny(path, INGOT_PARTS) && !path.contains("_block") && !path.contains("_ore")) {
			return INGOT;
		}
		return vanillaMax;
	}

	static boolean isHeavyBlock(String path) {
		return containsAny(path, HEAVY_PARTS) && !containsAny(path, LIGHT_PARTS);
	}

	private static boolean containsAny(String path, List<String> parts) {
		for (String part : parts) {
			if (path.contains(part)) {
				return true;
			}
		}
		return false;
	}
}
