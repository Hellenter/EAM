package io.github.hellenter.eam.mobs.entity.warg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WargLogicTest {
	@Test
	void offAngleIsMeasuredFromTheLook() {
		assertEquals(0.0, WargLogic.offAngle(0.0, 1.0, 0.0, 5.0), 1.0E-6);
		assertEquals(90.0, WargLogic.offAngle(0.0, 1.0, 3.0, 0.0), 1.0E-6);
		assertEquals(180.0, WargLogic.offAngle(0.0, 1.0, 0.0, -2.0), 1.0E-6);
		assertEquals(45.0, WargLogic.offAngle(1.0, 0.0, 1.0, 1.0), 1.0E-6);
	}

	@Test
	void lookingStraightDownWatchesEverything() {
		assertEquals(0.0, WargLogic.offAngle(0.0, 0.0, 1.0, 1.0), 1.0E-6);
	}

	@Test
	void watchedInsideTheCone() {
		assertTrue(WargLogic.watched(30.0, 60.0));
		assertFalse(WargLogic.watched(90.0, 60.0));
	}

	@Test
	void strikerIsTheOneFurthestBehind() {
		assertEquals(2, WargLogic.pickStriker(new double[] {10.0, 100.0, 170.0, 120.0}, 60.0));
	}

	@Test
	void noStrikeWhileEveryWargIsWatched() {
		assertEquals(-1, WargLogic.pickStriker(new double[] {10.0, 40.0, 59.0}, 60.0));
	}

	@Test
	void wargsNotReadyAreSkipped() {
		assertEquals(1, WargLogic.pickStriker(new double[] {-1.0, 90.0, -1.0}, 60.0));
		assertEquals(-1, WargLogic.pickStriker(new double[] {}, 60.0));
	}

	@Test
	void slotsSpreadEvenly() {
		assertEquals(0.0, WargLogic.slotAngle(0.0, 0, 4), 1.0E-9);
		assertEquals(Math.PI / 2.0, WargLogic.slotAngle(0.0, 1, 4), 1.0E-9);
		assertEquals(Math.PI + 0.5, WargLogic.slotAngle(0.5, 2, 4), 1.0E-9);
	}

	@Test
	void packSizeCoversTheRange() {
		assertEquals(3, WargLogic.packSize(3, 5, 0.0));
		assertEquals(4, WargLogic.packSize(3, 5, 0.5));
		assertEquals(5, WargLogic.packSize(3, 5, 0.999));
		assertEquals(4, WargLogic.packSize(4, 4, 0.7));
		assertEquals(3, WargLogic.packSize(5, 3, 0.0));
	}
}
