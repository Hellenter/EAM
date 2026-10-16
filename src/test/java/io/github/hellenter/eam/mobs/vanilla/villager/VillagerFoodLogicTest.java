package io.github.hellenter.eam.mobs.vanilla.villager;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class VillagerFoodLogicTest {
	@Test
	void halfOfTheFeedGoesToLivestock() {
		assertEquals(8, VillagerFoodLogic.livestockShare(16, 0.5));
		assertEquals(3, VillagerFoodLogic.livestockShare(7, 0.5));
		assertEquals(0, VillagerFoodLogic.livestockShare(-4, 0.5));
	}

	@Test
	void theBankShrinksWithTheFood() {
		assertEquals(10, VillagerFoodLogic.bankLeft(10, 30));
		assertEquals(6, VillagerFoodLogic.bankLeft(10, 6));
		assertEquals(0, VillagerFoodLogic.bankLeft(-2, 6));
	}
}
