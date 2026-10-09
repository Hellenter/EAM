package io.github.hellenter.eam.world.enchanting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.List;

class EnchantingLogicTest {
	private static final List<List<Integer>> COSTS = List.of(
			List.of(4),
			List.of(3, 4),
			List.of(2, 3, 4),
			List.of(1, 2, 3, 4),
			List.of(1, 2, 3, 4, 5)
	);

	@Test
	void everyTopLevelCostsFourBelowFiveLevels() {
		assertEquals(4, EnchantingLogic.resourceCost(COSTS, 1, 1));
		assertEquals(3, EnchantingLogic.resourceCost(COSTS, 2, 1));
		assertEquals(4, EnchantingLogic.resourceCost(COSTS, 2, 2));
		assertEquals(2, EnchantingLogic.resourceCost(COSTS, 3, 1));
		assertEquals(4, EnchantingLogic.resourceCost(COSTS, 3, 3));
		assertEquals(1, EnchantingLogic.resourceCost(COSTS, 4, 1));
		assertEquals(4, EnchantingLogic.resourceCost(COSTS, 4, 4));
	}

	@Test
	void fiveLevelsCostTheLevel() {
		for (int level = 1; level <= 5; level++) {
			assertEquals(level, EnchantingLogic.resourceCost(COSTS, 5, level));
		}
	}

	@Test
	void missingRowFallsBackToTheLevel() {
		assertEquals(7, EnchantingLogic.resourceCost(COSTS, 10, 7));
		assertEquals(2, EnchantingLogic.resourceCost(List.of(), 3, 2));
	}

	@Test
	void levelsMatchResources() {
		assertEquals(3, EnchantingLogic.levelCost(3, 1));
		assertEquals(6, EnchantingLogic.levelCost(3, 2));
	}

	@Test
	void needsBothResourcesAndLevels() {
		assertTrue(EnchantingLogic.affordable(4, 4, 4, 4, false));
		assertFalse(EnchantingLogic.affordable(3, 4, 10, 4, false));
		assertFalse(EnchantingLogic.affordable(10, 4, 3, 4, false));
		assertTrue(EnchantingLogic.affordable(0, 4, 0, 4, true));
	}
}
