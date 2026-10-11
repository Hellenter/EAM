package io.github.hellenter.eam.mobs.entity.spider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WoodSpiderLogicTest {
	@Test
	void seesHalfAsFarInBrightLight() {
		assertEquals(16.0, WoodSpiderLogic.followDistance(16.0, 0.3F, 0.5));
		assertEquals(8.0, WoodSpiderLogic.followDistance(16.0, 0.9F, 0.5));
	}

	@Test
	void poisonGrowsWithDifficulty() {
		assertEquals(0, WoodSpiderLogic.poisonTicks(100, 0));
		assertEquals(50, WoodSpiderLogic.poisonTicks(100, 1));
		assertEquals(100, WoodSpiderLogic.poisonTicks(100, 2));
		assertEquals(150, WoodSpiderLogic.poisonTicks(100, 3));
	}

	@Test
	void spawnsInDaylightOnlyOnLeavesAndRarely() {
		assertTrue(WoodSpiderLogic.lightAllows(true, false, 0.9F, 0.03F));
		assertFalse(WoodSpiderLogic.lightAllows(false, false, 0.0F, 0.03F));
		assertTrue(WoodSpiderLogic.lightAllows(false, true, 0.01F, 0.03F));
		assertFalse(WoodSpiderLogic.lightAllows(false, true, 0.5F, 0.03F));
	}
}
