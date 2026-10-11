package io.github.hellenter.eam.survival.bleeding;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/** {@code eam:bleeding}: a wound that hurts every few ticks and drains water and food faster. */
public class BleedingEffect extends MobEffect {
	private static final int COLOR = 0x9E1414;

	BleedingEffect() {
		super(MobEffectCategory.HARMFUL, COLOR);
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity mob, int amplification) {
		BleedingModule.tick(level, mob);
		return true;
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int tickCount, int amplification) {
		return true;
	}
}
