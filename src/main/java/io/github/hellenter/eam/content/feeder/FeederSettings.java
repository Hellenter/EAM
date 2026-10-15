package io.github.hellenter.eam.content.feeder;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Balance numbers for feeders and livestock, read from {@code data/eam/feeder/settings.json} so datapacks can override
 * them. Which animals are livestock and what they eat are the tags {@code eam:livestock} and {@code eam:livestock_feed}.
 *
 * @param headsPerFeeder    meals one feeder block gives a day: the heads it keeps fed. A trough of three feeds three times as many.
 * @param slotsPerFeeder    inventory slots of one feeder block, up to 12 (the menu fits six chest rows); a trough of three has three times as many
 * @param thinAfterDays     days without a meal after which an animal gets thin and drops no meat. It takes as many days of
 *                          meals to fatten up again.
 * @param animalRadius      how far, in blocks, livestock comes to a feeder from
 * @param farmerRadius      how far, in blocks, a farmer villager can be and still put crops in a feeder
 * @param farmerKeepsPoints villager food points of crops a farmer keeps for himself and other villagers; he gives the rest.
 *                          Vanilla villagers need 12 points to breed.
 * @param farmerKeepsSeeds  potatoes and carrots of each kind a farmer always keeps to plant his fields, over all his stacks
 * @param metalScale        how many times more slots and heads a metal feeder block has than a wooden one
 * @param breeding          whether fed livestock at a feeder breeds by itself, one pair per feeder block a day, until the
 *                          animals around reach the trough's heads
 * @param fedGrowthBoost    how many times faster animals kept at a feeder (fed from one within the last day) grow up,
 *                          get ready to breed again, and how many times as often a feeder pairs them
 */
public record FeederSettings(int headsPerFeeder, int slotsPerFeeder, int thinAfterDays, int animalRadius, int farmerRadius,
		int farmerKeepsPoints, int farmerKeepsSeeds, double metalScale, boolean breeding,
		double fedGrowthBoost) {
	public static final FeederSettings DEFAULT = new FeederSettings(10, 8, 7, 16, 8, 12, 8, 1.5, true, 1.5);

	public static final Codec<FeederSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.intRange(1, 1000).fieldOf("heads_per_feeder").orElse(DEFAULT.headsPerFeeder).forGetter(FeederSettings::headsPerFeeder),
			Codec.intRange(1, 12).fieldOf("slots_per_feeder").orElse(DEFAULT.slotsPerFeeder).forGetter(FeederSettings::slotsPerFeeder),
			Codec.intRange(1, 1000).fieldOf("thin_after_days").orElse(DEFAULT.thinAfterDays).forGetter(FeederSettings::thinAfterDays),
			Codec.intRange(1, 64).fieldOf("animal_radius").orElse(DEFAULT.animalRadius).forGetter(FeederSettings::animalRadius),
			Codec.intRange(0, 64).fieldOf("farmer_radius").orElse(DEFAULT.farmerRadius).forGetter(FeederSettings::farmerRadius),
			Codec.intRange(0, 10000).fieldOf("farmer_keeps_points").orElse(DEFAULT.farmerKeepsPoints).forGetter(FeederSettings::farmerKeepsPoints),
			Codec.intRange(0, 10000).fieldOf("farmer_keeps_seeds").orElse(DEFAULT.farmerKeepsSeeds).forGetter(FeederSettings::farmerKeepsSeeds),
			Codec.doubleRange(0.1, 100.0).fieldOf("metal_scale").orElse(DEFAULT.metalScale).forGetter(FeederSettings::metalScale),
			Codec.BOOL.fieldOf("breeding").orElse(DEFAULT.breeding).forGetter(FeederSettings::breeding),
			Codec.doubleRange(1.0, 10.0).fieldOf("fed_growth_boost").orElse(DEFAULT.fedGrowthBoost).forGetter(FeederSettings::fedGrowthBoost)
	).apply(instance, FeederSettings::new));
}
