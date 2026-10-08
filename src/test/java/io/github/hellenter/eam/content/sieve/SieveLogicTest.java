package io.github.hellenter.eam.content.sieve;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SieveLogicTest {
	@Test
	void meshTearsInThreeStages() {
		assertEquals(0, SieveLogic.wear(0));
		assertEquals(0, SieveLogic.wear(10));
		assertEquals(1, SieveLogic.wear(11));
		assertEquals(1, SieveLogic.wear(21));
		assertEquals(2, SieveLogic.wear(22));
		assertEquals(2, SieveLogic.wear(31));
		assertEquals(2, SieveLogic.wear(SieveLogic.USES));
	}

	@Test
	void breaksAfterThirtyTwoSifts() {
		assertFalse(SieveLogic.wornOut(31));
		assertTrue(SieveLogic.wornOut(32));
	}

	@Test
	void gravelShrinksWhileSifting() {
		int ticks = SieveSettings.DEFAULT.siftTicks();
		assertEquals(4, SieveLogic.gravelLeft(0, ticks));
		assertEquals(4, SieveLogic.gravelLeft(24, ticks));
		assertEquals(3, SieveLogic.gravelLeft(25, ticks));
		assertEquals(1, SieveLogic.gravelLeft(99, ticks));
		assertFalse(SieveLogic.done(99, ticks));
		assertTrue(SieveLogic.done(100, ticks));
	}
}
