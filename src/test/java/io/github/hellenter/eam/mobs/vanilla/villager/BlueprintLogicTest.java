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
}
