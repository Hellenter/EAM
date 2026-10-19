package io.github.hellenter.eam.survival.hunger;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * What running low on one nutrient does. The {@code deficiency} object of {@code survival/hunger.json}.
 *
 * @param threshold            a nutrient at or below this is deficient
 * @param proteinAttackPenalty without protein, attack damage drops by this fraction
 * @param proteinMiningPenalty without protein, mining speed drops by this fraction
 * @param fatHungerFactor      without fat, hunger drains this many times faster
 * @param carbsSprintTicks     without carbs, sprinting gives out after this many ticks of running
 * @param carbsRestTicks       after giving out, the player cannot sprint for this many ticks
 */
public record DeficiencyRules(
		int threshold,
		float proteinAttackPenalty,
		float proteinMiningPenalty,
		float fatHungerFactor,
		int carbsSprintTicks,
		int carbsRestTicks
) {
	public static final DeficiencyRules DEFAULT = new DeficiencyRules(0, 0.15F, 0.2F, 1.2F, 500, 100);

	private static final Codec<Float> FRACTION = Codec.floatRange(0.0F, 1.0F);
	private static final Codec<Integer> POSITIVE_INT = Codec.intRange(1, Integer.MAX_VALUE);

	public static final Codec<DeficiencyRules> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.intRange(0, Nutrition.MAX - 1).optionalFieldOf("threshold", DEFAULT.threshold).forGetter(DeficiencyRules::threshold),
			FRACTION.optionalFieldOf("protein_attack_penalty", DEFAULT.proteinAttackPenalty).forGetter(DeficiencyRules::proteinAttackPenalty),
			FRACTION.optionalFieldOf("protein_mining_penalty", DEFAULT.proteinMiningPenalty).forGetter(DeficiencyRules::proteinMiningPenalty),
			Codec.floatRange(1.0F, Float.MAX_VALUE).optionalFieldOf("fat_hunger_factor", DEFAULT.fatHungerFactor).forGetter(DeficiencyRules::fatHungerFactor),
			POSITIVE_INT.optionalFieldOf("carbs_sprint_ticks", DEFAULT.carbsSprintTicks).forGetter(DeficiencyRules::carbsSprintTicks),
			POSITIVE_INT.optionalFieldOf("carbs_rest_ticks", DEFAULT.carbsRestTicks).forGetter(DeficiencyRules::carbsRestTicks)
	).apply(instance, DeficiencyRules::new));
}
