package io.github.hellenter.eam.survival.thirst;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Balance numbers for thirst, read from {@code data/eam/thirst/settings.json} so datapacks can override them.
 * A missing or invalid field falls back to its default; encoding always writes every field.
 *
 * @param exhaustionPerPoint        exhaustion that costs one point of water (vanilla food uses 4.0)
 * @param activityMultiplier        share of vanilla food exhaustion (sprinting, jumping, mining, fighting)
 *                                  that also goes to thirst
 * @param passiveExhaustion         exhaustion added every tick just for being alive
 * @param hotMultiplier             multiplier for passive exhaustion in hot biomes and the Nether
 * @param hotTemperature            biome base temperature above which a biome counts as hot
 * @param dehydrationInterval       ticks between dehydration damage at zero water
 * @param dehydrationDamage         damage dealt each interval at zero water
 * @param peacefulRefillInterval    ticks between +1 water in peaceful
 * @param sourceSip                 water restored by one sip from a water source
 * @param sipCooldown               ticks between sips from a water source
 * @param poisoning                 food poisoning from sips at a water source
 */
public record ThirstSettings(
		float exhaustionPerPoint,
		float activityMultiplier,
		float passiveExhaustion,
		float hotMultiplier,
		float hotTemperature,
		int dehydrationInterval,
		float dehydrationDamage,
		int peacefulRefillInterval,
		int sourceSip,
		int sipCooldown,
		PoisoningSettings poisoning
) {
	public static final ThirstSettings DEFAULT = new ThirstSettings(
			4.0F, 1.0F, 0.003F, 2.0F, 1.5F, 80, 1.0F, 20, 2, 16, PoisoningSettings.DEFAULT
	);

	public static final Codec<ThirstSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.floatRange(0.1F, Float.MAX_VALUE).fieldOf("exhaustion_per_point").orElse(DEFAULT.exhaustionPerPoint).forGetter(ThirstSettings::exhaustionPerPoint),
			Codec.floatRange(0.0F, Float.MAX_VALUE).fieldOf("activity_multiplier").orElse(DEFAULT.activityMultiplier).forGetter(ThirstSettings::activityMultiplier),
			Codec.floatRange(0.0F, Float.MAX_VALUE).fieldOf("passive_exhaustion").orElse(DEFAULT.passiveExhaustion).forGetter(ThirstSettings::passiveExhaustion),
			Codec.floatRange(0.0F, Float.MAX_VALUE).fieldOf("hot_multiplier").orElse(DEFAULT.hotMultiplier).forGetter(ThirstSettings::hotMultiplier),
			Codec.FLOAT.fieldOf("hot_temperature").orElse(DEFAULT.hotTemperature).forGetter(ThirstSettings::hotTemperature),
			Codec.intRange(1, Integer.MAX_VALUE).fieldOf("dehydration_interval").orElse(DEFAULT.dehydrationInterval).forGetter(ThirstSettings::dehydrationInterval),
			Codec.floatRange(0.0F, Float.MAX_VALUE).fieldOf("dehydration_damage").orElse(DEFAULT.dehydrationDamage).forGetter(ThirstSettings::dehydrationDamage),
			Codec.intRange(1, Integer.MAX_VALUE).fieldOf("peaceful_refill_interval").orElse(DEFAULT.peacefulRefillInterval).forGetter(ThirstSettings::peacefulRefillInterval),
			Codec.intRange(0, Thirst.MAX).fieldOf("source_sip").orElse(DEFAULT.sourceSip).forGetter(ThirstSettings::sourceSip),
			Codec.intRange(1, Integer.MAX_VALUE).fieldOf("sip_cooldown").orElse(DEFAULT.sipCooldown).forGetter(ThirstSettings::sipCooldown),
			PoisoningSettings.CODEC.fieldOf("poisoning").orElse(DEFAULT.poisoning).forGetter(ThirstSettings::poisoning)
	).apply(instance, ThirstSettings::new));
}
