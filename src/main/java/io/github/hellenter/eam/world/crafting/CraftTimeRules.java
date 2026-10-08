package io.github.hellenter.eam.world.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * How long crafting takes, read from {@code data/eam/crafting/times.json} so datapacks can override it. The first rule
 * whose tag holds the result wins; a result no rule matches takes {@code defaultSeconds}.
 *
 * @param defaultSeconds           seconds for anything no rule matches
 * @param craftingTableMultiplier  times on a crafting table are multiplied by this (0.5 = twice as fast as by hand)
 * @param rules                    tag rules, checked in order
 * @param toolSpeedBonus           speed each tool in the crafting table adds, by tool tier (first entry is tier 1)
 */
public record CraftTimeRules(float defaultSeconds, float craftingTableMultiplier, List<Rule> rules, List<Float> toolSpeedBonus) {
	/**
	 * @param items         results this rule covers
	 * @param seconds       seconds per craft, or per item in the grid when {@code perIngredient}
	 * @param perIngredient armor: every ingot, gem or leather in the recipe adds {@code seconds}
	 */
	public record Rule(TagKey<Item> items, float seconds, boolean perIngredient) {
		public static final Codec<Rule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				TagKey.codec(Registries.ITEM).fieldOf("tag").forGetter(Rule::items),
				Codec.floatRange(0.0F, 1000.0F).fieldOf("seconds").forGetter(Rule::seconds),
				Codec.BOOL.optionalFieldOf("per_ingredient", false).forGetter(Rule::perIngredient)
		).apply(instance, Rule::new));
	}

	public static final CraftTimeRules DEFAULT = new CraftTimeRules(1.0F, 0.5F, List.of(
			new Rule(CraftingTags.TEN_SECONDS, 10.0F, false),
			new Rule(CraftingTags.ARMOR, 5.0F, true),
			new Rule(CraftingTags.FIVE_SECONDS, 5.0F, false)
	), List.of(0.15F, 0.25F, 0.35F));

	public static final Codec<CraftTimeRules> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.floatRange(0.0F, 1000.0F).fieldOf("default_seconds").orElse(DEFAULT.defaultSeconds).forGetter(CraftTimeRules::defaultSeconds),
			Codec.floatRange(0.0F, 10.0F).fieldOf("crafting_table_multiplier").orElse(DEFAULT.craftingTableMultiplier).forGetter(CraftTimeRules::craftingTableMultiplier),
			Rule.CODEC.listOf().fieldOf("rules").orElse(DEFAULT.rules).forGetter(CraftTimeRules::rules),
			Codec.floatRange(0.0F, 10.0F).listOf().fieldOf("tool_speed_bonus").orElse(DEFAULT.toolSpeedBonus).forGetter(CraftTimeRules::toolSpeedBonus)
	).apply(instance, CraftTimeRules::new));

	/**
	 * Ticks one craft of {@code result} takes with {@code ingredients} items in the grid; {@code toolTiers} are the tools
	 * in the crafting table, null for the inventory grid.
	 */
	public int ticks(ItemStack result, int ingredients, @Nullable List<Integer> toolTiers) {
		float multiplier = toolTiers == null ? 1.0F : craftingTableMultiplier * CraftingTierLogic.timeFactor(toolTiers, toolSpeedBonus);
		for (Rule rule : rules) {
			if (result.is(rule.items())) {
				return CraftTimeLogic.ticks(rule.seconds(), rule.perIngredient(), ingredients, multiplier);
			}
		}
		return CraftTimeLogic.ticks(defaultSeconds, false, ingredients, multiplier);
	}
}
