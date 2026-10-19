package io.github.hellenter.eam.mobs.vanilla.villager;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Balance numbers for how villagers feed the village, read from {@code data/eam/villagers/food.json} so datapacks can
 * override them.
 *
 * @param livestockShare    share of the livestock feed a player gives a farmer that goes straight to the feeders
 * @param feederRadius      how far, in blocks, a feeder can be from the farmer to get it
 * @param fishIntervalTicks how often, in ticks, a working fisherman catches a fish
 * @param fishWaterRadius   how far, in blocks, water must be for the fisherman to fish
 * @param fishWaterBlocks   how many water blocks he needs within that radius
 * @param fishMax           how many fish he keeps at most; he stops fishing until he has given some away
 */
public record FoodSettings(double livestockShare, int feederRadius, int fishIntervalTicks, int fishWaterRadius, int fishWaterBlocks,
		int fishMax) {
	public static final FoodSettings DEFAULT = new FoodSettings(0.5, 8, 1200, 8, 4, 16);

	public static final Codec<FoodSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.doubleRange(0.0, 1.0).fieldOf("livestock_share").orElse(DEFAULT.livestockShare).forGetter(FoodSettings::livestockShare),
			Codec.intRange(0, 32).fieldOf("feeder_radius").orElse(DEFAULT.feederRadius).forGetter(FoodSettings::feederRadius),
			Codec.intRange(20, 1_000_000).fieldOf("fish_interval_ticks").orElse(DEFAULT.fishIntervalTicks).forGetter(FoodSettings::fishIntervalTicks),
			Codec.intRange(1, 16).fieldOf("fish_water_radius").orElse(DEFAULT.fishWaterRadius).forGetter(FoodSettings::fishWaterRadius),
			Codec.intRange(1, 1000).fieldOf("fish_water_blocks").orElse(DEFAULT.fishWaterBlocks).forGetter(FoodSettings::fishWaterBlocks),
			Codec.intRange(0, 1000).fieldOf("fish_max").orElse(DEFAULT.fishMax).forGetter(FoodSettings::fishMax)
	).apply(instance, FoodSettings::new));
}
