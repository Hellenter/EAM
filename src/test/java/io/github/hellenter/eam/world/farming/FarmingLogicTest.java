package io.github.hellenter.eam.world.farming;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
		assertEquals(100, aged(4, 1.0));
	}

	@Test
	void animalsAtAFeederAgeHalfAsFastAgain() {
		assertEquals(150, aged(4, 1.5));
		assertEquals(600, aged(1, 1.5));
	}

	@Test
	void slowdownOfOneIsVanilla() {
		assertEquals(1, FarmingLogic.ageSteps(7, 1, 1.0));
		assertEquals(400, aged(1, 1.0));
	}

	@Test
	void ageStopsAtZero() {
		assertEquals(-1, FarmingLogic.stepAge(-3, 2));
		assertEquals(0, FarmingLogic.stepAge(-1, 2));
		assertEquals(0, FarmingLogic.stepAge(1, 2));
		assertEquals(4, FarmingLogic.stepAge(5, 1));
	}

	private static int aged(int slowdown, double speed) {
		int aged = 0;
		for (int tick = 0; tick < 400; tick++) aged += FarmingLogic.ageSteps(tick, slowdown, speed);
		return aged;
	}

	@Test
	void harvestAddsUpToItsBonus() {
		// The random returns the highest value it may: bound - 1.
		assertEquals(2, FarmingLogic.harvestExtra(2, bound -> bound - 1));
		assertEquals(0, FarmingLogic.harvestExtra(2, bound -> 0));
	}

	@Test
	void noHarvestBonusAddsNothing() {
		assertEquals(0, FarmingLogic.harvestExtra(0, bound -> { throw new AssertionError("not rolled"); }));
	}
}
