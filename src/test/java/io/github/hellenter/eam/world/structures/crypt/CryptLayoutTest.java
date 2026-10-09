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
			assertTrue(rooms.values().stream().anyMatch(room -> room.role() == Role.TREASURY));
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
	}
}
