package io.github.hellenter.eam.survival.death;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Death numbers, read from {@code data/eam/survival/death.json} so datapacks can override them.
 *
 * @param levelsLost experience levels a player loses on death; the rest stay with them
 */
public record DeathSettings(int levelsLost) {
	public static final DeathSettings DEFAULT = new DeathSettings(5);

	public static final Codec<DeathSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.intRange(0, 100000).optionalFieldOf("levels_lost", DEFAULT.levelsLost).forGetter(DeathSettings::levelsLost)
	).apply(instance, DeathSettings::new));
}
