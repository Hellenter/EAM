package io.github.hellenter.eam.world.forging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ForgingLogicTest {
	@Test
	void coolsOneMinuteAfterLeavingTheFurnace() {
		long coolsAt = ForgingLogic.coolsAt(1000);
		assertEquals(1000 + 1200, coolsAt);
		assertFalse(ForgingLogic.isCooled(coolsAt, 1000 + 1199));
		assertTrue(ForgingLogic.isCooled(coolsAt, 1000 + 1200));
	}

	@Test
	void ironInTheFurnaceOutputStaysHot() {
		assertFalse(ForgingLogic.isCooled(ForgingLogic.NOT_STARTED, Long.MAX_VALUE - 1));
		assertEquals(60, ForgingLogic.secondsLeft(ForgingLogic.NOT_STARTED, 0));
	}

	@Test
	void secondsLeftRoundUp() {
		assertEquals(60, ForgingLogic.secondsLeft(1200, 0));
		assertEquals(1, ForgingLogic.secondsLeft(1200, 1199));
		assertEquals(0, ForgingLogic.secondsLeft(1200, 1200));
		assertEquals(0, ForgingLogic.secondsLeft(1200, 5000));
	}

	@Test
	void anvilForgesOnePlatePerHotIngot() {
		assertEquals(5, ForgingLogic.plates(5, true, false));
		assertEquals(0, ForgingLogic.plates(5, false, false));
		assertEquals(0, ForgingLogic.plates(5, true, true));
	}
}
