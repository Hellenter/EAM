package io.github.hellenter.eam.survival.hunger;

import io.github.hellenter.eam.Eam;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * {@code eam:malnourished}: given while protein, fat or carbs is at zero. Mines 20% slower and moves 10% slower;
 * {@link HungerModule} also slows natural regeneration by a third.
 */
public class MalnourishedEffect extends MobEffect {
	private static final int COLOR = 0x8B6F47;

	MalnourishedEffect() {
		super(MobEffectCategory.HARMFUL, COLOR);
		addAttributeModifier(Attributes.BLOCK_BREAK_SPEED, Eam.id("effect.malnourished"), -0.2,
				AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		addAttributeModifier(Attributes.MOVEMENT_SPEED, Eam.id("effect.malnourished"), -0.1,
				AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
	}
}
