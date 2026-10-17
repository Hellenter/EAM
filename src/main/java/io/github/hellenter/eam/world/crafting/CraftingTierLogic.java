package io.github.hellenter.eam.world.crafting;

import java.util.List;

/**
 * Pure rules for crafting with metals. Every material and table tool has a tier: flint 1, copper and silver 2, iron and ancient metal 3,
 * mithril 4, adamantium (and netherite, its upgrade) 5.
 * Crafting with a material of tier N needs a knife of tier N - 1 or better in the crafting table; tools in the table
 * also make crafting faster, each by its tier's bonus: knives every craft, saws wooden and decorative results, hammers
 * metalwork. Tools below the wear-free tier (flint, copper, silver) lose a point of durability for each craft they help.
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

	/** Whether a table tool of {@code tier} loses durability when it helps a craft. */
	public static boolean wears(int tier, int wearFreeTier) {
		return tier > NONE && tier < wearFreeTier;
	}

	/** Speed tools add: each tool of tier N adds {@code bonuses[N - 1]}. Tiers outside the list add nothing. */
	public static float speedBonus(List<Integer> toolTiers, List<Float> bonuses) {
		float bonus = 0.0F;
		for (int tier : toolTiers) {
			if (tier > NONE && tier <= bonuses.size()) {
				bonus += bonuses.get(tier - 1);
			}
		}
		return bonus;
	}

	/** Factor on the craft time for a speed bonus: the time is divided by one plus the bonus. */
	public static float timeFactor(float speedBonus) {
		return 1.0F / (1.0F + speedBonus);
	}

	/** Factor on the craft time for tools that all apply, see {@link #speedBonus}. */
	public static float timeFactor(List<Integer> toolTiers, List<Float> bonuses) {
		return timeFactor(speedBonus(toolTiers, bonuses));
	}
}
