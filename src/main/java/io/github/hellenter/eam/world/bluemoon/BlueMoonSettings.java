package io.github.hellenter.eam.world.bluemoon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.hellenter.eam.world.bloodmoon.BloodMoonLogic;

/**
 * Blue moon numbers, read from {@code data/eam/events/blue_moon.json} so datapacks can override them. A missing or
 * invalid field falls back to its default.
 *
 * @param firstMinDay      the first blue moon of a world comes on this day or later (day 1 is the first day)
 * @param firstMaxDay      ... and on this day at the latest
 * @param intervalMinDays  later blue moons come at least this many days after the previous one
 * @param intervalMaxDays  ... and at most this many
 * @param startTime        ticks into the day when the event begins (12000: sunset); it ends when the next day begins
 * @param growthMultiplier crops on farmland grow this many times faster during the event
 * @param harvestMultiplier crops harvested during the event drop this many times more produce
 */
public record BlueMoonSettings(int firstMinDay, int firstMaxDay, int intervalMinDays, int intervalMaxDays, int startTime,
		double growthMultiplier, double harvestMultiplier) {
	/** About 30% more often than the blood moon: every 8-15 days (11.5 on average) against its 10-20 (15). */
	public static final BlueMoonSettings DEFAULT = new BlueMoonSettings(8, 14, 8, 15, 12_000, 2.0, 1.5);

	public static final Codec<BlueMoonSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.intRange(1, 100_000).fieldOf("first_min_day").orElse(DEFAULT.firstMinDay).forGetter(BlueMoonSettings::firstMinDay),
			Codec.intRange(1, 100_000).fieldOf("first_max_day").orElse(DEFAULT.firstMaxDay).forGetter(BlueMoonSettings::firstMaxDay),
			Codec.intRange(1, 100_000).fieldOf("interval_min_days").orElse(DEFAULT.intervalMinDays).forGetter(BlueMoonSettings::intervalMinDays),
			Codec.intRange(1, 100_000).fieldOf("interval_max_days").orElse(DEFAULT.intervalMaxDays).forGetter(BlueMoonSettings::intervalMaxDays),
			Codec.intRange(0, BloodMoonLogic.DAY_LENGTH - 1).fieldOf("start_time").orElse(DEFAULT.startTime).forGetter(BlueMoonSettings::startTime),
			Codec.doubleRange(0.01, 100.0).fieldOf("growth_multiplier").orElse(DEFAULT.growthMultiplier).forGetter(BlueMoonSettings::growthMultiplier),
			Codec.doubleRange(0.0, 16.0).fieldOf("harvest_multiplier").orElse(DEFAULT.harvestMultiplier).forGetter(BlueMoonSettings::harvestMultiplier)
	).apply(instance, BlueMoonSettings::new));
}
