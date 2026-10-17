package io.github.hellenter.eam.content.campfire;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Balance numbers for the camp fire, read from {@code data/eam/camp_fire/settings.json} so datapacks can override
 * them. A missing or invalid field falls back to its default.
 *
 * @param sticksToLight     sticks the fire needs before it can be lit
 * @param ticksPerStick     ticks a lit fire takes to burn one stick (600 = one stick every 30 seconds)
 * @param cookTicks         ticks food lies on a lit fire before it is cooked
 * @param burnTicks         ticks cooked food may stay on a lit fire before it gets burnt
 * @param ashTicks          ticks burnt food may stay on a lit fire before it turns to ash
 * @param sticksPerCharcoal burnt sticks that make one charcoal
 * @param maxCharcoal       most charcoal one fire drops
 * @param scorchTicks       ticks a fire has burnt before its sticks look scorched
 * @param charTicks         ticks a fire has burnt before its sticks look charred black
 * @param maxCookBonus      how much faster a full fire cooks than one with a single stick (0.25 = 25% faster); the
 *                          bonus grows evenly with each stick
 */
public record CampFireSettings(
		int sticksToLight,
		int ticksPerStick,
		int cookTicks,
		int burnTicks,
		int ashTicks,
		int sticksPerCharcoal,
		int maxCharcoal,
		int scorchTicks,
		int charTicks,
		float maxCookBonus
) {
	public static final CampFireSettings DEFAULT = new CampFireSettings(6, 600, 900, 1200, 1200, 3, 16, 600, 1800, 0.25F);

	public static final Codec<CampFireSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.intRange(1, CampFireBlock.MAX_STICKS).fieldOf("sticks_to_light").orElse(DEFAULT.sticksToLight).forGetter(CampFireSettings::sticksToLight),
			Codec.intRange(1, Integer.MAX_VALUE).fieldOf("ticks_per_stick").orElse(DEFAULT.ticksPerStick).forGetter(CampFireSettings::ticksPerStick),
			Codec.intRange(1, Integer.MAX_VALUE).fieldOf("cook_ticks").orElse(DEFAULT.cookTicks).forGetter(CampFireSettings::cookTicks),
			Codec.intRange(1, Integer.MAX_VALUE).fieldOf("burn_ticks").orElse(DEFAULT.burnTicks).forGetter(CampFireSettings::burnTicks),
			Codec.intRange(1, Integer.MAX_VALUE).fieldOf("ash_ticks").orElse(DEFAULT.ashTicks).forGetter(CampFireSettings::ashTicks),
			Codec.intRange(1, Integer.MAX_VALUE).fieldOf("sticks_per_charcoal").orElse(DEFAULT.sticksPerCharcoal).forGetter(CampFireSettings::sticksPerCharcoal),
			Codec.intRange(0, Integer.MAX_VALUE).fieldOf("max_charcoal").orElse(DEFAULT.maxCharcoal).forGetter(CampFireSettings::maxCharcoal),
			Codec.intRange(0, Integer.MAX_VALUE).fieldOf("scorch_ticks").orElse(DEFAULT.scorchTicks).forGetter(CampFireSettings::scorchTicks),
			Codec.intRange(0, Integer.MAX_VALUE).fieldOf("char_ticks").orElse(DEFAULT.charTicks).forGetter(CampFireSettings::charTicks),
			Codec.floatRange(0.0F, 10.0F).fieldOf("max_cook_bonus").orElse(DEFAULT.maxCookBonus).forGetter(CampFireSettings::maxCookBonus)
	).apply(instance, CampFireSettings::new));
}
