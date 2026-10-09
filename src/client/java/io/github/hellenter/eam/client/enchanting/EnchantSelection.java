package io.github.hellenter.eam.client.enchanting;

import io.github.hellenter.eam.world.enchanting.EnchantingOptions;

import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Enchantments and levels the player picked at the open table, in the order they were picked. Shared by both panels. */
final class EnchantSelection {
	private final Map<Holder<Enchantment>, Integer> levels = new LinkedHashMap<>();

	/** Drops picks the item in the table no longer allows, e.g. after it was enchanted or swapped. */
	void refresh(List<EnchantingOptions.Option> options) {
		List<Holder<Enchantment>> kept = new ArrayList<>();
		Iterator<Map.Entry<Holder<Enchantment>, Integer>> it = levels.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Holder<Enchantment>, Integer> entry = it.next();
			EnchantingOptions.Option option = EnchantingOptions.find(options, entry.getKey());
			if (option == null || !option.allows(entry.getValue()) || conflict(kept, entry.getKey()) != null) {
				it.remove();
			} else {
				kept.add(entry.getKey());
			}
		}
	}

	/** The picked level, 0 if not picked. */
	int level(EnchantingOptions.Option option) {
		return levels.getOrDefault(option.enchantment(), 0);
	}

	boolean isEmpty() {
		return levels.isEmpty();
	}

	/** A picked enchantment that cannot go together with {@code option}, or null. */
	@Nullable Holder<Enchantment> blockedBy(EnchantingOptions.Option option) {
		return conflict(levels.keySet(), option.enchantment());
	}

	private static @Nullable Holder<Enchantment> conflict(Iterable<Holder<Enchantment>> picked, Holder<Enchantment> enchantment) {
		for (Holder<Enchantment> other : picked) {
			if (!other.equals(enchantment) && !Enchantment.areCompatible(other, enchantment)) {
				return other;
			}
		}
		return null;
	}

	/** The level a press of + gives. */
	int nextLevel(EnchantingOptions.Option option) {
		int level = level(option);
		return level == 0 ? option.currentLevel() + 1 : level + 1;
	}

	boolean canRaise(EnchantingOptions.Option option) {
		return blockedBy(option) == null && nextLevel(option) <= option.maxLevel();
	}

	void raise(EnchantingOptions.Option option) {
		if (canRaise(option)) {
			levels.put(option.enchantment(), nextLevel(option));
		}
	}

	/** The picks as they would be after a press of + on {@code option}. */
	List<EnchantingOptions.Choice> choicesRaised(EnchantingOptions.Option option) {
		Map<Holder<Enchantment>, Integer> raised = new LinkedHashMap<>(levels);
		raised.put(option.enchantment(), nextLevel(option));
		List<EnchantingOptions.Choice> choices = new ArrayList<>();
		raised.forEach((enchantment, level) -> choices.add(new EnchantingOptions.Choice(enchantment, level)));
		return choices;
	}

	/** One level down; below the lowest level the item can get, the enchantment is unpicked. */
	void lower(EnchantingOptions.Option option) {
		int level = level(option) - 1;
		if (level <= option.currentLevel()) {
			levels.remove(option.enchantment());
		} else {
			levels.put(option.enchantment(), level);
		}
	}

	void toggle(EnchantingOptions.Option option) {
		if (level(option) > 0) {
			levels.remove(option.enchantment());
		} else {
			raise(option);
		}
	}

	List<EnchantingOptions.Choice> choices() {
		List<EnchantingOptions.Choice> choices = new ArrayList<>();
		levels.forEach((enchantment, level) -> choices.add(new EnchantingOptions.Choice(enchantment, level)));
		return choices;
	}
}
