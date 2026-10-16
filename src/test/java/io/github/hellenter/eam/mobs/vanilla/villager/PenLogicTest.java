package io.github.hellenter.eam.mobs.vanilla.villager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PenLogicTest {
	/** A flat 5x5 pen at height 0 fenced on all sides, with an optional gate at (2, -3). */
	private static PenLogic.Terrain pen(boolean gate) {
		return (x, y, z) -> {
			if (gate && x == 2 && z == -3) return PenLogic.DOOR;
			if (Math.abs(x) == 3 || Math.abs(z) == 3) return PenLogic.WALL;
			return 0;
		};
	}

	@Test
	void fenceWithoutGateIsAClosedPen() {
		assertTrue(PenLogic.closedPen(pen(false), 0, 0, 0, 1024));
	}

	@Test
	void aGateOpensThePen() {
		assertFalse(PenLogic.closedPen(pen(true), 0, 0, 0, 1024));
	}

	@Test
	void openFieldIsNoPen() {
		assertFalse(PenLogic.closedPen((x, y, z) -> 0, 0, 0, 0, 1024));
	}

	@Test
	void hillsAreWalkedOver() {
		PenLogic.Terrain hill = (x, y, z) -> Math.abs(x) == 3 || Math.abs(z) == 3 ? PenLogic.WALL : x > 0 ? 1 : 0;
		assertTrue(PenLogic.closedPen(hill, 0, 0, 0, 1024));
	}
}
