package io.github.hellenter.eam.world.structures.outpost;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OutpostLogicTest {
	@Test
	void clearingReachesRadiusFromEachSide() {
		assertTrue(OutpostLogic.inClearing(0, 0, 32, 32, 15, 16, 16));
		assertTrue(OutpostLogic.inClearing(0, 0, 32, 32, 15, -15, 10));
		assertTrue(OutpostLogic.inClearing(0, 0, 32, 32, 15, 47, 32));
		assertFalse(OutpostLogic.inClearing(0, 0, 32, 32, 15, -16, 10));
		assertFalse(OutpostLogic.inClearing(0, 0, 32, 32, 15, 10, 48));
	}

	@Test
	void clearingHasRoundedCorners() {
		assertTrue(OutpostLogic.inClearing(0, 0, 32, 32, 15, -10, -10));
		assertFalse(OutpostLogic.inClearing(0, 0, 32, 32, 15, -12, -12));
	}

	@Test
	void templateIsOnlyClearedAboveItsTop() {
		assertTrue(OutpostLogic.mayClear(false, 70, 90));
		assertFalse(OutpostLogic.mayClear(true, 90, 90));
		assertTrue(OutpostLogic.mayClear(true, 91, 90));
	}
}
