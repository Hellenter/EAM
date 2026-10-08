package io.github.hellenter.eam.mobs.vanilla.drops;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MobDropsLogicTest {
	@Test
	void burningInDaylightIsSunDeath() {
		assertTrue(MobDropsLogic.burnedBySun(true, 1000, 1000));
		assertTrue(MobDropsLogic.burnedBySun(true, 1000, 1000 + MobDropsLogic.SUN_FIRE_TICKS));
	}

	@Test
	void fireLongAfterTheSunIsNot() {
		assertFalse(MobDropsLogic.burnedBySun(true, 1000, 1001 + MobDropsLogic.SUN_FIRE_TICKS));
	}

	@Test
	void neverSunlitOrNotBurningIsNot() {
		assertFalse(MobDropsLogic.burnedBySun(true, -1, 1000));
		assertFalse(MobDropsLogic.burnedBySun(false, 1000, 1000));
	}
}
