package io.github.hellenter.eam.world.items;

import java.util.List;
import java.util.Set;

/**
 * MITE-style stack limits. Pure logic: decides a new max stack size from an item's id path and a few
 * facts about it, so it also covers items from other mods that follow vanilla naming. Limits never go
 * above the vanilla value, except for potions, which stack where vanilla keeps them single.
 */
public final class StackSizeLogic {
	/** Every placeable block. */
	public static final int BLOCK = 8;
	/** Food. */
	public static final int FOOD = 8;
	/** Ingots, gems and raw ore chunks. */
	public static final int INGOT = 16;
	/** Nuggets. */
	public static final int NUGGET = 64;
	/** Potions, including splash and lingering ones. */
	public static final int POTION = 4;
	/** Everything else: sticks, seeds, redstone, arrows and other small items. */
	public static final int SMALL_ITEM = 32;

	private static final Set<String> POTIONS = Set.of("potion", "splash_potion", "lingering_potion");

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
	 * @return the new max stack size
	 */
	public static int maxStackSize(String path, boolean placesBlock, boolean food, int vanillaMax) {
		if (POTIONS.contains(path)) {
			return POTION;
		}
		if (vanillaMax <= 1) {
			return vanillaMax;
		}
		return Math.min(vanillaMax, limit(path, placesBlock, food));
	}

	private static int limit(String path, boolean placesBlock, boolean food) {
		if (food) {
			return FOOD;
		}
		if (placesBlock) {
			return BLOCK;
		}
		if (path.endsWith("_nugget")) {
			return NUGGET;
		}
		if (INGOT_PARTS.stream().anyMatch(path::contains)) {
			return INGOT;
		}
		return SMALL_ITEM;
	}
}
