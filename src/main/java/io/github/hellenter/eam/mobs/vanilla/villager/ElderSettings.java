package io.github.hellenter.eam.mobs.vanilla.villager;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Balance numbers for the elder, read from {@code data/eam/villagers/elder.json} so datapacks can override them.
 *
 * @param intervalTicks how often, in ticks, the elder hands one thing out
 * @param radius        how far, in blocks, the villagers he hands things to can be
 * @param foodPerGift   food items he gives a hungry villager at a time
 */
public record ElderSettings(int intervalTicks, int radius, int foodPerGift) {
	public static final ElderSettings DEFAULT = new ElderSettings(100, 48, 4);

	public static final Codec<ElderSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.intRange(20, 1_000_000).fieldOf("interval_ticks").orElse(DEFAULT.intervalTicks).forGetter(ElderSettings::intervalTicks),
			Codec.intRange(1, 128).fieldOf("radius").orElse(DEFAULT.radius).forGetter(ElderSettings::radius),
			Codec.intRange(1, 64).fieldOf("food_per_gift").orElse(DEFAULT.foodPerGift).forGetter(ElderSettings::foodPerGift)
	).apply(instance, ElderSettings::new));
}
