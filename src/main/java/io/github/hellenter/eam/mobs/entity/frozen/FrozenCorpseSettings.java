package io.github.hellenter.eam.mobs.entity.frozen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Balance numbers for the frozen corpse, read from {@code data/eam/mobs/frozen_corpse.json} so datapacks can override
 * them. A missing or invalid field falls back to its default. Times are in ticks.
 *
 * @param slownessTicks    how long each hit leaves its victim slowed
 * @param maxSlownessLevel how deep the slowness goes as hits land one after another, 0 being Slowness I
 * @param spawnChance      chance that a natural spawn goes ahead
 */
public record FrozenCorpseSettings(int slownessTicks, int maxSlownessLevel, float spawnChance) {
	public static final FrozenCorpseSettings DEFAULT = new FrozenCorpseSettings(80, 2, 1.0F);

	public static final Codec<FrozenCorpseSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.intRange(0, 72000).fieldOf("slowness_ticks").orElse(DEFAULT.slownessTicks).forGetter(FrozenCorpseSettings::slownessTicks),
			Codec.intRange(0, 4).fieldOf("max_slowness_level").orElse(DEFAULT.maxSlownessLevel)
					.forGetter(FrozenCorpseSettings::maxSlownessLevel),
			Codec.floatRange(0.0F, 1.0F).fieldOf("spawn_chance").orElse(DEFAULT.spawnChance).forGetter(FrozenCorpseSettings::spawnChance)
	).apply(instance, FrozenCorpseSettings::new));
}
