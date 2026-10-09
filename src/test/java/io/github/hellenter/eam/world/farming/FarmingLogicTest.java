package io.github.hellenter.eam.world.farming;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FarmingLogicTest {
	@Test
	void hydratedCropRollsFiveTimesRarer() {
		// Vanilla bound 3 on hydrated farmland: 1 in 3 becomes 1 in 15.
		assertEquals(15, FarmingLogic.growthBound(3, 5.0, 1.0));
	}

	@Test
	void boneMealMakesGrowthOneAndAHalfTimesFaster() {
		assertEquals(10, FarmingLogic.growthBound(3, 5.0, 1.5));
	}

	@Test
	void boundNeverDropsBelowOne() {
		assertEquals(1, FarmingLogic.growthBound(1, 0.01, 100.0));
	}

	@Test
	void animalsAgeOneTickInFour() {
		int aged = 0;
		for (int tick = 0; tick < 400; tick++) {
			if (FarmingLogic.agesThisTick(tick, 4)) aged++;
		}
		assertEquals(100, aged);
	}

	@Test
	void slowdownOfOneIsVanilla() {
		assertTrue(FarmingLogic.agesThisTick(7, 1));
		assertFalse(FarmingLogic.agesThisTick(7, 4));
	}
}
