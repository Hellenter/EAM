package io.github.hellenter.eam.mobs.vanilla.villager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VillagerClericLogicTest {
	@Test
	void healingWaitsForHalfHealth() {
		assertFalse(VillagerClericLogic.drinks(true, 11.0F, 20.0F, true, false));
		assertTrue(VillagerClericLogic.drinks(true, 10.0F, 20.0F, false, false));
	}

	@Test
	void otherPotionsOnlyInAFightAndNotTwice() {
		assertFalse(VillagerClericLogic.drinks(false, 20.0F, 20.0F, false, false));
		assertTrue(VillagerClericLogic.drinks(false, 20.0F, 20.0F, true, false));
		assertFalse(VillagerClericLogic.drinks(false, 20.0F, 20.0F, true, true));
	}
}
