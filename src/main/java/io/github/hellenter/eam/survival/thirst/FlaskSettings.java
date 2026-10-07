package io.github.hellenter.eam.survival.thirst;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Flask balance, the {@code flask} block of the thirst settings.
 *
 * @param contaminationChance chance that filling a flask gives dirty water
 * @param poisoningDuration   ticks of food poisoning from drinking dirty flask water
 */
public record FlaskSettings(float contaminationChance, int poisoningDuration) {
	public static final FlaskSettings DEFAULT = new FlaskSettings(0.2F, 300);

	public static final Codec<FlaskSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.floatRange(0.0F, 1.0F).fieldOf("contamination_chance").orElse(DEFAULT.contaminationChance).forGetter(FlaskSettings::contaminationChance),
			Codec.intRange(1, Integer.MAX_VALUE).fieldOf("poisoning_duration").orElse(DEFAULT.poisoningDuration).forGetter(FlaskSettings::poisoningDuration)
	).apply(instance, FlaskSettings::new));
}
