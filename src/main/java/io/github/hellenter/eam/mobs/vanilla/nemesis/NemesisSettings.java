package io.github.hellenter.eam.mobs.vanilla.nemesis;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Nemesis numbers, read from {@code data/eam/mobs/nemesis.json} so datapacks can override them.
 *
 * @param healthPerKill share of the mob's base health it gains for each player it kills
 * @param maxKills      kills that still add health; later kills only renew the name
 * @param takeItem      whether the killer takes the victim's best piece of gear
 */
public record NemesisSettings(float healthPerKill, int maxKills, boolean takeItem) {
	public static final NemesisSettings DEFAULT = new NemesisSettings(0.5F, 4, true);

	public static final Codec<NemesisSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.floatRange(0.0F, 100.0F).optionalFieldOf("health_per_kill", DEFAULT.healthPerKill).forGetter(NemesisSettings::healthPerKill),
			Codec.intRange(0, 1000).optionalFieldOf("max_kills", DEFAULT.maxKills).forGetter(NemesisSettings::maxKills),
			Codec.BOOL.optionalFieldOf("take_item", DEFAULT.takeItem).forGetter(NemesisSettings::takeItem)
	).apply(instance, NemesisSettings::new));
}
