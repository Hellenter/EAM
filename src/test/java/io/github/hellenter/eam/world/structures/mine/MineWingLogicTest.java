package io.github.hellenter.eam.world.structures.mine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class MineWingLogicTest {
	/**
	 * The hall 0 with two branches: 1 - 2 - 3 - 4 and 5 - 6 - 7, where 6 also leads to 8. Piece 9 is not joined to the
	 * hall.
	 */
	private static final int[] PARENT = {-1, 0, 1, 2, 3, 0, 5, 6, 6, -1};

	@Test
	void aWingIsAPieceAndEverythingGrownFromIt() {
		assertTrue(MineWingLogic.inWing(PARENT, 2, 2));
		assertTrue(MineWingLogic.inWing(PARENT, 2, 4));
		assertFalse(MineWingLogic.inWing(PARENT, 2, 1));
		assertFalse(MineWingLogic.inWing(PARENT, 2, 6));
		assertEquals(4, MineWingLogic.size(PARENT, 1));
		assertEquals(3, MineWingLogic.size(PARENT, 6));
		assertEquals(10 - 1, MineWingLogic.size(PARENT, 0));
	}

	@Test
	void theWayInStaysOpen() {
		// The adit replaces dead end 4: no wing may hold it, so neither 1, 2 nor 3 starts one.
		List<Integer> roots = MineWingLogic.candidates(PARENT, 4, 3, 5);
		assertEquals(List.of(5, 6), roots);
	}

	@Test
	void wingsAreNeitherTooSmallNorHalfTheMine() {
		assertEquals(List.of(6), MineWingLogic.candidates(PARENT, 4, 3, 3));
		assertEquals(List.of(1, 2, 5, 6), MineWingLogic.candidates(PARENT, 9, 3, 4));
	}

	@Test
	void twoWingsMustNotShareAPiece() {
		assertTrue(MineWingLogic.disjoint(PARENT, 1, 5));
		assertFalse(MineWingLogic.disjoint(PARENT, 5, 6));
		assertFalse(MineWingLogic.disjoint(PARENT, 6, 5));
	}

	@Test
	void aWingFloodsOnlyWhenItRunsDeepEnough() {
		assertTrue(MineWingLogic.floods(77, 80));
		assertFalse(MineWingLogic.floods(78, 80));
		assertFalse(MineWingLogic.floods(84, 80));
	}

	@Test
	void rubbleIsTheSameInEveryChunkAndMostlyAHeap() {
		int[] counts = new int[4];
		for (int x = -64; x < 64; x++) {
			for (int z = -64; z < 64; z++) {
				int rubble = MineWingLogic.rubble(42L, x, z);
				assertEquals(rubble, MineWingLogic.rubble(42L, x, z));
				counts[rubble]++;
			}
		}
		int total = 128 * 128;
		// Most of the floor is a heap one block high or bare; a tunnel closed to the roof is the exception.
		assertTrue(counts[0] + counts[1] > total / 2, () -> java.util.Arrays.toString(counts));
		assertTrue(counts[3] > 0 && counts[3] < total / 5, () -> java.util.Arrays.toString(counts));
	}

	@Test
	void randomNumbersStayInRange() {
		for (int i = 0; i < 1000; i++) {
			double r = MineWingLogic.random(7L, i, -i, i * 3, 1);
			assertTrue(r >= 0.0 && r < 1.0);
		}
	}
}
