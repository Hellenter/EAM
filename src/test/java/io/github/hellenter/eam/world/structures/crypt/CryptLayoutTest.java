package io.github.hellenter.eam.world.structures.crypt;

import static io.github.hellenter.eam.world.structures.crypt.CryptLayout.EAST;
import static io.github.hellenter.eam.world.structures.crypt.CryptLayout.NORTH;
import static io.github.hellenter.eam.world.structures.crypt.CryptLayout.SOUTH;
import static io.github.hellenter.eam.world.structures.crypt.CryptLayout.WEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.hellenter.eam.world.structures.crypt.CryptLayout.Cell;
import io.github.hellenter.eam.world.structures.crypt.CryptLayout.Fit;
import io.github.hellenter.eam.world.structures.crypt.CryptLayout.Piece;
import io.github.hellenter.eam.world.structures.crypt.CryptLayout.Role;
import io.github.hellenter.eam.world.structures.crypt.CryptLayout.Room;

import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

class CryptLayoutTest {
	@Test
	void rotatesClockwise() {
		assertEquals(EAST, CryptLayout.rotate(NORTH, 1));
		assertEquals(NORTH, CryptLayout.rotate(WEST, 1));
		assertEquals(SOUTH | WEST, CryptLayout.rotate(NORTH | EAST, 2));
		assertEquals(NORTH | WEST, CryptLayout.rotate(NORTH | EAST, 3));
		assertEquals(SOUTH, CryptLayout.opposite(NORTH));
	}

	@Test
	void turnsBigRoomDoorsClockwise() {
		assertEquals(0b100, CryptLayout.rotateBig(0b1, 1), "north-west door turns to east-north");
		assertEquals(0b1, CryptLayout.rotateBig(0b1000000, 1), "west-south door turns to north-west");
		assertEquals(0b11, CryptLayout.rotateBig(0b110000, 2));
	}

	@Test
	void bigRoomDoorsComeFromItsCells() {
		for (long seed = 0; seed < 50; seed++) {
			List<CryptLayout> crypt = CryptLayout.generate(new Random(seed)::nextInt, CryptLayout.DEFAULT_BOUNDS, 3, 3, () -> 9, () -> 3);
			if (crypt == null) continue;
			for (CryptLayout layout : crypt) {
				for (CryptLayout.Big big : layout.bigs()) {
					int doors = big.doors(layout.rooms());
					int cellDoors = big.cells().stream().mapToInt(cell -> Integer.bitCount(layout.rooms().get(cell).doors())).sum();
					assertEquals(cellDoors, Integer.bitCount(doors), "every outside door of a cell is a door of the big room");
					assertTrue(Integer.bitCount(doors) >= 2, "the path goes in and out");
				}
			}
		}
		Map<Cell, Room> none = Map.of();
		assertEquals(0, new CryptLayout.Big(new Cell(0, 0)).doors(none));
	}

	@Test
	void cryptHasThreeLevelsWithFightsBigRoomsAndBoss() {
		int made = 0;
		for (long seed = 0; seed < 300; seed++) {
			Random random = new Random(seed);
			CryptLayout.Bounds bounds = seed % 2 == 0 ? CryptLayout.DEFAULT_BOUNDS : new CryptLayout.Bounds(-6, 6, -8, 0);
			List<CryptLayout> crypt = CryptLayout.generate(random::nextInt, bounds, 3, 2 + (int) (seed % 3 == 0 ? 1 : 0),
					() -> 7 + random.nextInt(4), () -> 1 + random.nextInt(3));
			if (crypt == null) continue;
			made++;
			assertEquals(3, crypt.size());
			assertEquals(SOUTH, crypt.getFirst().rooms().get(new Cell(0, 0)).doors() & SOUTH, "the first room opens to the entrance");
			assertNull(crypt.getFirst().up());
			int bigs = 0;
			for (int level = 0; level < 3; level++) {
				CryptLayout layout = crypt.get(level);
				boolean last = level == 2;
				bigs += layout.bigs().size();
				assertTrue(layout.bigs().size() <= 1, "big rooms spread over the levels");
				if (level > 0) {
					assertEquals(crypt.get(level - 1).down(), layout.up(), "a level starts at the foot of the stairs");
					assertEquals(Role.STAIRS, layout.rooms().get(layout.up()).role());
				}
				if (last) {
					assertNull(layout.down());
					assertNotNull(layout.boss(), "the last level ends in the boss room");
				} else {
					assertNull(layout.boss());
					assertEquals(Role.STAIRS, layout.rooms().get(layout.down()).role());
				}
				checkDoors(layout, bounds, level == 0);
				long fights = layout.rooms().values().stream().filter(room -> room.role() == Role.FIGHT).count();
				assertTrue(fights >= CryptLayout.MIN_FIGHTS, "seed " + seed + " level " + level + ": " + fights + " fights");
				for (Room room : layout.rooms().values()) {
					if (room.role() == Role.CORRIDOR) assertTrue(Integer.bitCount(room.doors()) <= 2, "no corridor crossroads");
				}
				CryptLayout.Boss boss = layout.boss();
				if (boss == null) continue;
				for (int dx = -1; dx <= 1; dx++) {
					for (int dz = -1; dz <= 1; dz++) {
						Cell cell = new Cell(boss.middle().x() + dx, boss.middle().z() + dz);
						assertTrue(bounds.contains(cell), "boss room in reach");
						assertNull(layout.rooms().get(cell), "no room inside the boss room");
						assertNull(crypt.get(level - 1).rooms().get(cell), "the boss room is taller than a level");
					}
				}
			}
			assertTrue(bigs >= 2, "at least two big rooms");
		}
		assertTrue(made >= 295, "the crypt nearly always fits: " + made);
	}

	private static void checkDoors(CryptLayout layout, CryptLayout.Bounds bounds, boolean first) {
		Map<Cell, Room> rooms = layout.rooms();
		CryptLayout.Boss boss = layout.boss();
		for (Map.Entry<Cell, Room> entry : rooms.entrySet()) {
			Cell cell = entry.getKey();
			assertTrue(bounds.contains(cell), "stays in reach");
			if (entry.getValue().role() == Role.TREASURY) assertTrue(entry.getValue().doors() != 0);
			CryptLayout.Big own = layout.bigs().stream().filter(big -> big.covers(cell)).findFirst().orElse(null);
			assertEquals(own != null, entry.getValue().role() == Role.BIG, "big rooms and their cells agree");
			for (int direction : new int[] {NORTH, EAST, SOUTH, WEST}) {
				if ((entry.getValue().doors() & direction) == 0) continue;
				if (first && cell.equals(new Cell(0, 0)) && direction == SOUTH) continue;
				Cell next = cell.step(direction);
				assertTrue(own == null || !own.covers(next), "big room cells have no doors between them");
				if (boss != null && boss.covers(next)) {
					assertEquals(boss.middle(), next.step(direction), "the door is in the middle of the boss room's side");
					assertEquals(CryptLayout.opposite(direction), boss.door());
					continue;
				}
				Room other = rooms.get(next);
				assertNotNull(other, "door leads somewhere");
				assertTrue((other.doors() & CryptLayout.opposite(direction)) != 0, "door is matched");
			}
		}
	}

	@Test
	void fitsExactDoorsFirstThenMore() {
		List<Piece<String>> pieces = List.of(
				new Piece<>("corner", Role.CORRIDOR, NORTH | WEST),
				new Piece<>("cross", Role.CORRIDOR, NORTH | EAST | SOUTH | WEST),
				new Piece<>("fight", Role.FIGHT, NORTH | SOUTH));

		Fit<String> corner = CryptLayout.fit(pieces, Role.CORRIDOR, SOUTH | EAST, bound -> 0);
		assertEquals("corner", corner.id());
		assertEquals(SOUTH | EAST, CryptLayout.rotate(NORTH | WEST, corner.quarters()));
		assertEquals(0, corner.extraDoors());

		Fit<String> straight = CryptLayout.fit(pieces, Role.CORRIDOR, EAST | WEST, bound -> 0);
		assertEquals("cross", straight.id());
		assertEquals(NORTH | SOUTH, straight.extraDoors());

		assertEquals("fight", CryptLayout.fit(pieces, Role.FIGHT, EAST | WEST, bound -> 0).id());
		assertEquals("corner", CryptLayout.fit(pieces, Role.FIGHT, NORTH | EAST, bound -> 0).id(), "falls back to corridors");
		assertNull(CryptLayout.fit(List.of(pieces.get(2)), Role.TREASURY, NORTH | EAST, bound -> 0));

		List<Piece<String>> bigs = List.of(new Piece<>("big", Role.BIG, 0b11), pieces.get(1));
		Fit<String> big = CryptLayout.fit(bigs, Role.BIG, 0b1100, bound -> 0);
		assertEquals("big", big.id());
		assertEquals(1, big.quarters(), "the north doors turn to the east side");
		assertNull(CryptLayout.fit(bigs, Role.BIG, 0b101, bound -> 0), "big rooms never fall back to corridors");
	}
}
