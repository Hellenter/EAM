package io.github.hellenter.eam.survival.sleep;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.hellenter.eam.survival.sleep.SafeRoomLogic.Cell;
import io.github.hellenter.eam.survival.sleep.SafeRoomLogic.Result;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

class SafeRoomLogicTest {
	private static final SafeRoomSettings SETTINGS = SafeRoomSettings.DEFAULT;
	/** The bed lies at (1, 1, 1) and (2, 1, 1). */
	private static final int[][] BED = {{1, 1, 1}, {2, 1, 1}};

	/** Open air everywhere except the blocks set; the room is lit at the bed unless the light is changed. */
	private static final class World implements SafeRoomLogic.RoomView {
		final Map<String, Cell> cells = new HashMap<>();
		int light = 14;

		/** A hollow box with walls of {@code wall} from (0,0,0) to (sizeX+1, height+1, sizeZ+1); the bed is weak. */
		World box(int sizeX, int height, int sizeZ, Cell wall) {
			for (int x = 0; x <= sizeX + 1; x++) {
				for (int y = 0; y <= height + 1; y++) {
					for (int z = 0; z <= sizeZ + 1; z++) {
						boolean shell = x == 0 || y == 0 || z == 0 || x == sizeX + 1 || y == height + 1 || z == sizeZ + 1;
						if (shell) set(x, y, z, wall);
					}
				}
			}
			set(1, 1, 1, Cell.WEAK);
			set(2, 1, 1, Cell.WEAK);
			return this;
		}

		World set(int x, int y, int z, Cell cell) {
			cells.put(x + "," + y + "," + z, cell);
			return this;
		}

		@Override
		public Cell cell(int x, int y, int z) {
			return cells.getOrDefault(x + "," + y + "," + z, Cell.OPEN);
		}

		@Override
		public int blockLight(int x, int y, int z) {
			return light;
		}
	}

	@Test
	void stoneRoomWithTorchIsSafe() {
		assertEquals(Result.SAFE, SafeRoomLogic.check(new World().box(4, 2, 4, Cell.STRONG), BED, SETTINGS));
	}

	@Test
	void holeInTheWallIsNotEnclosed() {
		World world = new World().box(4, 2, 4, Cell.STRONG).set(5, 1, 2, Cell.OPEN);
		assertEquals(Result.NOT_ENCLOSED, SafeRoomLogic.check(world, BED, SETTINGS));
	}

	@Test
	void openFieldIsNotEnclosed() {
		World world = new World().set(1, 1, 1, Cell.WEAK).set(2, 1, 1, Cell.WEAK);
		assertEquals(Result.NOT_ENCLOSED, SafeRoomLogic.check(world, BED, SETTINGS));
	}

	@Test
	void hugeHallCountsAsNotEnclosed() {
		// 30 x 30 x 3 is 2700 cells, above the 1024 cap.
		assertEquals(Result.NOT_ENCLOSED, SafeRoomLogic.check(new World().box(30, 3, 30, Cell.STRONG), BED, SETTINGS));
	}

	@Test
	void dirtHutIsTooWeak() {
		assertEquals(Result.WEAK_BLOCKS, SafeRoomLogic.check(new World().box(4, 2, 4, Cell.WEAK), BED, SETTINGS));
	}

	@Test
	void oneDirtBlockInAStoneWallIsTooWeak() {
		World world = new World().box(4, 2, 4, Cell.STRONG).set(5, 1, 2, Cell.WEAK);
		assertEquals(Result.WEAK_BLOCKS, SafeRoomLogic.check(world, BED, SETTINGS));
	}

	@Test
	void furnitureInsideAStrongShellIsFine() {
		World world = new World().box(4, 2, 4, Cell.STRONG).set(4, 1, 4, Cell.WEAK).set(4, 1, 3, Cell.WEAK);
		assertEquals(Result.SAFE, SafeRoomLogic.check(world, BED, SETTINGS));
	}

	@Test
	void smallRoomNeedsADoor() {
		// 2 x 2 floor: the bed and one free row.
		assertEquals(Result.NEEDS_DOOR, SafeRoomLogic.check(new World().box(2, 2, 2, Cell.STRONG), BED, SETTINGS));
	}

	@Test
	void smallRoomWithADoorIsSafe() {
		World world = new World().box(2, 2, 2, Cell.STRONG).set(3, 1, 2, Cell.DOOR).set(3, 2, 2, Cell.DOOR);
		assertEquals(Result.SAFE, SafeRoomLogic.check(world, BED, SETTINGS));
	}

	@Test
	void threeByThreeNeedsNoDoor() {
		assertEquals(Result.SAFE, SafeRoomLogic.check(new World().box(3, 2, 3, Cell.STRONG), BED, SETTINGS));
	}

	@Test
	void darkRoomNeedsLight() {
		World world = new World().box(4, 2, 4, Cell.STRONG);
		world.light = 0;
		assertEquals(Result.NEEDS_LIGHT, SafeRoomLogic.check(world, BED, SETTINGS));
	}

	@Test
	void windowIsTheWeakSpot() {
		World world = new World().box(4, 2, 4, Cell.STRONG).set(5, 1, 2, Cell.OPEN);
		SafeRoomLogic.Report report = SafeRoomLogic.inspect(world, BED, SETTINGS);
		assertEquals(Result.NOT_ENCLOSED, report.result());
		assertArrayEquals(new int[] {5, 1, 2}, report.spot());
	}

	@Test
	void doorwayIsTheWeakSpot() {
		World world = new World().box(4, 2, 4, Cell.STRONG).set(3, 1, 5, Cell.OPEN).set(3, 2, 5, Cell.OPEN);
		int[] spot = SafeRoomLogic.inspect(world, BED, SETTINGS).spot();
		assertNotNull(spot);
		assertEquals(3, spot[0]);
		assertEquals(5, spot[2]);
	}

	@Test
	void missingRoofShowsOpenSky() {
		World world = new World().box(4, 2, 4, Cell.STRONG);
		for (int x = 1; x <= 4; x++) {
			for (int z = 1; z <= 4; z++) {
				world.set(x, 3, z, Cell.OPEN);
			}
		}
		SafeRoomLogic.Report report = SafeRoomLogic.inspect(world, BED, SETTINGS);
		assertEquals(Result.NOT_ENCLOSED, report.result());
		assertNotNull(report.spot());
	}

	@Test
	void dirtBlockInStoneWallIsTheWeakSpot() {
		World world = new World().box(4, 2, 4, Cell.STRONG).set(5, 1, 2, Cell.WEAK);
		SafeRoomLogic.Report report = SafeRoomLogic.inspect(world, BED, SETTINGS);
		assertEquals(Result.WEAK_BLOCKS, report.result());
		assertArrayEquals(new int[] {5, 1, 2}, report.spot());
	}

	@Test
	void hugeHallHasNoSpot() {
		assertNull(SafeRoomLogic.inspect(new World().box(30, 3, 30, Cell.STRONG), BED, SETTINGS).spot());
	}

	@Test
	void dirtFloorIsShownBesideTheBed() {
		World world = new World().box(4, 2, 4, Cell.STRONG);
		for (int x = 1; x <= 4; x++) {
			for (int z = 1; z <= 4; z++) {
				world.set(x, 0, z, Cell.WEAK);
			}
		}
		SafeRoomLogic.Report report = SafeRoomLogic.inspect(world, BED, SETTINGS);
		assertEquals(Result.WEAK_BLOCKS, report.result());
		assertEquals(0, report.spot()[1]);
		assertEquals(1, report.seenFrom()[1]);
		boolean underBed = report.spot()[2] == 1 && (report.spot()[0] == 1 || report.spot()[0] == 2);
		assertEquals(false, underBed, "spot hidden under the bed");
	}
}
