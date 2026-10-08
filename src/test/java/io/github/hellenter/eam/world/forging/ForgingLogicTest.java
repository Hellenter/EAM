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
	void anvilForgesOnePlateFromTwoHotIngots() {
		assertTrue(ForgingLogic.canForge(2, true, false));
		assertTrue(ForgingLogic.canForge(5, true, false));
		assertFalse(ForgingLogic.canForge(1, true, false));
		assertFalse(ForgingLogic.canForge(5, false, false));
		assertFalse(ForgingLogic.canForge(5, true, true));
	}

	@Test
	void plateTakesFiveSeconds() {
		assertFalse(ForgingLogic.forged(1099, 1000, ForgingLogic.FORGE_TICKS));
		assertTrue(ForgingLogic.forged(1100, 1000, ForgingLogic.FORGE_TICKS));
		assertTrue(ForgingLogic.forged(1000, 1000, 0));
		assertEquals(50, ForgingLogic.progress(1050, 1000, ForgingLogic.FORGE_TICKS));
		assertEquals(100, ForgingLogic.progress(1500, 1000, ForgingLogic.FORGE_TICKS));
	}
}
