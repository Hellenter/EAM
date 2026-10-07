package io.github.hellenter.eam.survival.hunger;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Hunger balance numbers. Loaded from {@code data/<namespace>/survival/hunger.json}, so a datapack can
 * override them; missing keys fall back to {@link #DEFAULT}.
 *
 * @param exhaustionPerPoint     exhaustion that costs one point of saturation or food
 * @param passiveExhaustion      exhaustion added every tick, even when standing still
 * @param regenIntervalTicks     ticks between natural regeneration of one health point
 * @param regenMinFood           food needed for natural regeneration
 * @param regenExhaustion        exhaustion cost of one regenerated health point
 * @param starvationIntervalTicks ticks between starvation damage at zero food
 * @param starvationDamage       starvation damage per hit, on every difficulty
 * @param startingNutrients      protein, fat and carbs of a new or respawned player, out of {@link Nutrition#MAX}
 * @param nutrientDecay          how much each nutrient drops per spent point of saturation or food
 * @param balanceThreshold       all three nutrients at or above this give the balanced diet bonus
 * @param balancedHungerFactor   with the bonus, a point of saturation or food costs this many times more exhaustion
 * @param balancedRegenFactor    with the bonus, natural regeneration is this many times faster
 */
public record HungerRules(
		float exhaustionPerPoint,
		float passiveExhaustion,
		int regenIntervalTicks,
		int regenMinFood,
		float regenExhaustion,
		int starvationIntervalTicks,
		float starvationDamage,
		int startingNutrients,
		float nutrientDecay,
		int balanceThreshold,
		float balancedHungerFactor,
		float balancedRegenFactor
) {
	public static final HungerRules DEFAULT = new HungerRules(4.0F, 0.0025F, 600, 1, 4.0F, 80, 1.0F, 50, 0.5F, 70, 2.0F, 1.5F);

	private static final Codec<Integer> POSITIVE_INT = Codec.intRange(1, Integer.MAX_VALUE);
	private static final Codec<Integer> NON_NEGATIVE_INT = Codec.intRange(0, Integer.MAX_VALUE);
	private static final Codec<Float> POSITIVE_FLOAT = Codec.floatRange(0.001F, Float.MAX_VALUE);
	private static final Codec<Float> NON_NEGATIVE_FLOAT = Codec.floatRange(0.0F, Float.MAX_VALUE);

	public static final Codec<HungerRules> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			POSITIVE_FLOAT.optionalFieldOf("exhaustion_per_point", DEFAULT.exhaustionPerPoint).forGetter(HungerRules::exhaustionPerPoint),
			NON_NEGATIVE_FLOAT.optionalFieldOf("passive_exhaustion", DEFAULT.passiveExhaustion).forGetter(HungerRules::passiveExhaustion),
			POSITIVE_INT.optionalFieldOf("regen_interval_ticks", DEFAULT.regenIntervalTicks).forGetter(HungerRules::regenIntervalTicks),
			NON_NEGATIVE_INT.optionalFieldOf("regen_min_food", DEFAULT.regenMinFood).forGetter(HungerRules::regenMinFood),
			NON_NEGATIVE_FLOAT.optionalFieldOf("regen_exhaustion", DEFAULT.regenExhaustion).forGetter(HungerRules::regenExhaustion),
			POSITIVE_INT.optionalFieldOf("starvation_interval_ticks", DEFAULT.starvationIntervalTicks).forGetter(HungerRules::starvationIntervalTicks),
			NON_NEGATIVE_FLOAT.optionalFieldOf("starvation_damage", DEFAULT.starvationDamage).forGetter(HungerRules::starvationDamage),
			Codec.intRange(0, Nutrition.MAX).optionalFieldOf("starting_nutrients", DEFAULT.startingNutrients).forGetter(HungerRules::startingNutrients),
			Codec.floatRange(0.0F, Nutrition.MAX).optionalFieldOf("nutrient_decay", DEFAULT.nutrientDecay).forGetter(HungerRules::nutrientDecay),
			Codec.intRange(0, Nutrition.MAX).optionalFieldOf("balance_threshold", DEFAULT.balanceThreshold).forGetter(HungerRules::balanceThreshold),
			POSITIVE_FLOAT.optionalFieldOf("balanced_hunger_factor", DEFAULT.balancedHungerFactor).forGetter(HungerRules::balancedHungerFactor),
			POSITIVE_FLOAT.optionalFieldOf("balanced_regen_factor", DEFAULT.balancedRegenFactor).forGetter(HungerRules::balancedRegenFactor)
	).apply(instance, HungerRules::new));
}
