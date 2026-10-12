package io.github.hellenter.eam.mobs.entity.upyr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UpyrLogicTest {
	@Test
	void garlicHalvesTheBlow() {
		assertEquals(2.0F, UpyrLogic.wardedDamage(4.0F, true, 0.5F), 1.0E-6F);
		assertEquals(4.0F, UpyrLogic.wardedDamage(4.0F, false, 0.5F), 1.0E-6F);
	}

	@Test
	void upyrDrinksWhatItsBiteTook() {
		assertEquals(4.0F, UpyrLogic.drunk(20.0F, 16.0F, 1.0F, false), 1.0E-6F);
		assertEquals(2.0F, UpyrLogic.drunk(20.0F, 16.0F, 0.5F, false), 1.0E-6F);
		// Absorption soaked the bite: nothing to drink.
		assertEquals(0.0F, UpyrLogic.drunk(20.0F, 20.0F, 1.0F, false), 1.0E-6F);
	}

	@Test
	void upyrWillNotDrinkFromGarlic() {
		assertEquals(0.0F, UpyrLogic.drunk(20.0F, 18.0F, 1.0F, true), 1.0E-6F);
	}

	@Test
	void upyrComesOnlyWhileFewAreAround() {
		assertTrue(UpyrLogic.spawnDue(0, 2, 0.1F, 0.2F));
		assertFalse(UpyrLogic.spawnDue(0, 2, 0.5F, 0.2F));
		assertFalse(UpyrLogic.spawnDue(2, 2, 0.0F, 0.2F));
	}

	@Test
	void upyrAppearsBetweenTheDistances() {
		assertEquals(20.0, UpyrLogic.spawnDistance(20.0, 36.0, 0.0F), 1.0E-6);
		assertEquals(28.0, UpyrLogic.spawnDistance(20.0, 36.0, 0.5F), 1.0E-6);
	}
}
