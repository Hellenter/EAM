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
 * @param waterRadius       how far, in blocks across, water (a water block or a water cauldron) can be from a feeder for
 *                          its animals to have water; 0 means they need none
 * @param crowdRadius       how far, in blocks across, other livestock counts as close around an animal
 * @param crowdLimit        most other heads close around an animal before it has too little space
 * @param sickAfterDays     days without water or space after which an animal at a feeder falls sick: it grows up at the
 *                          normal speed, is not paired and moves slowly until it has as many good days
 */
public record FeederSettings(int headsPerFeeder, int slotsPerFeeder, int thinAfterDays, int animalRadius, int farmerRadius,
		int farmerKeepsPoints, int farmerKeepsSeeds, double metalScale, boolean breeding,
		double fedGrowthBoost, int waterRadius, int crowdRadius, int crowdLimit, int sickAfterDays) {
	public static final FeederSettings DEFAULT = new FeederSettings(10, 8, 7, 24, 8, 12, 8, 1.5, true, 1.5, 8, 2, 6, 2);

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
			Codec.doubleRange(1.0, 10.0).fieldOf("fed_growth_boost").orElse(DEFAULT.fedGrowthBoost).forGetter(FeederSettings::fedGrowthBoost),
			Codec.intRange(0, 16).fieldOf("water_radius").orElse(DEFAULT.waterRadius).forGetter(FeederSettings::waterRadius),
			Codec.intRange(1, 8).fieldOf("crowd_radius").orElse(DEFAULT.crowdRadius).forGetter(FeederSettings::crowdRadius),
			Codec.intRange(0, 100).fieldOf("crowd_limit").orElse(DEFAULT.crowdLimit).forGetter(FeederSettings::crowdLimit),
			Codec.intRange(1, 100).fieldOf("sick_after_days").orElse(DEFAULT.sickAfterDays).forGetter(FeederSettings::sickAfterDays)
	).apply(instance, FeederSettings::new));
}
