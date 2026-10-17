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
 * @param driedMultiplier how many times longer dried food keeps than fresh food of its rule
 */
public record SpoilageSettings(List<Rule> rules, double daysPerIce, double driedMultiplier) {
	/** Raw meat and fish: five days, then rotten flesh. */
	public static final TagKey<Item> SPOILS_FAST = TagKey.create(Registries.ITEM, Eam.id("spoils_fast"));
	/** Fruit, vegetables and cooked food: ten days, then rotten food. */
	public static final TagKey<Item> SPOILS = TagKey.create(Registries.ITEM, Eam.id("spoils"));

	public static final SpoilageSettings DEFAULT = new SpoilageSettings(List.of(
			new Rule(SPOILS_FAST, 5.0, Items.ROTTEN_FLESH),
			new Rule(SPOILS, 10.0, EamItems.ROTTEN_FOOD)), 1.0, 5.0);

	public static final Codec<SpoilageSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Rule.CODEC.listOf().fieldOf("rules").orElse(DEFAULT.rules).forGetter(SpoilageSettings::rules),
			Codec.doubleRange(0.01, 1000.0).fieldOf("days_per_ice").orElse(DEFAULT.daysPerIce).forGetter(SpoilageSettings::daysPerIce),
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
