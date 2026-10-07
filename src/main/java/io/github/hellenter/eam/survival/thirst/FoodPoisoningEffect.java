package io.github.hellenter.eam.survival.thirst;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/** {@code eam:food_poisoning}: drains water and food every tick. Caught from sips at a water source. */
public class FoodPoisoningEffect extends MobEffect {
	private static final int COLOR = 0x7A5C1E;

	FoodPoisoningEffect() {
		super(MobEffectCategory.HARMFUL, COLOR);
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity mob, int amplification) {
		if (mob instanceof ServerPlayer player) {
			ThirstModule.poisoningTick(player);
		}
		return true;
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int tickCount, int amplification) {
		return true;
	}
}
