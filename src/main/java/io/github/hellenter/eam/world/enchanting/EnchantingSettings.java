package io.github.hellenter.eam.world.enchanting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Prices of enchantments chosen at the enchanting table, read from {@code data/eam/enchanting/costs.json} so datapacks
 * can override them. Which enchantments are offered and what they cost is set by the enchantment tags
 * {@code #eam:offensive} and {@code #eam:supportive}.
 *
 * @param offensiveResource item paid for an offensive enchantment
 * @param supportiveResource item paid for a supportive enchantment
 * @param costs              resources per level, one row per max level: row 1 is for enchantments with one level
 * @param levelsPerResource  experience levels taken per resource paid
 */
public record EnchantingSettings(Item offensiveResource, Item supportiveResource, List<List<Integer>> costs, int levelsPerResource) {
	public static final EnchantingSettings DEFAULT = new EnchantingSettings(Items.DIAMOND, Items.EMERALD, List.of(
			List.of(4),
			List.of(3, 4),
			List.of(2, 3, 4),
			List.of(1, 2, 3, 4),
			List.of(1, 2, 3, 4, 5)
	), 1);

	public static final Codec<EnchantingSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			BuiltInRegistries.ITEM.byNameCodec().fieldOf("offensive_resource").orElse(DEFAULT.offensiveResource)
					.forGetter(EnchantingSettings::offensiveResource),
			BuiltInRegistries.ITEM.byNameCodec().fieldOf("supportive_resource").orElse(DEFAULT.supportiveResource)
					.forGetter(EnchantingSettings::supportiveResource),
			Codec.intRange(0, 64).listOf().listOf().fieldOf("costs").orElse(DEFAULT.costs).forGetter(EnchantingSettings::costs),
			Codec.intRange(0, 100).fieldOf("levels_per_resource").orElse(DEFAULT.levelsPerResource)
					.forGetter(EnchantingSettings::levelsPerResource)
	).apply(instance, EnchantingSettings::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, EnchantingSettings> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC);

	public Item resource(boolean offensive) {
		return offensive ? offensiveResource : supportiveResource;
	}
}
