package io.github.hellenter.eam.world.structures.mine;

import static io.github.hellenter.eam.world.structures.mine.UnderworldMineLogic.AIR;
import static io.github.hellenter.eam.world.structures.mine.UnderworldMineLogic.OTHER;
import static io.github.hellenter.eam.world.structures.mine.UnderworldMineLogic.SOLID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class UnderworldMineLogicTest {
	/** The mine: one corridor ending at x = 0, its floor at y 80. */
	private static final List<UnderworldMineLogic.Box> MINE = List.of(new UnderworldMineLogic.Box(-15, 80, -1, -1, 83, 1));

	/** Rock up to x 19, then a cave whose floor is at {@code floor}. */
	private static UnderworldMineLogic.Terrain caveFrom(int caveX, int floor) {
		return (x, y, z) -> x >= caveX && y > floor && y < floor + 20 ? AIR : SOLID;
	}

	@Test
	void digsOnToTheFirstCaveFloorClearOfTheMine() {
		UnderworldMineLogic.Adit adit = UnderworldMineLogic.find(caveFrom(8, 80), MINE, 0, 80, 0, 1, 0, 40, 57, 180);
		assertNotNull(adit);
		// The cave starts at x 8, but the terrain closes in up to 12 blocks around the corridor (which ends at x -1).
		assertEquals(11, adit.length());
		assertEquals(80, adit.endFloor());
	}

	@Test
	void stepsDownOneBlockPerCell() {
		UnderworldMineLogic.Adit adit = UnderworldMineLogic.find(caveFrom(14, 75), MINE, 0, 80, 0, 1, 0, 40, 57, 180);
		assertNotNull(adit);
		assertEquals(75, adit.endFloor());
		assertEquals(80, adit.floor(0));
		assertEquals(80, adit.floor(1));
		assertEquals(79, adit.floor(2));
		assertEquals(75, adit.floor(6));
		assertEquals(75, adit.floor(adit.length()));
		// The stair stands in the higher cell and climbs back towards the mine.
		assertEquals(-1, UnderworldMineLogic.stair(adit, 1));
		assertEquals(-1, UnderworldMineLogic.stair(adit, 5));
		assertEquals(0, UnderworldMineLogic.stair(adit, 6));
		assertEquals(0, UnderworldMineLogic.stair(adit, 0));
	}

	@Test
	void climbsTowardsAHigherFloor() {
		UnderworldMineLogic.Adit adit = UnderworldMineLogic.find(caveFrom(14, 84), MINE, 0, 80, 0, 1, 0, 40, 57, 180);
		assertNotNull(adit);
		assertEquals(84, adit.endFloor());
		assertEquals(0, UnderworldMineLogic.stair(adit, 1));
		assertEquals(1, UnderworldMineLogic.stair(adit, 2));
		assertEquals(1, UnderworldMineLogic.stair(adit, 5));
	}

	@Test
	void runsSidewaysAlongZ() {
		UnderworldMineLogic.Terrain cave = (x, y, z) -> z <= -20 && y > 80 && y < 100 ? AIR : SOLID;
		UnderworldMineLogic.Adit adit = UnderworldMineLogic.find(cave, List.of(), 0, 80, 0, 0, -1, 40, 57, 180);
		assertNotNull(adit);
		assertEquals(20, adit.length());
		assertEquals(-20, adit.z(20, 0));
		assertEquals(1, adit.x(20, 1));
	}

	@Test
	void givesUpWithoutACaveInReach() {
		assertNull(UnderworldMineLogic.find(caveFrom(50, 80), MINE, 0, 80, 0, 1, 0, 40, 57, 180));
		// A cave floor 30 blocks down is too steep for an adit of this length.
		assertNull(UnderworldMineLogic.find(caveFrom(14, 50), MINE, 0, 80, 0, 1, 0, 20, 57, 180));
	}

	@Test
	void neverDigsIntoLava() {
		UnderworldMineLogic.Terrain lava = (x, y, z) -> x == 5 && y == 81 ? OTHER : caveFrom(14, 80).kind(x, y, z);
		assertNull(UnderworldMineLogic.find(lava, MINE, 0, 80, 0, 1, 0, 40, 57, 180));
	}

	@Test
	void neverCutsThroughAnotherPiece() {
		List<UnderworldMineLogic.Box> pieces = List.of(MINE.getFirst(), new UnderworldMineLogic.Box(6, 70, -3, 8, 90, 3));
		assertNull(UnderworldMineLogic.find(caveFrom(30, 80), pieces, 0, 80, 0, 1, 0, 40, 57, 180));
	}

	@Test
	void framesOnlyLevelCells() {
		UnderworldMineLogic.Adit level = new UnderworldMineLogic.Adit(0, 80, 0, 1, 0, 80, 20);
		assertTrue(UnderworldMineLogic.frame(level, 3));
		assertTrue(UnderworldMineLogic.frame(level, 8));
		assertFalse(UnderworldMineLogic.frame(level, 4));
		UnderworldMineLogic.Adit ramp = new UnderworldMineLogic.Adit(0, 80, 0, 1, 0, 70, 20);
		assertFalse(UnderworldMineLogic.frame(ramp, 3));
		assertTrue(UnderworldMineLogic.frame(ramp, 13));
	}
}
