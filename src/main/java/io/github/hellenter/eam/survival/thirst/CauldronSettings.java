package io.github.hellenter.eam.survival.thirst;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Drinking and boiling water in a cauldron.
 *
 * @param drink    water restored by one gulp straight from a water cauldron; each gulp takes one level of water
 * @param boilTime ticks a water cauldron must sit over a heat source ({@code #eam:cauldron_heat_sources}) to boil
 */
public record CauldronSettings(int drink, int boilTime) {
	public static final CauldronSettings DEFAULT = new CauldronSettings(6, 600);

	public static final Codec<CauldronSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.intRange(1, Thirst.MAX).fieldOf("drink").orElse(DEFAULT.drink).forGetter(CauldronSettings::drink),
			Codec.intRange(1, Integer.MAX_VALUE).fieldOf("boil_time").orElse(DEFAULT.boilTime).forGetter(CauldronSettings::boilTime)
	).apply(instance, CauldronSettings::new));
}
