package io.github.hellenter.eam.mobs.entity.lost;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * The longbowman's mark: it does nothing by itself; the marked one takes more damage ({@link LostMark#damage}) and the
 * Lost go for him first.
 */
final class MarkEffect extends MobEffect {
	MarkEffect(int color) {
		super(MobEffectCategory.HARMFUL, color);
	}
}
