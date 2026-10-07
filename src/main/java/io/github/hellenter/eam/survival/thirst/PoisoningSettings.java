package io.github.hellenter.eam.survival.thirst;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Food poisoning from drinking straight out of a water source, the {@code poisoning} block of the thirst settings.
 *
 * @param chance            chance that one sip poisons the player
 * @param dirtyWaterChance  chance per sip in biomes tagged {@code eam:dirty_water} (jungles, swamps)
 * @param duration          ticks the effect lasts
 * @param thirstExhaustion  thirst exhaustion added every tick of the effect
 * @param foodExhaustion    food exhaustion added every tick of the effect
 * @param immunity          ticks after the effect ends during which sips cannot poison
 */
public record PoisoningSettings(
		float chance,
		float dirtyWaterChance,
		int duration,
		float thirstExhaustion,
		float foodExhaustion,
		int immunity
) {
	public static final PoisoningSettings DEFAULT = new PoisoningSettings(0.4F, 0.6F, 600, 0.03F, 0.02F, 6000);

	public static final Codec<PoisoningSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.floatRange(0.0F, 1.0F).fieldOf("chance").orElse(DEFAULT.chance).forGetter(PoisoningSettings::chance),
			Codec.floatRange(0.0F, 1.0F).fieldOf("dirty_water_chance").orElse(DEFAULT.dirtyWaterChance).forGetter(PoisoningSettings::dirtyWaterChance),
			Codec.intRange(1, Integer.MAX_VALUE).fieldOf("duration").orElse(DEFAULT.duration).forGetter(PoisoningSettings::duration),
			Codec.floatRange(0.0F, Float.MAX_VALUE).fieldOf("thirst_exhaustion").orElse(DEFAULT.thirstExhaustion).forGetter(PoisoningSettings::thirstExhaustion),
			Codec.floatRange(0.0F, Float.MAX_VALUE).fieldOf("food_exhaustion").orElse(DEFAULT.foodExhaustion).forGetter(PoisoningSettings::foodExhaustion),
			Codec.intRange(0, Integer.MAX_VALUE).fieldOf("immunity").orElse(DEFAULT.immunity).forGetter(PoisoningSettings::immunity)
	).apply(instance, PoisoningSettings::new));
}
