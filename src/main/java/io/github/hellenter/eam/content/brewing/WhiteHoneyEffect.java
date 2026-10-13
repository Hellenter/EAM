package io.github.hellenter.eam.content.brewing;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * White honey: while it lasts the drinker cannot get the effects in {@link WhiteHoneyLogic#WARDED} and takes less wither
 * damage ({@link WhiteHoneyLogic#witherDamage}). Drinking it washes those effects away.
 */
class WhiteHoneyEffect extends MobEffect {
	WhiteHoneyEffect(int color) {
		super(MobEffectCategory.BENEFICIAL, color);
	}

	@Override
	public void onEffectStarted(LivingEntity mob, int amplifier) {
		List<Holder<MobEffect>> warded = mob.getActiveEffectsMap().keySet().stream().filter(WhiteHoneyLogic::isWarded).toList();
		for (Holder<MobEffect> effect : warded) {
			mob.removeEffect(effect);
		}
	}
}
