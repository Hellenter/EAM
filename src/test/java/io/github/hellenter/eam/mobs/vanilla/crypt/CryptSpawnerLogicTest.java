package io.github.hellenter.eam.mobs.vanilla.crypt;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CryptSpawnerLogicTest {
	@Test
	void dropsToTheFloorBelow() {
		assertEquals(64, CryptSpawnerLogic.standingY(64, 67, 3, y -> y == 64));
	}

	@Test
	void climbsWhenNoFloorBelow() {
		assertEquals(66, CryptSpawnerLogic.standingY(64, 63, 3, y -> y == 66));
	}

	@Test
	void prefersTheNearestFloorBelowTheStart() {
		assertEquals(65, CryptSpawnerLogic.standingY(64, 66, 3, y -> y == 61 || y == 65));
	}

	@Test
	void staysWithinTheRange() {
		assertEquals(67, CryptSpawnerLogic.standingY(64, 67, 3, y -> y == 60 || y == 68));
	}
}
