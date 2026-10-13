package io.github.hellenter.eam.mobs.entity.lost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.hellenter.eam.mobs.entity.knight.SkeletonKnightLogic.Attack;

import org.junit.jupiter.api.Test;

class ShieldWallLogicTest {
	private static final LostSettings S = LostSettings.DEFAULT;
	private static final ShieldWallSettings.Crossbow C = S.shieldWall().crossbow();

	@Test
	void rowStandsAcrossTheWayToTheEnemy() {
		// Commander at the origin, enemy 10 blocks south (+z): a row along x, 3 blocks south of him.
		double[][] members = {{-3.0, 0.0}, {-1.0, 0.0}, {1.0, 0.0}, {3.0, 0.0}};
		double[][] spots = LostLogic.wallSpots(0.0, 0.0, 0.0, 10.0, members, 0.85, 3.0);
		for (double[] spot : spots) {
			assertEquals(3.0, spot[1], 1.0E-9);
		}
		// Facing +z, his right is -x: everyone keeps his own side, neighbours 0.85 apart, centred on the line.
		assertEquals(-1.275, spots[0][0], 1.0E-9);
		assertEquals(-0.425, spots[1][0], 1.0E-9);
		assertEquals(0.425, spots[2][0], 1.0E-9);
		assertEquals(1.275, spots[3][0], 1.0E-9);
	}

	@Test
	void rowHalfwayToACloseEnemy() {
		double[][] spots = LostLogic.wallSpots(0.0, 0.0, 4.0, 0.0, new double[][] {{0.0, 0.0}}, 0.85, 3.0);
		assertEquals(2.0, spots[0][0], 1.0E-9);
		assertEquals(0.0, spots[0][1], 1.0E-9);
	}

	@Test
	void eightSpearmenMakeOneLongerRow() {
		double[][] members = new double[8][];
		for (int i = 0; i < 8; i++) {
			members[i] = new double[] {i, 0.0};
		}
		double[][] spots = LostLogic.wallSpots(3.5, 0.0, 3.5, -10.0, members, 1.0, 3.0);
		// Facing -z, his right is +x: the row runs from 0 to 7 along x, three blocks towards the enemy.
		for (int i = 0; i < 8; i++) {
			assertEquals(i, spots[i][0], 1.0E-9);
			assertEquals(-3.0, spots[i][1], 1.0E-9);
		}
	}

	@Test
	void enemyOnTheCommanderStillGivesSpots() {
		double[][] spots = LostLogic.wallSpots(0.0, 0.0, 0.0, 0.0, new double[][] {{1.0, 1.0}, {-1.0, 1.0}}, 1.0, 3.0);
		assertEquals(2, spots.length);
		assertFalse(Double.isNaN(spots[0][0]));
		assertEquals(1.0, Math.abs(spots[0][0] - spots[1][0]) + Math.abs(spots[0][1] - spots[1][1]), 1.0E-9);
	}

	@Test
	void commanderKeepsCrossbowRange() {
		assertEquals(LostLogic.ArcherMove.RETREAT, LostLogic.commanderMove(C.minRange() - 1.0, true, C));
		assertEquals(LostLogic.ArcherMove.HOLD, LostLogic.commanderMove(C.minRange() + 1.0, true, C));
		assertEquals(LostLogic.ArcherMove.APPROACH, LostLogic.commanderMove(C.minRange() + 1.0, false, C));
		assertEquals(LostLogic.ArcherMove.APPROACH, LostLogic.commanderMove(C.maxRange() + 1.0, true, C));
	}

	@Test
	void wallRoles() {
		assertTrue(LostRole.WALL_SPEARMAN.shielded());
		assertTrue(LostRole.WALL_SPEARMAN.spear());
		assertFalse(LostRole.WALL_SPEARMAN.guards());
		assertFalse(LostRole.WALL_COMMANDER.shielded());
		assertTrue(LostRole.WALL_COMMANDER.shooter());
		assertEquals(Attack.POKE, LostLogic.choose(LostRole.WALL_SPEARMAN, 3.0, false, 0, 0, S.follower(), S.spearReach(), 0.0, 0.0));
		assertNull(LostLogic.choose(LostRole.WALL_COMMANDER, 1.0, false, 0, 0, S.follower(), S.spearReach(), 0.0, 0.0));
		assertEquals(4, S.shieldWall().spearmen());
	}
}
