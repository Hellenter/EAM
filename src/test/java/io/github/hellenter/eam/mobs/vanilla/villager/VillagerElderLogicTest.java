package io.github.hellenter.eam.mobs.vanilla.villager;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class VillagerElderLogicTest {
	@Test
	void armsTheBestArmedFirst() {
		assertEquals(1, VillagerElderLogic.armsFirst(new int[] {0, 3, 2}));
	}

	@Test
	void onATieTheFirst() {
		assertEquals(0, VillagerElderLogic.armsFirst(new int[] {0, 0}));
	}

	@Test
	void nobodyToArm() {
		assertEquals(-1, VillagerElderLogic.armsFirst(new int[0]));
	}

	@Test
	void foodAndGoodsAreAllKept() {
		assertEquals(Integer.MAX_VALUE, VillagerElderLogic.keeps(VillagerElderLogic.Kind.FOOD, false, false, false, 4, 64));
		assertEquals(Integer.MAX_VALUE, VillagerElderLogic.keeps(VillagerElderLogic.Kind.GOODS, false, true, false, 4, 64));
	}

	@Test
	void clericGivesAllPotionsGuardKeepsHisShareOthersOne() {
		assertEquals(0, VillagerElderLogic.keeps(VillagerElderLogic.Kind.POTION, false, true, false, 4, 64));
		assertEquals(4, VillagerElderLogic.keeps(VillagerElderLogic.Kind.POTION, true, false, false, 4, 64));
		assertEquals(1, VillagerElderLogic.keeps(VillagerElderLogic.Kind.POTION, false, false, false, 4, 64));
	}

	@Test
	void onlyAGuardWithABowKeepsArrows() {
		assertEquals(64, VillagerElderLogic.keeps(VillagerElderLogic.Kind.ARROW, true, false, true, 4, 64));
		assertEquals(0, VillagerElderLogic.keeps(VillagerElderLogic.Kind.ARROW, true, false, false, 4, 64));
		assertEquals(0, VillagerElderLogic.keeps(VillagerElderLogic.Kind.ARROW, false, false, true, 4, 64));
	}

	@Test
	void otherThingsGoToTheElder() {
		assertEquals(0, VillagerElderLogic.keeps(VillagerElderLogic.Kind.OTHER, true, false, true, 4, 64));
	}
}
