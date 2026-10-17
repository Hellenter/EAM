package io.github.hellenter.eam.content.underworld;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * How glowshroom tips regrow their caps, read from {@code data/eam/glowshroom/settings.json}.
 *
 * @param ripenChance chance a bare tip ripens on each of its random ticks (about every 68 seconds at the default random
 *                    tick speed): 0.05 ripens it in about 23 minutes
 */
public record GlowshroomSettings(float ripenChance) {
	public static final GlowshroomSettings DEFAULT = new GlowshroomSettings(0.05F);

	public static final Codec<GlowshroomSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.floatRange(0.0F, 1.0F).fieldOf("ripen_chance").orElse(DEFAULT.ripenChance).forGetter(GlowshroomSettings::ripenChance)
	).apply(instance, GlowshroomSettings::new));
}
