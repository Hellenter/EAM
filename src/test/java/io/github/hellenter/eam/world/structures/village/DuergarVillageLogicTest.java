package io.github.hellenter.eam.world.structures.village;

import static io.github.hellenter.eam.world.structures.village.DuergarVillageLogic.AIR;
import static io.github.hellenter.eam.world.structures.village.DuergarVillageLogic.NONE;
import static io.github.hellenter.eam.world.structures.village.DuergarVillageLogic.SOLID;
import static io.github.hellenter.eam.world.structures.village.DuergarVillageLogic.WATER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.IntUnaryOperator;

import org.junit.jupiter.api.Test;

class DuergarVillageLogicTest {
	/** Rock up to 80, a cave to 100, rock to 120, a second cave above. */
	private static final IntUnaryOperator TWO_CAVES = y -> y <= 80 || (y > 100 && y <= 120) ? SOLID : AIR;

	@Test
	void findsEveryFloorHighestFirst() {
		assertEquals(List.of(120, 80), DuergarVillageLogic.floors(TWO_CAVES, 60, 130));
	}

	@Test
	void needsRoomToStand() {
		IntUnaryOperator lowGap = y -> y == 81 || y == 82 ? AIR : SOLID;
		assertTrue(DuergarVillageLogic.floors(lowGap, 60, 130).isEmpty());
	}

	@Test
	void shallowWaterIsAFloorDeepWaterIsNot() {
		IntUnaryOperator shallow = y -> y <= 80 ? SOLID : y == 81 ? WATER : AIR;
		IntUnaryOperator deep = y -> y <= 80 ? SOLID : y <= 83 ? WATER : AIR;
		assertTrue(DuergarVillageLogic.isFloor(shallow, 80));
		assertFalse(DuergarVillageLogic.isFloor(deep, 80));
	}

	@Test
	void findsTheNearestFloorWithinRange() {
		assertEquals(80, DuergarVillageLogic.floorNear(TWO_CAVES, 84, 6));
		assertEquals(NONE, DuergarVillageLogic.floorNear(TWO_CAVES, 90, 6));
		assertEquals(120, DuergarVillageLogic.floorNear(TWO_CAVES, 117, 6));
	}

	@Test
	void hangsOverOpenAirOnly() {
		assertTrue(DuergarVillageLogic.hangs(TWO_CAVES, 90));
		assertFalse(DuergarVillageLogic.hangs(TWO_CAVES, 81));
	}

	@Test
	void buriedWhenTheFloorStandsTwoBlocksHigher() {
		assertTrue(DuergarVillageLogic.buried(TWO_CAVES, 78));
		assertFalse(DuergarVillageLogic.buried(TWO_CAVES, 79));
		assertFalse(DuergarVillageLogic.buried(TWO_CAVES, 80));
		IntUnaryOperator pond = y -> y <= 80 ? SOLID : y <= 82 ? WATER : AIR;
		assertFalse(DuergarVillageLogic.buried(pond, 80));
	}

	@Test
	void stepsAtMostMaxStepFromItsParent() {
		assertEquals(3, DuergarVillageLogic.step(7, 2, 1));
		assertEquals(-1, DuergarVillageLogic.step(-5, 0, 1));
		assertEquals(2, DuergarVillageLogic.step(2, 2, 1));
		assertEquals(4, DuergarVillageLogic.median(new int[] {9, 4, 1, 4, 5}));
	}

	@Test
	void fitsAStairToTheFloor() {
		assertEquals(1, DuergarVillageLogic.stairRise(1, 1, 4));
		assertEquals(3, DuergarVillageLogic.stairRise(3, 1, 4));
		assertEquals(4, DuergarVillageLogic.stairRise(6, 1, 4));
		assertEquals(-2, DuergarVillageLogic.stairRise(-2, 1, 4));
		assertEquals(-4, DuergarVillageLogic.stairRise(-9, -1, 4));
		assertEquals(-1, DuergarVillageLogic.stairRise(0, -1, 4));
		assertEquals(1, DuergarVillageLogic.stairRise(0, 1, 4));
	}
}
