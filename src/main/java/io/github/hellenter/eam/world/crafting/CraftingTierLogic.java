package io.github.hellenter.eam.world.crafting;

import java.util.List;

/**
 * Pure rules for crafting with metals. Every material and table tool has a tier: flint 1, copper and silver 2, iron 3.
 * Crafting with a material of tier N needs a tool of tier N - 1 or better in the crafting table; tools in the table
 * also make crafting faster, each by its tier's bonus.
 */
public final class CraftingTierLogic {
	/** No tier: the item is no metal and no table tool. */
	public static final int NONE = 0;

	private CraftingTierLogic() {
	}

	/** Lowest tool tier the table needs to craft with materials whose highest tier is {@code materialTier}. */
	public static int requiredToolTier(int materialTier) {
		return Math.max(NONE, materialTier - 1);
	}

	/** Whether tools whose best tier is {@code bestToolTier} may craft with materials of {@code materialTier}. */
	public static boolean canCraft(int materialTier, int bestToolTier) {
		return bestToolTier >= requiredToolTier(materialTier);
	}

	/**
	 * Factor on the craft time for the tools in the table: each tool of tier N adds {@code bonuses[N - 1]} to the speed,
	 * so the time is divided by one plus their sum. Tiers outside the list add nothing.
	 */
	public static float timeFactor(List<Integer> toolTiers, List<Float> bonuses) {
		float speed = 1.0F;
		for (int tier : toolTiers) {
			if (tier > NONE && tier <= bonuses.size()) {
				speed += bonuses.get(tier - 1);
			}
		}
		return 1.0F / speed;
	}
}
