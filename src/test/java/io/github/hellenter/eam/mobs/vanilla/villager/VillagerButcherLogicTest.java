package io.github.hellenter.eam.mobs.vanilla.villager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VillagerButcherLogicTest {
	@Test
	void leavesAFewToBreed() {
		assertFalse(VillagerButcherLogic.slaughters(3, 3));
		assertTrue(VillagerButcherLogic.slaughters(4, 3));
	}
}
