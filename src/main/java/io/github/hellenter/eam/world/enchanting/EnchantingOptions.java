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

import org.jspecify.annotations.Nullable;

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

		public int shelves(EnchantingSettings settings, int level) {
			return settings.shelvesFor(maxLevel(), level);
		}

		public boolean allows(int level) {
			return level > currentLevel && level <= maxLevel();
		}
	}

	/** An enchantment and the level the player wants it at, as the client sends it. */
	public record Choice(Holder<Enchantment> enchantment, int level) {
	}

	/** A choice matched to its option. */
	public record Pick(Option option, int level) {
	}

	private EnchantingOptions() {
	}

	/**
	 * Offensive enchantments first, then supportive ones. A book takes any of them and other items every one they support,
	 * the same set an anvil applies from books; already enchanted items can get more, as long as the enchantments are
	 * compatible.
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
			if (!seen.add(holder) || !book && !holder.value().isSupportedItem(stack)) {
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

	/**
	 * The options for {@code choices}, or null unless every choice is on offer at its level, no enchantment is chosen
	 * twice and the chosen enchantments are compatible with each other.
	 */
	public static @Nullable List<Pick> check(List<Option> options, List<Choice> choices) {
		if (choices.isEmpty()) {
			return null;
		}
		List<Pick> picks = new ArrayList<>();
		Set<Holder<Enchantment>> chosen = new HashSet<>();
		for (Choice choice : choices) {
			Option option = find(options, choice.enchantment());
			if (option == null || !option.allows(choice.level()) || !EnchantmentHelper.isEnchantmentCompatible(chosen, choice.enchantment())) {
				return null;
			}
			chosen.add(choice.enchantment());
			picks.add(new Pick(option, choice.level()));
		}
		return picks;
	}

	/** What the picked enchantments cost together. */
	public static EnchantingLogic.Price price(EnchantingSettings settings, List<Pick> picks) {
		EnchantingLogic.Price price = EnchantingLogic.Price.FREE;
		for (Pick pick : picks) {
			Option option = pick.option();
			price = price.plus(option.offensive(), option.resourceCost(settings, pick.level()), option.levelCost(settings, pick.level()));
		}
		return price;
	}

	/**
	 * Bookshelves around the table needed to give {@code stack} the picked enchantments: at least the minimum, one more per
	 * level the item ends up with in all past the first (levels it already has count too), and what the highest picked
	 * level of each enchantment needs.
	 */
	public static int requiredShelves(EnchantingSettings settings, ItemStack stack, List<Pick> picks) {
		int total = totalLevels(stack);
		int required = settings.minShelves();
		for (Pick pick : picks) {
			total += pick.level() - pick.option().currentLevel();
			required = Math.max(required, pick.option().shelves(settings, pick.level()));
		}
		return Math.max(required, EnchantingLogic.shelvesForTotal(settings.minShelves(), total));
	}

	/** Enchantment levels the item has in all. */
	public static int totalLevels(ItemStack stack) {
		int total = 0;
		for (var entry : EnchantmentHelper.getEnchantmentsForCrafting(stack).entrySet()) {
			total += entry.getIntValue();
		}
		return total;
	}

	/** The option for {@code enchantment}, or null if it is not on offer for this item. */
	public static @Nullable Option find(List<Option> options, Holder<Enchantment> enchantment) {
		for (Option option : options) {
			if (option.enchantment().equals(enchantment)) {
				return option;
			}
		}
		return null;
	}
}
