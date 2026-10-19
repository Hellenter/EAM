package io.github.hellenter.eam.mobs.entity.leshy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.hellenter.eam.mobs.entity.leshy.LeshyLogic.Move;
import io.github.hellenter.eam.mobs.entity.leshy.LeshyLogic.Point;

import org.junit.jupiter.api.Test;

class LeshyLogicTest {
	@Test
	void appearsFromDayTwenty() {
		assertFalse(LeshyLogic.oldEnough(19, 20));
		assertTrue(LeshyLogic.oldEnough(20, 20));
	}

	@Test
	void aRegionIsAWoodOrNotForGood() {
		boolean first = LeshyLogic.leshyWood(42L, 100, 100, 512, 0.25F);
		// Anywhere inside the same region, the same answer.
		assertEquals(first, LeshyLogic.leshyWood(42L, 511, 0, 512, 0.25F));
		assertEquals(first, LeshyLogic.leshyWood(42L, 100, 100, 512, 0.25F));
	}

	@Test
	void aboutTheChanceOfRegionsAreWoods() {
		int woods = 0;
		int total = 0;
		for (int x = -100; x < 100; x++) {
			for (int z = -100; z < 100; z++) {
				total++;
				if (LeshyLogic.leshyWood(1234L, x * 512, z * 512, 512, 0.25F)) woods++;
			}
		}
		assertEquals(0.25, woods / (double) total, 0.02);
		assertFalse(LeshyLogic.leshyWood(1234L, 0, 0, 512, 0.0F));
		assertTrue(LeshyLogic.leshyWood(1234L, 0, 0, 512, 1.0F));
	}

	@Test
	void treesShieldItUnlessItBurns() {
		assertTrue(LeshyLogic.shielded(true, false));
		assertFalse(LeshyLogic.shielded(true, true));
		assertFalse(LeshyLogic.shielded(false, false));
		assertEquals(12.0F, LeshyLogic.damageTaken(6.0F, true, 2.0F), 1.0E-6);
		assertEquals(6.0F, LeshyLogic.damageTaken(6.0F, false, 2.0F), 1.0E-6);
	}

	@Test
	void falseStepsSoundAwayFromIt() {
		// Leshy to the west of the player: the steps come from the east.
		Point p = LeshyLogic.decoy(0.0, 0.0, -10.0, 0.0, 8.0, 0.0F);
		assertEquals(8.0, p.x(), 1.0E-6);
		assertEquals(0.0, p.z(), 1.0E-6);
	}

	@Test
	void behindIsOppositeTheLook() {
		// Yaw 0 looks along +z.
		Point p = LeshyLogic.behind(0.0, 0.0, 0.0F, 5.0, 0.0F);
		assertEquals(0.0, p.x(), 1.0E-6);
		assertEquals(-5.0, p.z(), 1.0E-6);
	}

	@Test
	void movesInOrder() {
		assertEquals(Move.FLY, LeshyLogic.move(30.0, false, 0, 18.0, 0, 0, 2, 0, 4.0, 20.0));
		assertEquals(Move.FLY, LeshyLogic.move(3.0, true, 0, 18.0, 0, 0, 2, 0, 4.0, 20.0));
		assertEquals(Move.WOLVES, LeshyLogic.move(3.0, false, 100, 18.0, 0, 0, 2, 0, 4.0, 20.0));
		assertEquals(Move.ROOTS, LeshyLogic.move(10.0, false, 100, 18.0, 0, 2, 2, 0, 4.0, 20.0));
		assertEquals(Move.MELEE, LeshyLogic.move(2.0, false, 100, 18.0, 0, 2, 2, 0, 4.0, 20.0));
		assertEquals(Move.MELEE, LeshyLogic.move(10.0, false, 100, 18.0, 0, 2, 2, 50, 4.0, 20.0));
		assertEquals(Move.MELEE, LeshyLogic.move(30.0, false, 100, 18.0, 0, 0, 0, 50, 4.0, 20.0));
	}

	@Test
	void rootLineReachesPastTheTarget() {
		assertEquals(10, LeshyLogic.rootCount(10.0, 1.25, 16));
		assertEquals(16, LeshyLogic.rootCount(40.0, 1.25, 16));
		assertEquals(3, LeshyLogic.rootCount(0.5, 1.25, 16));
	}

	@Test
	void mapHeadingStaysPutForHalfASecond() {
		float yaw = LeshyLogic.spinningYaw(100L, 7);
		assertEquals(yaw, LeshyLogic.spinningYaw(109L, 7), 1.0E-6);
		assertTrue(yaw >= 0.0F && yaw < 360.0F);
	}

	@Test
	void peaceLasts() {
		assertTrue(LeshyLogic.atPeace(100L, 200L));
		assertFalse(LeshyLogic.atPeace(200L, 200L));
	}
}
