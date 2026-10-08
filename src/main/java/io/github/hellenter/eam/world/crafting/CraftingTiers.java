package io.github.hellenter.eam.world.crafting;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Reads tiers from item tags; the rules are in {@link CraftingTierLogic}. */
public final class CraftingTiers {
	private CraftingTiers() {
	}

	/** Tier of a crafting table tool, or {@link CraftingTierLogic#NONE}. */
	public static int toolTier(ItemStack stack) {
		for (int tier = CraftingTags.TOOL_TIERS.size(); tier > 0; tier--) {
			if (stack.is(CraftingTags.TOOL_TIERS.get(tier - 1))) {
				return tier;
			}
		}
		return CraftingTierLogic.NONE;
	}

	/** Tier of a crafting material, or {@link CraftingTierLogic#NONE}. */
	public static int materialTier(ItemStack stack) {
		for (int tier = CraftingTags.MATERIAL_TIERS.size(); tier > 0; tier--) {
			if (stack.is(CraftingTags.MATERIAL_TIERS.get(tier - 1))) {
				return tier;
			}
		}
		return CraftingTierLogic.NONE;
	}

	/**
	 * Highest material tier in the grid. A result tagged {@link CraftingTags#TIER_FREE} (ingots, nuggets, metal blocks)
	 * needs no tool, so metal can be packed and unpacked anywhere.
	 */
	public static int materialTier(Container grid, ItemStack result) {
		if (result.isEmpty() || result.is(CraftingTags.TIER_FREE)) {
			return CraftingTierLogic.NONE;
		}
		int tier = CraftingTierLogic.NONE;
		for (ItemStack stack : grid) {
			tier = Math.max(tier, materialTier(stack));
		}
		return tier;
	}

	/** Tiers of the tools in a crafting table, one per tool. */
	public static List<Integer> toolTiers(Container tools) {
		List<Integer> tiers = new ArrayList<>();
		for (ItemStack stack : tools) {
			int tier = toolTier(stack);
			if (tier > CraftingTierLogic.NONE) {
				tiers.add(tier);
			}
		}
		return tiers;
	}
}
