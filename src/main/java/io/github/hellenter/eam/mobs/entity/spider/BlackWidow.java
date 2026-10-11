package io.github.hellenter.eam.mobs.entity.spider;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * MITE's black widow: a wood spider in black with a red hourglass on its back. Just as small and frail, but its bite
 * poisons for several seconds. It lives in jungles and dark forests and does not need a tree to spawn by.
 */
public class BlackWidow extends WoodSpider {
	public BlackWidow(EntityType<? extends BlackWidow> type, Level level) {
		super(type, level);
		xpReward = 5;
	}

	@Override
	protected SpiderSettings settings() {
		return SpiderModule.blackWidow();
	}
}
