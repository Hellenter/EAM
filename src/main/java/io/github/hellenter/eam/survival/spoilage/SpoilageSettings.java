package io.github.hellenter.eam.survival.spoilage;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamItems;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Balance numbers for food spoilage and fridges, read from {@code data/eam/spoilage/settings.json} so datapacks can
 * override them.
 *
 * @param rules      what spoils, how fast and into what; the first rule whose tag holds an item wins
 * @param daysPerIce how long one ice item keeps a fridge cold
 * @param climate    how the place a fridge stands in changes how long its ice lasts
 * @param driedMultiplier how many times longer dried food keeps than fresh food of its rule
 */
public record SpoilageSettings(List<Rule> rules, double daysPerIce, Climate climate, double driedMultiplier) {
	/** Raw meat and fish: five days, then rotten flesh. */
	public static final TagKey<Item> SPOILS_FAST = TagKey.create(Registries.ITEM, Eam.id("spoils_fast"));
	/** Fruit, vegetables and cooked food: ten days, then rotten food. */
	public static final TagKey<Item> SPOILS = TagKey.create(Registries.ITEM, Eam.id("spoils"));

	public static final SpoilageSettings DEFAULT = new SpoilageSettings(List.of(
			new Rule(SPOILS_FAST, 5.0, Items.ROTTEN_FLESH),
			new Rule(SPOILS, 10.0, EamItems.ROTTEN_FOOD)), 1.0, Climate.DEFAULT, 5.0);

	public static final Codec<SpoilageSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Rule.CODEC.listOf().fieldOf("rules").orElse(DEFAULT.rules).forGetter(SpoilageSettings::rules),
			Codec.doubleRange(0.01, 1000.0).fieldOf("days_per_ice").orElse(DEFAULT.daysPerIce).forGetter(SpoilageSettings::daysPerIce),
			Climate.CODEC.fieldOf("climate").orElse(DEFAULT.climate).forGetter(SpoilageSettings::climate),
			Codec.doubleRange(1.0, 1000.0).fieldOf("dried_multiplier").orElse(DEFAULT.driedMultiplier).forGetter(SpoilageSettings::driedMultiplier)
	).apply(instance, SpoilageSettings::new));

	/** The rule {@code stack} spoils by, or null when it keeps. */
	public @Nullable Rule rule(ItemStack stack) {
		for (Rule rule : rules) {
			if (stack.is(rule.items)) return rule;
		}
		return null;
	}

	public long ticksPerIce() {
		return SpoilageLogic.lifetime(daysPerIce);
	}

	/** How long one ice lasts in a place where it lasts {@code factor} times as long as usual, at least one tick. */
	public long ticksPerIce(double factor) {
		return Math.max(1L, Math.round(ticksPerIce() * factor));
	}

	/**
	 * Ice lasts {@code hotFactor} times as long in a hot biome (the Nether's are all hot) and {@code coldFactor} times
	 * as long in a snowy biome or next to snow or ice blocks ({@code eam:fridge_chillers}). Both apply to a fridge with
	 * snow next to it in a hot biome.
	 *
	 * @param hotTemperature  biome base temperature above which a biome counts as hot
	 * @param coldTemperature biome base temperature below which a biome counts as snowy
	 * @param hotFactor       how long ice lasts in a hot biome, against a temperate one
	 * @param coldFactor      how long ice lasts in the cold
	 */
	public record Climate(float hotTemperature, float coldTemperature, double hotFactor, double coldFactor) {
		public static final Climate DEFAULT = new Climate(1.5F, 0.15F, 0.67, 1.33);

		public static final Codec<Climate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.FLOAT.fieldOf("hot_temperature").orElse(DEFAULT.hotTemperature).forGetter(Climate::hotTemperature),
				Codec.FLOAT.fieldOf("cold_temperature").orElse(DEFAULT.coldTemperature).forGetter(Climate::coldTemperature),
				Codec.doubleRange(0.01, 100.0).fieldOf("hot_factor").orElse(DEFAULT.hotFactor).forGetter(Climate::hotFactor),
				Codec.doubleRange(0.01, 100.0).fieldOf("cold_factor").orElse(DEFAULT.coldFactor).forGetter(Climate::coldFactor)
		).apply(instance, Climate::new));

		/** How long ice lasts here against a temperate place with nothing cold around. */
		public double factor(float temperature, boolean chilled) {
			double factor = temperature > hotTemperature ? hotFactor : 1.0;
			return temperature < coldTemperature || chilled ? factor * coldFactor : factor;
		}
	}

	/**
	 * @param items   foods this rule covers
	 * @param days    how long they keep out of a fridge
	 * @param becomes what each spoiled item turns into
	 */
	public record Rule(TagKey<Item> items, double days, Item becomes) {
		public static final Codec<Rule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				TagKey.codec(Registries.ITEM).fieldOf("items").forGetter(Rule::items),
				Codec.doubleRange(0.01, 10000.0).fieldOf("days").forGetter(Rule::days),
				BuiltInRegistries.ITEM.byNameCodec().fieldOf("becomes").forGetter(Rule::becomes)
		).apply(instance, Rule::new));

		public long lifetime() {
			return SpoilageLogic.lifetime(days);
		}
	}
}
