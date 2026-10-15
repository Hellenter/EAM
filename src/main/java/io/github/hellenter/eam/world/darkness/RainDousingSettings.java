package io.github.hellenter.eam.world.darkness;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * How fast rain puts fires out, read from {@code data/eam/rain_dousing/settings.json}. Each chance applies once per
 * random tick of the block (about every 68 seconds at the default random tick speed) while rain falls on it.
 *
 * @param torchChance    chance a torch goes out
 * @param campfireChance chance a lit campfire or camp fire goes out
 */
public record RainDousingSettings(float torchChance, float campfireChance) {
	public static final RainDousingSettings DEFAULT = new RainDousingSettings(0.5F, 0.5F);

	public static final Codec<RainDousingSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.floatRange(0.0F, 1.0F).fieldOf("torch_chance").orElse(DEFAULT.torchChance).forGetter(RainDousingSettings::torchChance),
			Codec.floatRange(0.0F, 1.0F).fieldOf("campfire_chance").orElse(DEFAULT.campfireChance).forGetter(RainDousingSettings::campfireChance)
	).apply(instance, RainDousingSettings::new));
}
