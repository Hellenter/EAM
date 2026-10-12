package io.github.hellenter.eam.mobs.entity.upyr;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * A ward against bloodsuckers: it does nothing by itself, upyrs and vampire bats check for it when they attack
 * ({@link UpyrModule#wardedAttack}).
 */
class WardEffect extends MobEffect {
	WardEffect(int color) {
		super(MobEffectCategory.BENEFICIAL, color);
	}
}
