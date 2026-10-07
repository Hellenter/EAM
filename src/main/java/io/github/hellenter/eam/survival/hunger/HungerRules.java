package io.github.hellenter.eam.survival.hunger;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Hunger balance numbers. Loaded from {@code data/<namespace>/survival/hunger.json}, so a datapack can
 * override them; missing keys fall back to {@link #DEFAULT}.
 *
 * @param baseMaxFood            food bar size at XP level 0
 * @param maxFoodStep            how much the food bar grows per step
 * @param levelsPerStep          XP levels per growth step
 * @param maxFoodCap             largest food bar size
 * @param exhaustionPerPoint     exhaustion that costs one point of saturation or food
 * @param passiveExhaustion      exhaustion added every tick, even when standing still
 * @param regenIntervalTicks     ticks between natural regeneration of one health point
 * @param regenMinFood           food needed for natural regeneration
 * @param regenExhaustion        exhaustion cost of one regenerated health point
 * @param starvationIntervalTicks ticks between starvation damage at zero food
 * @param starvationDamage       starvation damage per hit, on every difficulty
 */
public record HungerRules(
		int baseMaxFood,
		int maxFoodStep,
		int levelsPerStep,
		int maxFoodCap,
		float exhaustionPerPoint,
		float passiveExhaustion,
		int regenIntervalTicks,
		int regenMinFood,
		float regenExhaustion,
		int starvationIntervalTicks,
		float starvationDamage
) {
	public static final HungerRules DEFAULT = new HungerRules(6, 2, 5, 20, 4.0F, 0.0025F, 600, 1, 4.0F, 80, 1.0F);

	private static final Codec<Integer> POSITIVE_INT = Codec.intRange(1, Integer.MAX_VALUE);
	private static final Codec<Integer> NON_NEGATIVE_INT = Codec.intRange(0, Integer.MAX_VALUE);
	private static final Codec<Float> POSITIVE_FLOAT = Codec.floatRange(0.001F, Float.MAX_VALUE);
	private static final Codec<Float> NON_NEGATIVE_FLOAT = Codec.floatRange(0.0F, Float.MAX_VALUE);

	public static final Codec<HungerRules> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.intRange(1, 20).optionalFieldOf("base_max_food", DEFAULT.baseMaxFood).forGetter(HungerRules::baseMaxFood),
			NON_NEGATIVE_INT.optionalFieldOf("max_food_step", DEFAULT.maxFoodStep).forGetter(HungerRules::maxFoodStep),
			POSITIVE_INT.optionalFieldOf("levels_per_step", DEFAULT.levelsPerStep).forGetter(HungerRules::levelsPerStep),
			Codec.intRange(1, 20).optionalFieldOf("max_food_cap", DEFAULT.maxFoodCap).forGetter(HungerRules::maxFoodCap),
			POSITIVE_FLOAT.optionalFieldOf("exhaustion_per_point", DEFAULT.exhaustionPerPoint).forGetter(HungerRules::exhaustionPerPoint),
			NON_NEGATIVE_FLOAT.optionalFieldOf("passive_exhaustion", DEFAULT.passiveExhaustion).forGetter(HungerRules::passiveExhaustion),
			POSITIVE_INT.optionalFieldOf("regen_interval_ticks", DEFAULT.regenIntervalTicks).forGetter(HungerRules::regenIntervalTicks),
			NON_NEGATIVE_INT.optionalFieldOf("regen_min_food", DEFAULT.regenMinFood).forGetter(HungerRules::regenMinFood),
			NON_NEGATIVE_FLOAT.optionalFieldOf("regen_exhaustion", DEFAULT.regenExhaustion).forGetter(HungerRules::regenExhaustion),
			POSITIVE_INT.optionalFieldOf("starvation_interval_ticks", DEFAULT.starvationIntervalTicks).forGetter(HungerRules::starvationIntervalTicks),
			NON_NEGATIVE_FLOAT.optionalFieldOf("starvation_damage", DEFAULT.starvationDamage).forGetter(HungerRules::starvationDamage)
	).apply(instance, HungerRules::new));
}
