package io.github.hellenter.eam.survival.sleep;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Marker for {@code eam:rested}. Its bonuses come from {@link RestedSettings} and are applied by {@link RestedModule},
 * so a datapack can change them.
 */
public class RestedEffect extends MobEffect {
	RestedEffect() {
		super(MobEffectCategory.BENEFICIAL, 0xE8D27A);
	}
}
