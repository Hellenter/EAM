package io.github.hellenter.eam.world.farming;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Balance numbers for crop and animal growth, read from {@code data/eam/farming/growth.json} so datapacks can override
 * them. A missing or invalid field falls back to its default.
 *
 * @param cropSlowdown  how many times slower crops on farmland grow than in vanilla. At 5 a crop on hydrated farmland
 *                      takes about 2 hours instead of 24 minutes.
 * @param boneMealBoost growth speed multiplier a crop gets from bone meal until it is broken.
 * @param animalSlowdown how many times slower animals age: babies grow up and parents wait to breed again.
 */
public record FarmingSettings(double cropSlowdown, double boneMealBoost, int animalSlowdown) {
	public static final FarmingSettings DEFAULT = new FarmingSettings(5.0, 1.5, 4);

	public static final Codec<FarmingSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.doubleRange(0.01, 100.0).fieldOf("crop_slowdown").orElse(DEFAULT.cropSlowdown).forGetter(FarmingSettings::cropSlowdown),
			Codec.doubleRange(1.0, 100.0).fieldOf("bone_meal_boost").orElse(DEFAULT.boneMealBoost).forGetter(FarmingSettings::boneMealBoost),
			Codec.intRange(1, 100).fieldOf("animal_slowdown").orElse(DEFAULT.animalSlowdown).forGetter(FarmingSettings::animalSlowdown)
	).apply(instance, FarmingSettings::new));
}
