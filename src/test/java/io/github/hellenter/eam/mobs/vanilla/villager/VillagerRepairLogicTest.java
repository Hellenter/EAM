package io.github.hellenter.eam.mobs.vanilla.villager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VillagerRepairLogicTest {
	@Test
	void oneMasterServesTwelve() {
		assertTrue(VillagerRepairLogic.served(11, 1, 12));
		assertFalse(VillagerRepairLogic.served(12, 1, 12));
		assertTrue(VillagerRepairLogic.served(12, 2, 12));
	}

	@Test
	void noMasterServesNobody() {
		assertFalse(VillagerRepairLogic.served(0, 0, 12));
	}

	@Test
	void mendingTakesAShareAndStopsAtFull() {
		assertEquals(100 - 63, VillagerRepairLogic.mended(100, 250, 0.25));
		assertEquals(0, VillagerRepairLogic.mended(10, 250, 0.25));
		assertEquals(4, VillagerRepairLogic.mended(5, 3, 0.1));
	}

	@Test
	void eachVillagerChecksOncePerInterval() {
		int checks = 0;
		for (long time = 0; time < 1200; time++) {
			if (VillagerRepairLogic.checksNow(time, 7, 1200)) checks++;
		}
		assertEquals(1, checks);
	}
}
