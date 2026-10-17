package io.github.hellenter.eam.world.structures.outpost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OutpostLogicTest {
	@Test
	void clearingReachesRadiusFromEachSide() {
		assertTrue(OutpostLogic.inClearing(0, 0, 32, 32, 15, 16, 16));
		assertTrue(OutpostLogic.inClearing(0, 0, 32, 32, 15, -15, 10));
		assertTrue(OutpostLogic.inClearing(0, 0, 32, 32, 15, 47, 32));
		assertFalse(OutpostLogic.inClearing(0, 0, 32, 32, 15, -16, 10));
		assertFalse(OutpostLogic.inClearing(0, 0, 32, 32, 15, 10, 48));
	}

	@Test
	void clearingHasRoundedCorners() {
		assertTrue(OutpostLogic.inClearing(0, 0, 32, 32, 15, -10, -10));
		assertFalse(OutpostLogic.inClearing(0, 0, 32, 32, 15, -12, -12));
	}

	private static final int WIDTH = 2 * OutpostLogic.REACH + 1;

	/** Ground rising {@code slopeX} per sample to the east and {@code slopeZ} per sample to the south, from 64. */
	private static int[][] slope(int slopeX, int slopeZ) {
		int[][] ground = new int[WIDTH][WIDTH];
		for (int x = 0; x < WIDTH; x++) {
			for (int z = 0; z < WIDTH; z++) ground[x][z] = 64 + slopeX * x + slopeZ * z;
		}
		return ground;
	}

	@Test
	void flatGroundKeepsTheChunkMiddle() {
		OutpostLogic.Site site = OutpostLogic.site(slope(0, 0), new boolean[WIDTH][WIDTH], 10, 0);
		assertEquals(new OutpostLogic.Site(0, 0, 0, 64), site);
	}

	@Test
	void gateFacesDownhillAndOutpostStandsOnTheHighSide() {
		// Rising to the east: the gate faces west and the outpost stands at its eastern edge's height.
		OutpostLogic.Site east = OutpostLogic.site(slope(1, 0), new boolean[WIDTH][WIDTH], 10, 0);
		assertEquals(3, east.gate());
		assertEquals(64 + OutpostLogic.REACH + 2, east.ground());
		// Rising to the north: the gate faces south.
		assertEquals(2, OutpostLogic.site(slope(0, -1), new boolean[WIDTH][WIDTH], 10, 1).gate());
	}

	@Test
	void prefersTheFlattestMiddle() {
		int[][] ground = slope(0, 0);
		// A hill on the western samples: the outpost moves east, off it.
		for (int z = 0; z < WIDTH; z++) {
			for (int x = 0; x <= OutpostLogic.REACH - 2; x++) ground[x][z] = 72;
		}
		OutpostLogic.Site site = OutpostLogic.site(ground, new boolean[WIDTH][WIDTH], 10, 0);
		assertEquals(1, site.dx());
		assertEquals(64, site.ground());
	}

	@Test
	void rejectsSteepOrWetSites() {
		assertNull(OutpostLogic.site(slope(3, 0), new boolean[WIDTH][WIDTH], 10, 0));
		boolean[][] wet = new boolean[WIDTH][WIDTH];
		for (int x = 0; x < WIDTH; x++) wet[x][OutpostLogic.REACH] = true;
		assertNull(OutpostLogic.site(slope(0, 0), wet, 10, 0));
	}

	@Test
	void bankSlopesDownFromTheGrass() {
		assertEquals(0, OutpostLogic.distance(0, 0, 32, 32, 10, 10));
		assertEquals(3, OutpostLogic.distance(0, 0, 32, 32, 35, 10));
		assertEquals(70, OutpostLogic.bankTop(70, 1));
		assertEquals(66, OutpostLogic.bankTop(70, 5));
	}
}
