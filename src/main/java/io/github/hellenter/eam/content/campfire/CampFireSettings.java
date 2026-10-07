package io.github.hellenter.eam.content.campfire;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Balance numbers for the camp fire, read from {@code data/eam/camp_fire/settings.json} so datapacks can override
 * them. A missing or invalid field falls back to its default.
 *
 * @param sticksToLight     sticks the fire needs before it can be lit
 * @param ticksPerStick     ticks a lit fire takes to burn one stick (400 = 3 sticks a minute)
 * @param cookTicks         ticks food lies on a lit fire before it is cooked
 * @param burnTicks         ticks cooked food may stay on a lit fire before it burns away
 * @param sticksPerCharcoal burnt sticks that make one charcoal
 * @param maxCharcoal       most charcoal one fire drops
 * @param scorchTicks       ticks a fire has burnt before its sticks look scorched
 * @param charTicks         ticks a fire has burnt before its sticks look charred black
 */
public record CampFireSettings(
		int sticksToLight,
		int ticksPerStick,
		int cookTicks,
		int burnTicks,
		int sticksPerCharcoal,
		int maxCharcoal,
		int scorchTicks,
		int charTicks
) {
	public static final CampFireSettings DEFAULT = new CampFireSettings(6, 400, 900, 900, 3, 16, 600, 1800);

	public static final Codec<CampFireSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.intRange(1, CampFireBlock.MAX_STICKS).fieldOf("sticks_to_light").orElse(DEFAULT.sticksToLight).forGetter(CampFireSettings::sticksToLight),
			Codec.intRange(1, Integer.MAX_VALUE).fieldOf("ticks_per_stick").orElse(DEFAULT.ticksPerStick).forGetter(CampFireSettings::ticksPerStick),
			Codec.intRange(1, Integer.MAX_VALUE).fieldOf("cook_ticks").orElse(DEFAULT.cookTicks).forGetter(CampFireSettings::cookTicks),
			Codec.intRange(1, Integer.MAX_VALUE).fieldOf("burn_ticks").orElse(DEFAULT.burnTicks).forGetter(CampFireSettings::burnTicks),
			Codec.intRange(1, Integer.MAX_VALUE).fieldOf("sticks_per_charcoal").orElse(DEFAULT.sticksPerCharcoal).forGetter(CampFireSettings::sticksPerCharcoal),
			Codec.intRange(0, Integer.MAX_VALUE).fieldOf("max_charcoal").orElse(DEFAULT.maxCharcoal).forGetter(CampFireSettings::maxCharcoal),
			Codec.intRange(0, Integer.MAX_VALUE).fieldOf("scorch_ticks").orElse(DEFAULT.scorchTicks).forGetter(CampFireSettings::scorchTicks),
			Codec.intRange(0, Integer.MAX_VALUE).fieldOf("char_ticks").orElse(DEFAULT.charTicks).forGetter(CampFireSettings::charTicks)
	).apply(instance, CampFireSettings::new));
}
