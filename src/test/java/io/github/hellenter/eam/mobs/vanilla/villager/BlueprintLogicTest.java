package io.github.hellenter.eam.mobs.vanilla.villager;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

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

	@Test
	void oneBlockHasTwelveEdges() {
		assertEquals(12, BlueprintLogic.outline(Set.of(BlockPos.ZERO)).size());
	}

	@Test
	void aRowOfBlocksShowsOnlyItsOutline() {
		List<BlueprintLogic.Edge> edges = BlueprintLogic.outline(Set.of(new BlockPos(0, 0, 0), new BlockPos(1, 0, 0), new BlockPos(2, 0, 0)));
		assertEquals(12, edges.size());
		assertTrue(edges.contains(new BlueprintLogic.Edge(0, 0, 0, 0, 3)));
	}

	@Test
	void anInnerCornerIsOutlinedToo() {
		// An L of three blocks: the crease inside the L is an edge as well.
		List<BlueprintLogic.Edge> edges = BlueprintLogic.outline(Set.of(new BlockPos(0, 0, 0), new BlockPos(1, 0, 0), new BlockPos(0, 0, 1)));
		assertTrue(edges.contains(new BlueprintLogic.Edge(1, 1, 0, 1, 1)));
	}

	@Test
	void aheadKeepsTheNearSideFourBlocksAway() {
		// Facing south (+z), a 5 by 7 building: its centre 4 + 3.5 blocks ahead, on the ground.
		assertArrayEquals(new int[] {0, 7, 0}, BlueprintLogic.ahead(0.5, 0.0, 0.0F, 30.0F, 5, 7, 4.0, 16));
		// Facing east (-90 yaw, +x): the half width along x counts, 2.5.
		assertArrayEquals(new int[] {6, 0, 0}, BlueprintLogic.ahead(0.0, 0.5, -90.0F, 0.0F, 5, 7, 4.0, 16));
	}

	@Test
	void lookingUpLiftsTheBuilding() {
		// 45 degrees up, the centre 6 blocks ahead: 6 up.
		assertEquals(6, BlueprintLogic.ahead(0.5, 0.0, 0.0F, -45.0F, 4, 4, 4.0, 16)[2]);
		// Straight up is held to the most lift.
		assertEquals(16, BlueprintLogic.ahead(0.5, 0.0, 0.0F, -90.0F, 4, 4, 4.0, 16)[2]);
	}
}
