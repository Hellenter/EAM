package io.github.hellenter.eam.world.enchanting;

import java.util.List;

/** Prices and bookshelf requirements of chosen enchantments. Pure, unit tested. */
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

	/**
	 * Bookshelves level {@code level} of an enchantment with {@code maxLevel} levels needs. Same table layout as the prices;
	 * a missing row or entry needs three shelves per level.
	 */
	public static int shelvesForLevel(List<List<Integer>> shelves, int maxLevel, int level) {
		if (maxLevel >= 1 && maxLevel <= shelves.size()) {
			List<Integer> row = shelves.get(maxLevel - 1);
			if (level >= 1 && level <= row.size()) {
				return row.get(level - 1);
			}
		}
		return 3 * level;
	}

	/**
	 * Bookshelves an item with {@code totalLevels} enchantment levels in all needs: {@code minShelves} give the first level,
	 * every further shelf one more.
	 */
	public static int shelvesForTotal(int minShelves, int totalLevels) {
		return minShelves + Math.max(0, totalLevels - 1);
	}

	/** Experience levels the same choice costs. */
	public static int levelCost(int resourceCost, int levelsPerResource) {
		return resourceCost * levelsPerResource;
	}

	/**
	 * Whether a player can pay for a set of chosen enchantments.
	 *
	 * @param offensive  offensive resources the player has
	 * @param supportive supportive resources the player has
	 * @param sameItem   whether both prices are paid with the same item, so the player's stock covers their sum
	 */
	public static boolean affordable(Price price, int offensive, int supportive, boolean sameItem, int playerLevel, boolean infinite) {
		if (infinite) {
			return true;
		}
		boolean resources = sameItem
				? offensive >= price.offensive() + price.supportive()
				: offensive >= price.offensive() && supportive >= price.supportive();
		return resources && playerLevel >= price.levels();
	}

	/** Total price of a set of chosen enchantments: offensive and supportive resources and experience levels. */
	public record Price(int offensive, int supportive, int levels) {
		public static final Price FREE = new Price(0, 0, 0);

		public Price plus(boolean offensive, int resources, int levels) {
			return offensive
					? new Price(this.offensive + resources, supportive, this.levels + levels)
					: new Price(this.offensive, supportive + resources, this.levels + levels);
		}
	}
}
