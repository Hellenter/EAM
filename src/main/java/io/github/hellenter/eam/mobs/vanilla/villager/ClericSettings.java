package io.github.hellenter.eam.mobs.vanilla.villager;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;

import java.util.Map;

/**
 * Balance numbers for the cleric, read from {@code data/eam/villagers/cleric.json} so datapacks can override them.
 *
 * @param healIntervalTicks how often, in ticks, he heals hurt villagers and hands potions to guards
 * @param radius            how far, in blocks, he reaches
 * @param healAmount        health he gives each hurt villager every time
 * @param brewIntervalTicks how often, in ticks, he brews a potion at work from an ingredient a player gave him
 * @param potionMax         potions he keeps at most, his own and the guards' each
 * @param potions           the potion each ingredient makes; the cleric's inventory takes these ingredients
 */
public record ClericSettings(int healIntervalTicks, int radius, float healAmount, int brewIntervalTicks, int potionMax,
		Map<Item, Holder<Potion>> potions) {
	public static final ClericSettings DEFAULT = new ClericSettings(200, 16, 2.0F, 1200, 4, Map.of(
			Items.GLISTERING_MELON_SLICE, Potions.HEALING,
			Items.GHAST_TEAR, Potions.REGENERATION,
			Items.BLAZE_POWDER, Potions.STRENGTH,
			Items.SUGAR, Potions.SWIFTNESS,
			Items.MAGMA_CREAM, Potions.FIRE_RESISTANCE,
			Items.GOLDEN_CARROT, Potions.NIGHT_VISION));

	public static final Codec<ClericSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.intRange(20, 1_000_000).fieldOf("heal_interval_ticks").orElse(DEFAULT.healIntervalTicks).forGetter(ClericSettings::healIntervalTicks),
			Codec.intRange(1, 64).fieldOf("radius").orElse(DEFAULT.radius).forGetter(ClericSettings::radius),
			Codec.floatRange(0.0F, 1000.0F).fieldOf("heal_amount").orElse(DEFAULT.healAmount).forGetter(ClericSettings::healAmount),
			Codec.intRange(20, 1_000_000).fieldOf("brew_interval_ticks").orElse(DEFAULT.brewIntervalTicks).forGetter(ClericSettings::brewIntervalTicks),
			Codec.intRange(0, 64).fieldOf("potion_max").orElse(DEFAULT.potionMax).forGetter(ClericSettings::potionMax),
			Codec.unboundedMap(BuiltInRegistries.ITEM.byNameCodec(), Potion.CODEC).fieldOf("potions").orElse(DEFAULT.potions)
					.forGetter(ClericSettings::potions)
	).apply(instance, ClericSettings::new));
}
