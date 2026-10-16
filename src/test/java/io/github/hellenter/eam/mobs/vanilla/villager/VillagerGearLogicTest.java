package io.github.hellenter.eam.mobs.vanilla.villager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VillagerGearLogicTest {
	@Test
	void emptyHandTakesTheWorkItem() {
		assertTrue(VillagerGearLogic.resetsHand(true, false, false, true));
	}

	@Test
	void emptyHandStaysEmptyWithoutAWorkItem() {
		assertFalse(VillagerGearLogic.resetsHand(true, false, false, false));
	}

	@Test
	void anOldTradesWorkItemIsSwapped() {
		assertTrue(VillagerGearLogic.resetsHand(false, true, false, true));
		assertTrue(VillagerGearLogic.resetsHand(false, true, false, false));
	}

	@Test
	void theRightWorkItemStays() {
		assertFalse(VillagerGearLogic.resetsHand(false, true, true, true));
	}

	@Test
	void aPlayersItemIsNeverTouched() {
		assertFalse(VillagerGearLogic.resetsHand(false, false, false, true));
	}
}
