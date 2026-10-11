package io.github.hellenter.eam.survival.bleeding;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Balance numbers for bleeding, read from {@code data/eam/survival/bleeding.json} so datapacks can override them.
 * A missing or invalid field falls back to its default.
 *
 * @param damageInterval  ticks between two hits of bleeding damage
 * @param damage          damage of one hit; the default 0.5 every 15 ticks takes one heart over three seconds
 * @param drainMultiplier how much faster water and food drain while bleeding
 */
public record BleedingSettings(int damageInterval, float damage, float drainMultiplier) {
	public static final BleedingSettings DEFAULT = new BleedingSettings(15, 0.5F, 3.0F);

	public static final Codec<BleedingSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.intRange(1, 1200).fieldOf("damage_interval").orElse(DEFAULT.damageInterval).forGetter(BleedingSettings::damageInterval),
			Codec.floatRange(0.0F, 100.0F).fieldOf("damage").orElse(DEFAULT.damage).forGetter(BleedingSettings::damage),
			Codec.floatRange(1.0F, 100.0F).fieldOf("drain_multiplier").orElse(DEFAULT.drainMultiplier).forGetter(BleedingSettings::drainMultiplier)
	).apply(instance, BleedingSettings::new));
}
