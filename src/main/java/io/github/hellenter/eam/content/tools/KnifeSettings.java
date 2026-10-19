package io.github.hellenter.eam.content.tools;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Balance numbers for knives, read from {@code data/eam/tools/knife.json} so datapacks can override them.
 *
 * @param animalLootMultiplier drops of an animal ({@link KnifeModule#BUTCHERED}) a player kills with a knife are
 *                             multiplied by this; a fractional item drops with that chance
 */
public record KnifeSettings(double animalLootMultiplier) {
	public static final KnifeSettings DEFAULT = new KnifeSettings(1.2);

	public static final Codec<KnifeSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.doubleRange(1.0, 10.0).fieldOf("animal_loot_multiplier").orElse(DEFAULT.animalLootMultiplier)
					.forGetter(KnifeSettings::animalLootMultiplier)
	).apply(instance, KnifeSettings::new));
}
