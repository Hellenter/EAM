package io.github.hellenter.eam.world.enchanting;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Enchantments a player can choose for an item at the table. Both sides build the same list: the client to show it, the
 * server to check a choice.
 */
public final class EnchantingOptions {
	/**
	 * One enchantment on offer.
	 *
	 * @param currentLevel the level the item already has, 0 if none; the player picks a level above it
	 */
	public record Option(Holder<Enchantment> enchantment, boolean offensive, int currentLevel) {
		public int maxLevel() {
			return enchantment.value().getMaxLevel();
		}

		public int resourceCost(EnchantingSettings settings, int level) {
			return EnchantingLogic.resourceCost(settings.costs(), maxLevel(), level);
		}

		public int levelCost(EnchantingSettings settings, int level) {
			return EnchantingLogic.levelCost(resourceCost(settings, level), settings.levelsPerResource());
		}

		public boolean allows(int level) {
			return level > currentLevel && level <= maxLevel();
		}
	}

	private EnchantingOptions() {
	}

	/**
	 * Offensive enchantments first, then supportive ones. Like the vanilla table, a book takes any of them and other items
	 * only their primary ones; already enchanted items can get more, as long as the enchantments are compatible.
	 */
	public static List<Option> forItem(ItemStack stack, HolderLookup.RegistryLookup<Enchantment> enchantments) {
		List<Option> options = new ArrayList<>();
		if (stack.isEmpty()) {
			return options;
		}
		boolean book = stack.is(Items.BOOK) || stack.is(Items.ENCHANTED_BOOK);
		if (!book && !stack.has(DataComponents.ENCHANTABLE)) {
			return options;
		}
		ItemEnchantments existing = EnchantmentHelper.getEnchantmentsForCrafting(stack);
		Set<Holder<Enchantment>> seen = new HashSet<>();
		add(options, seen, stack, book, existing, enchantments, EnchantingTags.OFFENSIVE, true);
		add(options, seen, stack, book, existing, enchantments, EnchantingTags.SUPPORTIVE, false);
		return options;
	}

	private static void add(List<Option> options, Set<Holder<Enchantment>> seen, ItemStack stack, boolean book,
			ItemEnchantments existing, HolderLookup.RegistryLookup<Enchantment> enchantments, TagKey<Enchantment> tag, boolean offensive) {
		for (Holder<Enchantment> holder : enchantments.get(tag).map(HolderSet.Named::stream).orElseGet(Stream::empty).toList()) {
			if (!seen.add(holder) || !book && !holder.value().isPrimaryItem(stack)) {
				continue;
			}
			int current = existing.getLevel(holder);
			if (current >= holder.value().getMaxLevel()) {
				continue;
			}
			Set<Holder<Enchantment>> others = new HashSet<>(existing.keySet());
			others.remove(holder);
			if (EnchantmentHelper.isEnchantmentCompatible(others, holder)) {
				options.add(new Option(holder, offensive, current));
			}
		}
	}

	/** The option for {@code enchantment}, or null if it is not on offer for this item. */
	public static Option find(List<Option> options, Holder<Enchantment> enchantment) {
		for (Option option : options) {
			if (option.enchantment().equals(enchantment)) {
				return option;
			}
		}
		return null;
	}
}
