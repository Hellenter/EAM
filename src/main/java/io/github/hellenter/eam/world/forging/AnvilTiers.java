package io.github.hellenter.eam.world.forging;

import io.github.hellenter.eam.content.anvil.MetalAnvilBlock;
import io.github.hellenter.eam.world.crafting.CraftingTags;
import io.github.hellenter.eam.world.crafting.CraftingTierLogic;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Repairable;
import net.minecraft.world.level.block.state.BlockState;

/** Reads anvil, item and hammer tiers; the rules are in {@link ForgingLogic}. */
public final class AnvilTiers {
	/** The vanilla anvil is the iron anvil. */
	public static final int IRON = 3;

	private AnvilTiers() {
	}

	public static int anvilTier(BlockState state) {
		return state.getBlock() instanceof MetalAnvilBlock anvil ? anvil.tier() : IRON;
	}

	/**
	 * Tier of an item on the anvil: the highest crafting material tier among what repairs it (flint 1, copper and silver
	 * 2, iron 3). Items repaired by nothing tiered, such as books or leather, have none.
	 */
	public static int itemTier(ItemStack stack) {
		Repairable repairable = stack.get(DataComponents.REPAIRABLE);
		if (repairable == null) {
			return CraftingTierLogic.NONE;
		}
		HolderSet<Item> items = repairable.items();
		int tier = CraftingTierLogic.NONE;
		for (Holder<Item> item : items) {
			for (int t = CraftingTags.MATERIAL_TIERS.size(); t > tier; t--) {
				if (item.is(CraftingTags.MATERIAL_TIERS.get(t - 1))) {
					tier = t;
					break;
				}
			}
		}
		return tier;
	}

	public static int hammerTier(ItemStack stack) {
		for (int tier = ForgingTags.HAMMER_TIERS.size(); tier > 0; tier--) {
			if (stack.is(ForgingTags.HAMMER_TIERS.get(tier - 1))) {
				return tier;
			}
		}
		return CraftingTierLogic.NONE;
	}
}
