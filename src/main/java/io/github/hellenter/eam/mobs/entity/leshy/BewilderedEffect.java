package io.github.hellenter.eam.mobs.entity.leshy;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Led astray by a leshy. It does nothing by itself: the compass needle and the player's marker on maps check for it
 * ({@link LeshyModule#bewildered}).
 */
class BewilderedEffect extends MobEffect {
	BewilderedEffect(int color) {
		super(MobEffectCategory.HARMFUL, color);
	}
}
