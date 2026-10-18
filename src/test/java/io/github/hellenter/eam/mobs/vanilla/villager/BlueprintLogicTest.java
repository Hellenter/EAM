package io.github.hellenter.eam.mobs.vanilla.villager;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

class BlueprintLogicTest {
	@Test
	void turnsKeepTheTemplateInItsTurnedBox() {
		// A 5 wide (x), 3 deep (z) template; its corner block at x 4, z 0.
		assertArrayEquals(new int[] {4, 0}, BlueprintLogic.turn(4, 0, 5, 3, 0));
		assertArrayEquals(new int[] {2, 4}, BlueprintLogic.turn(4, 0, 5, 3, 1));
		assertArrayEquals(new int[] {0, 2}, BlueprintLogic.turn(4, 0, 5, 3, 2));
		assertArrayEquals(new int[] {0, 0}, BlueprintLogic.turn(4, 0, 5, 3, 3));
		assertArrayEquals(new int[] {3, 5}, BlueprintLogic.turnedSize(5, 3, 1));
		assertArrayEquals(new int[] {5, 3}, BlueprintLogic.turnedSize(5, 3, 2));
	}

	@Test
	void fourTurnsComeBack() {
		int[] xz = {1, 2};
		for (int i = 0; i < 4; i++) {
			int[] size = BlueprintLogic.turnedSize(5, 3, i);
			xz = BlueprintLogic.turn(xz[0], xz[1], size[0], size[1], 1);
		}
		assertArrayEquals(new int[] {1, 2}, xz);
	}

	@Test
	void standsCentredOnTheLookedAtBlock() {
		assertArrayEquals(new int[] {7, 64, -3}, BlueprintLogic.origin(10, 64, 0, 7, 7));
	}
}
