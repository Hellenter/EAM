package io.github.hellenter.eam.world.structures.castle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;

import org.junit.jupiter.api.Test;

import java.util.Set;

class CastleLogicTest {
	private static final int HALF_X = CastleLogic.halfSamples(80);
	private static final int HALF_Z = CastleLogic.halfSamples(59);
	private static final int WIDTH = 2 * CastleLogic.reach(Math.max(HALF_X, HALF_Z)) + 1;

	@Test
	void samplesCoverTheFootprint() {
		assertEquals(5, HALF_X);
		assertEquals(4, HALF_Z);
		assertTrue(HALF_X * CastleLogic.STEP >= 40);
		assertTrue(HALF_Z * CastleLogic.STEP >= 30);
	}

	private static int[][] flat(int height) {
		int[][] ground = new int[WIDTH][WIDTH];
		for (int[] row : ground) java.util.Arrays.fill(row, height);
		return ground;
	}

	@Test
	void flatLandTakesTheChunkMiddle() {
		CastleLogic.Site site = CastleLogic.site(flat(70), new boolean[WIDTH][WIDTH], HALF_X, HALF_Z, 10);
		assertNotNull(site);
		assertEquals(0, site.dx());
		assertEquals(0, site.dz());
		assertEquals(70, site.ground());
	}

	@Test
	void standsOnTheHighestGroundOfItsFootprint() {
		int[][] ground = flat(70);
		ground[WIDTH / 2 + 2][WIDTH / 2 - 1] = 75;
		CastleLogic.Site site = CastleLogic.site(ground, new boolean[WIDTH][WIDTH], HALF_X, HALF_Z, 10);
		assertNotNull(site);
		assertEquals(75, site.ground());
	}

	@Test
	void movesAwayFromAHillTooSteep() {
		int[][] ground = flat(70);
		// A spike at the west edge of the middle footprint: the castle shifts east, clear of it.
		ground[WIDTH / 2 - HALF_X][WIDTH / 2] = 100;
		CastleLogic.Site site = CastleLogic.site(ground, new boolean[WIDTH][WIDTH], HALF_X, HALF_Z, 10);
		assertNotNull(site);
		assertTrue(site.dx() > 0);
		assertEquals(70, site.ground());
	}

	@Test
	void wetOrSteepEverywhereGivesNoSite() {
		boolean[][] wet = new boolean[WIDTH][WIDTH];
		for (boolean[] row : wet) java.util.Arrays.fill(row, true);
		assertNull(CastleLogic.site(flat(70), wet, HALF_X, HALF_Z, 10));
		int[][] ground = new int[WIDTH][WIDTH];
		for (int x = 0; x < WIDTH; x++) for (int z = 0; z < WIDTH; z++) ground[x][z] = 60 + 3 * x;
		assertNull(CastleLogic.site(ground, new boolean[WIDTH][WIDTH], HALF_X, HALF_Z, 10));
	}

	@Test
	void woolInTheOpenMarksAMob() {
		BlockPos wool = new BlockPos(5, 3, 5);
		assertTrue(CastleLogic.marksMob(wool, Set.of(wool.east())::contains));
		assertTrue(CastleLogic.marksMob(wool, Set.of(wool.north(), wool.south())::contains));
	}

	@Test
	void woolLaidIntoAFloorIsOnlyWool() {
		BlockPos wool = new BlockPos(5, 3, 5);
		// Open above and below does not count: the floor around it is solid.
		assertFalse(CastleLogic.marksMob(wool, Set.of(wool.above(), wool.above(2))::contains));
	}
}
