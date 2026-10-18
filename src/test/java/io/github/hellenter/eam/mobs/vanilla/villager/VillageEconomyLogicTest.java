package io.github.hellenter.eam.mobs.vanilla.villager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.Map;

class VillageEconomyLogicTest {
	@Test
	void farmerShareGrowsWithFieldsAndHoe() {
		// 100 farmland at 0.025 each and no base: 2.5 per half day, 5 a day, as with a wooden hoe.
		assertEquals(2.5, VillageEconomyLogic.yield(0, 0.025, 100, 1.0, 1, 0.25, 0, 0.15), 1.0E-9);
		// An iron hoe doubles it.
		assertEquals(5.0, VillageEconomyLogic.yield(0, 0.025, 100, 2.0, 1, 0.25, 0, 0.15), 1.0E-9);
	}

	@Test
	void levelAndComponentsRaiseTheShare() {
		assertEquals(2.0 * 1.5 * 1.3, VillageEconomyLogic.yield(2, 0, 0, 1.0, 3, 0.25, 2, 0.15), 1.0E-9);
	}

	@Test
	void oneGuardForEachEightGrownVillagers() {
		assertEquals(0, VillageEconomyLogic.guardsNeeded(7, 8));
		assertEquals(1, VillageEconomyLogic.guardsNeeded(8, 8));
		assertEquals(2, VillageEconomyLogic.guardsNeeded(17, 8));
		assertEquals(0, VillageEconomyLogic.guardsNeeded(17, 0));
	}

	@Test
	void nitwitsFirstThenJoblessThenTheBiggestTrade() {
		long nitwit = VillageEconomyLogic.guardRank(true, false, false, 1, 0);
		long jobless = VillageEconomyLogic.guardRank(false, true, false, 1, 0);
		long ofThree = VillageEconomyLogic.guardRank(false, false, false, 3, 50);
		long ofTwo = VillageEconomyLogic.guardRank(false, false, false, 2, 0);
		long ofThreeNovice = VillageEconomyLogic.guardRank(false, false, false, 3, 0);
		assertTrue(nitwit < jobless);
		assertTrue(jobless < ofThreeNovice);
		assertTrue(ofThreeNovice < ofThree);
		assertTrue(ofThree < ofTwo);
		assertEquals(Long.MAX_VALUE, VillageEconomyLogic.guardRank(false, false, false, 1, 0));
		assertEquals(Long.MAX_VALUE, VillageEconomyLogic.guardRank(true, false, true, 1, 0));
	}

	@Test
	void childrenNeedABedAndFood() {
		assertTrue(VillageEconomyLogic.canBreed(5, 6, 6, 6));
		assertFalse(VillageEconomyLogic.canBreed(6, 6, 100, 6));
		assertFalse(VillageEconomyLogic.canBreed(5, 6, 5.9, 6));
	}

	@Test
	void golemsByVillageSize() {
		assertEquals(1, VillageEconomyLogic.golemsAllowed(3, 8));
		assertEquals(2, VillageEconomyLogic.golemsAllowed(16, 8));
	}

	@Test
	void treasuryPaysAndNeverGoesBelowNothing() {
		VillageTreasury treasury = VillageTreasury.EMPTY.add(Map.of(VillageResource.WOOD, 10.0));
		assertTrue(VillageEconomyLogic.affords(treasury.stock(), Map.of(VillageResource.WOOD, 10.0)));
		assertFalse(VillageEconomyLogic.affords(treasury.stock(), Map.of(VillageResource.STONE, 1.0)));
		assertEquals(4.0, treasury.pay(Map.of(VillageResource.WOOD, 6.0)).get(VillageResource.WOOD), 1.0E-9);
		assertEquals(0.0, treasury.add(Map.of(VillageResource.PROVISIONS, -3.0)).get(VillageResource.PROVISIONS), 1.0E-9);
	}
}
