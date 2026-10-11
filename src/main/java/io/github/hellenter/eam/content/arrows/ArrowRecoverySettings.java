package io.github.hellenter.eam.content.arrows;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Read from {@code data/eam/arrows/recovery.json} so datapacks can override it: the chance for each arrow stuck in a
 * mob to drop when the mob dies, and how much each level of Arrow Recovery on the bow adds.
 */
public record ArrowRecoverySettings(float baseChance, float chancePerLevel) {
	public static final ArrowRecoverySettings DEFAULT = new ArrowRecoverySettings(0.5F, 0.12F);

	public static final Codec<ArrowRecoverySettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.floatRange(0.0F, 1.0F).optionalFieldOf("base_chance", DEFAULT.baseChance).forGetter(ArrowRecoverySettings::baseChance),
			Codec.floatRange(0.0F, 1.0F).optionalFieldOf("chance_per_level", DEFAULT.chancePerLevel).forGetter(ArrowRecoverySettings::chancePerLevel)
	).apply(instance, ArrowRecoverySettings::new));

	public float chance(int recoveryLevel) {
		return ArrowRecoveryLogic.chance(baseChance, chancePerLevel, recoveryLevel);
	}
}
