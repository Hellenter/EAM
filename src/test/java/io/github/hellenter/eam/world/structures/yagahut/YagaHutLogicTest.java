package io.github.hellenter.eam.world.structures.yagahut;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class YagaHutLogicTest {
	@Test
	void groundIsTheMiddleHeightAboveTheWater() {
		assertEquals(65, YagaHutLogic.groundLevel(new int[] {64, 65, 70, 65, 63}, 63));
		assertEquals(63, YagaHutLogic.groundLevel(new int[] {60, 61, 62, 64, 61}, 63));
	}

	@Test
	void levelInsideThenEasesBack() {
		assertEquals(65, YagaHutLogic.targetTop(65, 80, 0));
		assertEquals(65, YagaHutLogic.targetTop(65, 80, YagaHutLogic.FLAT_RADIUS));
		assertEquals(80, YagaHutLogic.targetTop(65, 80, YagaHutLogic.FLAT_RADIUS + YagaHutLogic.BLEND));
		int half = YagaHutLogic.targetTop(65, 80, YagaHutLogic.FLAT_RADIUS + YagaHutLogic.BLEND / 2.0);
		assertTrue(half > 65 && half < 80);
	}

	@Test
	void onlyLakesOnTheSlopeKeepTheirWater() {
		assertFalse(YagaHutLogic.keepsWater(10, 60, 63));
		assertTrue(YagaHutLogic.keepsWater(36, 61, 63));
		assertFalse(YagaHutLogic.keepsWater(36, 63, 63));
		assertTrue(YagaHutLogic.inReach(40));
		assertFalse(YagaHutLogic.inReach(40.5));
	}
}
