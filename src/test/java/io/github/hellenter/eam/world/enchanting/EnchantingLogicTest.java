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
	void priceAddsUpPerResource() {
		EnchantingLogic.Price price = EnchantingLogic.Price.FREE.plus(true, 3, 3).plus(false, 4, 4).plus(true, 1, 1);
		assertEquals(new EnchantingLogic.Price(4, 4, 8), price);
	}

	@Test
	void needsBothResourcesAndLevels() {
		EnchantingLogic.Price price = new EnchantingLogic.Price(4, 2, 6);
		assertTrue(EnchantingLogic.affordable(price, 4, 2, false, 6, false));
		assertFalse(EnchantingLogic.affordable(price, 3, 2, false, 6, false));
		assertFalse(EnchantingLogic.affordable(price, 4, 1, false, 6, false));
		assertFalse(EnchantingLogic.affordable(price, 4, 2, false, 5, false));
		assertTrue(EnchantingLogic.affordable(price, 0, 0, false, 0, true));
	}

	@Test
	void sameResourceMustCoverTheSum() {
		EnchantingLogic.Price price = new EnchantingLogic.Price(4, 2, 0);
		assertFalse(EnchantingLogic.affordable(price, 5, 5, true, 0, false));
		assertTrue(EnchantingLogic.affordable(price, 6, 6, true, 0, false));
	}

	private static final List<List<Integer>> SHELVES = List.of(
			List.of(9),
			List.of(6, 9),
			List.of(6, 9, 12),
			List.of(3, 6, 9, 12),
			List.of(3, 6, 9, 12, 15)
	);

	@Test
	void maximsShelfExamples() {
		assertEquals(15, EnchantingLogic.shelvesForLevel(SHELVES, 5, 5)); // Sharpness V
		assertEquals(12, EnchantingLogic.shelvesForLevel(SHELVES, 3, 3)); // Looting III
		assertEquals(9, EnchantingLogic.shelvesForLevel(SHELVES, 1, 1)); // Silk Touch
		assertEquals(9, EnchantingLogic.shelvesForLevel(SHELVES, 2, 2)); // Fire Aspect II
		assertEquals(10, EnchantingLogic.shelvesForTotal(3, 5 + 3)); // Sharpness V + Looting III
	}

	@Test
	void minimumShelvesGiveOneLevel() {
		assertEquals(3, EnchantingLogic.shelvesForTotal(3, 1));
		assertEquals(3, EnchantingLogic.shelvesForTotal(3, 0));
		assertEquals(4, EnchantingLogic.shelvesForTotal(3, 2));
	}

	@Test
	void missingShelfRowNeedsThreePerLevel() {
		assertEquals(21, EnchantingLogic.shelvesForLevel(SHELVES, 10, 7));
	}
}
