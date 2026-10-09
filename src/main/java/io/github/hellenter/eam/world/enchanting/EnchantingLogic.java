package io.github.hellenter.eam.world.enchanting;

import java.util.List;

/** Prices of a chosen enchantment. Pure, unit tested. */
public final class EnchantingLogic {
	private EnchantingLogic() {
	}

	/**
	 * Resources level {@code level} of an enchantment with {@code maxLevel} levels costs. The table has a row per max
	 * level, starting at 1; a missing row or entry costs the level itself.
	 */
	public static int resourceCost(List<List<Integer>> costs, int maxLevel, int level) {
		if (maxLevel >= 1 && maxLevel <= costs.size()) {
			List<Integer> row = costs.get(maxLevel - 1);
			if (level >= 1 && level <= row.size()) {
				return row.get(level - 1);
			}
		}
		return level;
	}

	/** Experience levels the same choice costs. */
	public static int levelCost(int resourceCost, int levelsPerResource) {
		return resourceCost * levelsPerResource;
	}

	/** Whether a player with {@code resources} of the right item and {@code playerLevel} levels can pay. */
	public static boolean affordable(int resources, int resourceCost, int playerLevel, int levelCost, boolean infinite) {
		return infinite || resources >= resourceCost && playerLevel >= levelCost;
	}
}
