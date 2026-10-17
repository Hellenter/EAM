package io.github.hellenter.eam.content.underworld;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GlowshroomLogicTest {
	@Test
	void onlyBareTipsRipenUnderTheChance() {
		assertTrue(GlowshroomLogic.ripens(false, 0.01F, 0.05F));
		assertFalse(GlowshroomLogic.ripens(false, 0.05F, 0.05F));
		assertFalse(GlowshroomLogic.ripens(true, 0.0F, 0.05F));
		assertFalse(GlowshroomLogic.ripens(false, 0.0F, 0.0F));
	}

	@Test
	void ripeTipsShineBrighter() {
		assertEquals(10, GlowshroomLogic.light(true));
		assertTrue(GlowshroomLogic.light(false) < GlowshroomLogic.light(true));
	}
}
