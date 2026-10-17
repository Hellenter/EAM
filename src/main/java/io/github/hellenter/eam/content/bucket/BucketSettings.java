package io.github.hellenter.eam.content.bucket;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.hellenter.eam.content.EamItems;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Map;

/**
 * Chance for each empty bucket to melt when it scoops lava, and how often a cow or goat gives milk, read from
 * {@code data/eam/buckets/lava_melting.json} so datapacks can override them. A bucket missing from the file keeps its
 * default chance.
 *
 * @param milkIntervalTicks how long after milking a cow or goat gives milk again: one day
 */
public record BucketSettings(Map<Item, Float> lavaMeltChance, int milkIntervalTicks) {
	public static final int DEFAULT_MILK_INTERVAL = 24000;

	public static final Codec<BucketSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.unboundedMap(BuiltInRegistries.ITEM.byNameCodec(), Codec.floatRange(0.0F, 1.0F))
					.fieldOf("lava_melt_chance").forGetter(BucketSettings::lavaMeltChance),
			Codec.intRange(0, 10_000_000).optionalFieldOf("milk_interval_ticks", DEFAULT_MILK_INTERVAL)
					.forGetter(BucketSettings::milkIntervalTicks)
	).apply(instance, BucketSettings::new));

	public BucketSettings(Map<Item, Float> lavaMeltChance) {
		this(lavaMeltChance, DEFAULT_MILK_INTERVAL);
	}

	/** MITE's chances: copper and silver 16%, gold 20%, iron 8%, mithril 1%. */
	public static Map<Item, Float> defaultChances() {
		return Map.of(
				EamItems.COPPER_BUCKET, BucketLogic.miteMeltChance(4.0F),
				EamItems.SILVER_BUCKET, BucketLogic.miteMeltChance(4.0F),
				EamItems.GOLDEN_BUCKET, 0.2F,
				Items.BUCKET, BucketLogic.miteMeltChance(8.0F),
				EamItems.MITHRIL_BUCKET, BucketLogic.miteMeltChance(64.0F));
	}

	public float meltChance(Item bucket) {
		Float chance = lavaMeltChance.get(bucket);
		return chance != null ? chance : defaultChances().getOrDefault(bucket, 0.0F);
	}
}
