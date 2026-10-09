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
 * @param toolSpeedBonus           speed each knife in the crafting table adds to every craft, by tier (first entry is tier 1)
 * @param sawSpeedBonus            speed each saw adds to wooden and decorative results ({@link CraftingTags#SAW_RESULTS})
 * @param hammerSpeedBonus         speed each hammer adds to crafts with metal in the grid ({@link CraftingTags#METALS})
 */
public record CraftTimeRules(float defaultSeconds, float craftingTableMultiplier, List<Rule> rules, List<Float> toolSpeedBonus,
		List<Float> sawSpeedBonus, List<Float> hammerSpeedBonus) {
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
	), List.of(0.15F, 0.25F, 0.35F, 0.45F), List.of(0.0F, 0.35F, 0.5F, 0.65F), List.of(0.0F, 0.25F, 0.5F, 0.75F));

	public static final Codec<CraftTimeRules> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.floatRange(0.0F, 1000.0F).fieldOf("default_seconds").orElse(DEFAULT.defaultSeconds).forGetter(CraftTimeRules::defaultSeconds),
			Codec.floatRange(0.0F, 10.0F).fieldOf("crafting_table_multiplier").orElse(DEFAULT.craftingTableMultiplier).forGetter(CraftTimeRules::craftingTableMultiplier),
			Rule.CODEC.listOf().fieldOf("rules").orElse(DEFAULT.rules).forGetter(CraftTimeRules::rules),
			Codec.floatRange(0.0F, 10.0F).listOf().fieldOf("tool_speed_bonus").orElse(DEFAULT.toolSpeedBonus).forGetter(CraftTimeRules::toolSpeedBonus),
			Codec.floatRange(0.0F, 10.0F).listOf().fieldOf("saw_speed_bonus").orElse(DEFAULT.sawSpeedBonus).forGetter(CraftTimeRules::sawSpeedBonus),
			Codec.floatRange(0.0F, 10.0F).listOf().fieldOf("hammer_speed_bonus").orElse(DEFAULT.hammerSpeedBonus).forGetter(CraftTimeRules::hammerSpeedBonus)
	).apply(instance, CraftTimeRules::new));

	/**
	 * Ticks one craft of {@code result} takes with {@code ingredients} items in the grid; {@code tools} are the tools in
	 * the crafting table, null for the inventory grid.
	 */
	public int ticks(ItemStack result, int ingredients, @Nullable TableTools tools) {
		float multiplier = tools == null ? 1.0F : craftingTableMultiplier * CraftingTierLogic.timeFactor(speedBonus(result, tools));
		for (Rule rule : rules) {
			if (result.is(rule.items())) {
				return CraftTimeLogic.ticks(rule.seconds(), rule.perIngredient(), ingredients, multiplier);
			}
		}
		return CraftTimeLogic.ticks(defaultSeconds, false, ingredients, multiplier);
	}

	/** Speed the table's tools add to crafting {@code result}: knives always, saws and hammers only for their crafts. */
	public float speedBonus(ItemStack result, TableTools tools) {
		float bonus = CraftingTierLogic.speedBonus(tools.knives(), toolSpeedBonus);
		if (result.is(CraftingTags.SAW_RESULTS)) {
			bonus += CraftingTierLogic.speedBonus(tools.saws(), sawSpeedBonus);
		}
		if (tools.metal()) {
			bonus += CraftingTierLogic.speedBonus(tools.hammers(), hammerSpeedBonus);
		}
		return bonus;
	}
}
