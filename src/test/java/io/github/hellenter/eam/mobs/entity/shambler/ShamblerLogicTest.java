package io.github.hellenter.eam.mobs.entity.shambler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ShamblerLogicTest {
	@Test
	void standsUpWhenAPlayerComesClose() {
		assertTrue(ShamblerLogic.wakes(4.9 * 4.9, 5.0));
		assertTrue(ShamblerLogic.wakes(25.0, 5.0));
		assertFalse(ShamblerLogic.wakes(5.1 * 5.1, 5.0));
	}

	@Test
	void settlesDownOnlyWhenLeftAloneLongEnough() {
		assertTrue(ShamblerLogic.settles(300, 300, false, Double.POSITIVE_INFINITY, 5.0));
		assertFalse(ShamblerLogic.settles(299, 300, false, Double.POSITIVE_INFINITY, 5.0));
		// Burning, it panics instead.
		assertFalse(ShamblerLogic.settles(300, 300, true, Double.POSITIVE_INFINITY, 5.0));
		// A player still nearby (within twice the wake distance) keeps it up.
		assertFalse(ShamblerLogic.settles(300, 300, false, 9.0 * 9.0, 5.0));
		assertTrue(ShamblerLogic.settles(300, 300, false, 11.0 * 11.0, 5.0));
	}

	@Test
	void plainOneTakesMoreWhileBurning() {
		assertEquals(8.0F, ShamblerLogic.damageTaken(4.0F, true, false, 2.0F), 1.0E-6F);
		assertEquals(4.0F, ShamblerLogic.damageTaken(4.0F, false, false, 2.0F), 1.0E-6F);
		// A smoldering one never minds fire.
		assertEquals(4.0F, ShamblerLogic.damageTaken(4.0F, true, true, 2.0F), 1.0E-6F);
	}

	@Test
	void plainOneBurnsLong() {
		assertEquals(200, ShamblerLogic.fireTicks(60, 200));
		assertEquals(300, ShamblerLogic.fireTicks(300, 200));
		assertEquals(0, ShamblerLogic.fireTicks(0, 200));
	}
}
