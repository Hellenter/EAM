package io.github.hellenter.eam.content.oven;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Balance numbers for the clay oven, read from {@code data/eam/clay_oven/settings.json} so datapacks can override them.
 * The oven cooks smoking recipes, which take 200 ticks (10 s) in a vanilla smoker.
 *
 * @param cookTimeMultiplier how many times longer than in a smoker food cooks (2.0 = 20 s for most food)
 * @param fuelTimeMultiplier how many times longer than in a furnace a fuel item burns: clay holds the heat
 */
public record ClayOvenSettings(float cookTimeMultiplier, float fuelTimeMultiplier) {
	public static final ClayOvenSettings DEFAULT = new ClayOvenSettings(2.0F, 1.5F);

	public static final Codec<ClayOvenSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.floatRange(0.01F, 100.0F).fieldOf("cook_time_multiplier").orElse(DEFAULT.cookTimeMultiplier)
					.forGetter(ClayOvenSettings::cookTimeMultiplier),
			Codec.floatRange(0.01F, 100.0F).fieldOf("fuel_time_multiplier").orElse(DEFAULT.fuelTimeMultiplier)
					.forGetter(ClayOvenSettings::fuelTimeMultiplier)
	).apply(instance, ClayOvenSettings::new));
}
