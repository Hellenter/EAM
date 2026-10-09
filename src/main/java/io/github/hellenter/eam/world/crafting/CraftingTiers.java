package io.github.hellenter.eam.world.crafting;

import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Reads tiers from item tags; the rules are in {@link CraftingTierLogic}. */
public final class CraftingTiers {
	private CraftingTiers() {
	}

	/** Tier of {@code stack} by the tier tags {@code tiers} (first is tier 1), or {@link CraftingTierLogic#NONE}. */
	public static int tier(ItemStack stack, List<TagKey<Item>> tiers) {
		for (int tier = tiers.size(); tier > 0; tier--) {
			if (stack.is(tiers.get(tier - 1))) {
				return tier;
			}
		}
		return CraftingTierLogic.NONE;
	}

	/** Tier of a crafting material, or {@link CraftingTierLogic#NONE}. */
	public static int materialTier(ItemStack stack) {
		return tier(stack, CraftingTags.MATERIAL_TIERS);
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

	/** Whether the grid holds metal ({@link CraftingTags#METALS}). */
	public static boolean hasMetal(Container grid) {
		for (ItemStack stack : grid) {
			if (stack.is(CraftingTags.METALS)) {
				return true;
			}
		}
		return false;
	}

	/** Tiers of the tools of one kind in a crafting table, one per tool; {@code tiers} are that kind's tier tags. */
	public static List<Integer> toolTiers(Container tools, List<TagKey<Item>> tiers) {
		List<Integer> result = new ArrayList<>();
		for (ItemStack stack : tools) {
			int tier = tier(stack, tiers);
			if (tier > CraftingTierLogic.NONE) {
				result.add(tier);
			}
		}
		return result;
	}
}
