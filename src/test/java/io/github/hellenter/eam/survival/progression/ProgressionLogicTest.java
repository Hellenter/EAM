package io.github.hellenter.eam.survival.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ProgressionLogicTest {
	private static final ProgressionRules RULES = ProgressionRules.DEFAULT;

	@Test
	void barsGrowEveryFiveLevels() {
		assertEquals(6, ProgressionLogic.capacity(0, RULES));
		assertEquals(6, ProgressionLogic.capacity(4, RULES));
		assertEquals(8, ProgressionLogic.capacity(5, RULES));
		assertEquals(18, ProgressionLogic.capacity(30, RULES));
		assertEquals(20, ProgressionLogic.capacity(35, RULES));
		assertEquals(20, ProgressionLogic.capacity(Integer.MAX_VALUE, RULES));
		assertEquals(6, ProgressionLogic.capacity(-3, RULES));
	}

	@Test
	void gracePeriodLastsTwoMinutes() {
		assertTrue(ProgressionLogic.inGracePeriod(0, RULES));
		assertTrue(ProgressionLogic.inGracePeriod(2399, RULES));
		assertFalse(ProgressionLogic.inGracePeriod(2400, RULES));
	}
}
