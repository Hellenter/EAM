package io.github.hellenter.eam.survival.death;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DeathLogicTest {
	@Test
	void losesOnlyTheGivenLevels() {
		assertEquals(25, DeathLogic.levelsAfterDeath(30, 5));
		assertEquals(0, DeathLogic.levelsAfterDeath(5, 5));
	}

	@Test
	void neverGoesBelowZero() {
		assertEquals(0, DeathLogic.levelsAfterDeath(3, 5));
		assertEquals(7, DeathLogic.levelsAfterDeath(7, -2));
	}
}
