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
	void doorsMatchBetweenNeighbours() {
		for (long seed = 0; seed < 200; seed++) {
			Random random = new Random(seed);
			Map<Cell, Room> rooms = CryptLayout.generate(random::nextInt, 9, 3).rooms();
			assertTrue(rooms.values().stream().noneMatch(room -> room.role() == Role.TREASURY && room.doors() == 0));
			assertEquals(SOUTH, rooms.get(new Cell(0, 0)).doors() & SOUTH, "the first room opens to the entrance");
			for (Map.Entry<Cell, Room> entry : rooms.entrySet()) {
				Cell cell = entry.getKey();
				assertTrue(cell.z() <= 0, "nothing south of the entrance row");
				assertTrue(cell.z() >= -CryptLayout.MAX_NORTH && Math.abs(cell.x()) <= CryptLayout.MAX_SIDEWAYS, "stays in reach");
				for (int direction : new int[] {NORTH, EAST, SOUTH, WEST}) {
					if ((entry.getValue().doors() & direction) == 0) continue;
					if (cell.equals(new Cell(0, 0)) && direction == SOUTH) continue;
					Room other = rooms.get(cell.step(direction));
					assertNotNull(other, "door leads somewhere");
					assertTrue((other.doors() & CryptLayout.opposite(direction)) != 0, "door is matched");
				}
				if (entry.getValue().role() == Role.FIGHT) assertTrue(CryptLayout.isStraight(entry.getValue().doors()));
			}
			assertEquals(Role.STAIRS, rooms.get(CryptLayout.generate(new Random(seed)::nextInt, 9, 3).stairs()).role());
		}
	}

	@Test
	void lowerLevelEndsInBossRoomClearOfTheLevelAbove() {
		int withBoss = 0;
		for (long seed = 0; seed < 200; seed++) {
			Random random = new Random(seed);
			CryptLayout first = CryptLayout.generate(random::nextInt, 9, 3);
			CryptLayout second = CryptLayout.generateBelow(first, random::nextInt, 9, 3);
			Map<Cell, Room> rooms = second.rooms();
			assertEquals(Role.STAIRS, rooms.get(first.stairs()).role(), "the level starts at the foot of the stairs");
			CryptLayout.Boss boss = second.boss();
			for (Map.Entry<Cell, Room> entry : rooms.entrySet()) {
				Cell cell = entry.getKey();
				assertTrue(cell.z() <= 0 && cell.z() >= -CryptLayout.MAX_NORTH && Math.abs(cell.x()) <= CryptLayout.MAX_SIDEWAYS);
				for (int direction : new int[] {NORTH, EAST, SOUTH, WEST}) {
					if ((entry.getValue().doors() & direction) == 0) continue;
					Cell next = cell.step(direction);
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
			if (boss == null) continue;
			withBoss++;
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					Cell cell = new Cell(boss.middle().x() + dx, boss.middle().z() + dz);
					assertTrue(cell.z() <= 0 && cell.z() >= -CryptLayout.MAX_NORTH && Math.abs(cell.x()) <= CryptLayout.MAX_SIDEWAYS, "boss room in reach");
					assertNull(rooms.get(cell), "no room inside the boss room");
					assertNull(first.rooms().get(cell), "the boss room is taller than a level");
				}
			}
		}
		assertTrue(withBoss >= 190, "the boss room nearly always fits: " + withBoss);
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
	}
}
