package io.github.hellenter.eam.survival.hunger;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Marker for a missing nutrient: {@code eam:protein_deficiency}, {@code eam:fat_deficiency} or
 * {@code eam:carbs_deficiency}. The penalties themselves come from {@link DeficiencyRules} and are applied by
 * {@link HungerModule}, so a datapack can change them.
 */
public class DeficiencyEffect extends MobEffect {
	DeficiencyEffect(int color) {
		super(MobEffectCategory.HARMFUL, color);
	}
}
