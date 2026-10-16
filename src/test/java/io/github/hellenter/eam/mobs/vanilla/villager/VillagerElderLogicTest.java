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
}
