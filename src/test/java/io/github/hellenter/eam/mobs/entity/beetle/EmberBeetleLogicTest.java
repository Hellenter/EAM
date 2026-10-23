package io.github.hellenter.eam.mobs.entity.beetle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EmberBeetleLogicTest {
	@Test
	void noticesOnlyPlayersCloserThanTheRange() {
		assertTrue(EmberBeetleLogic.notices(3.9 * 3.9, 4.0));
		assertFalse(EmberBeetleLogic.notices(4.0 * 4.0, 4.0));
		assertFalse(EmberBeetleLogic.notices(10.0 * 10.0, 4.0));
		assertFalse(EmberBeetleLogic.notices(0.0, 0.0));
	}

	@Test
	void crushingBlowsCrackTheShell() {
		assertEquals(9.0F, EmberBeetleLogic.shellDamage(6.0F, true, 1.5F), 1.0E-6F);
		assertEquals(6.0F, EmberBeetleLogic.shellDamage(6.0F, false, 1.5F), 1.0E-6F);
	}

	@Test
	void bellyFillsUpToTheLimit() {
		assertTrue(EmberBeetleLogic.hungry(2, 3));
		assertFalse(EmberBeetleLogic.hungry(3, 3));
		assertEquals(1, EmberBeetleLogic.afterEating(0, 3));
		assertEquals(3, EmberBeetleLogic.afterEating(3, 3));
	}
}
