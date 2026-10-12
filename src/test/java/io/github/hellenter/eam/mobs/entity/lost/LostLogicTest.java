package io.github.hellenter.eam.mobs.entity.lost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.hellenter.eam.mobs.entity.knight.SkeletonKnightLogic.Attack;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.DoubleSupplier;

class LostLogicTest {
	private static final LostSettings S = LostSettings.DEFAULT;

	@Test
	void squadOfThreeToFive() {
		assertEquals(2, LostLogic.followerCount(2, 4, 0.0));
		assertEquals(3, LostLogic.followerCount(2, 4, 0.5));
		assertEquals(4, LostLogic.followerCount(2, 4, 0.999));
	}

	@Test
	void spawnCooldownBetweenMinAndMax() {
		assertEquals(2400, LostLogic.spawnCooldown(2400, 4800, 0.0));
		assertEquals(3600, LostLogic.spawnCooldown(2400, 4800, 0.5));
		assertTrue(LostLogic.spawnCooldown(2400, 4800, 0.999) < 4800);
		assertEquals(2400, LostLogic.spawnCooldown(4800, 2400, 0.0));
	}

	@Test
	void firstFollowerShieldsSecondShoots() {
		List<LostRole> roles = LostLogic.followerRoles(2, () -> 0.9);
		assertTrue(roles.get(0).shielded());
		assertEquals(LostRole.ARCHER, roles.get(1));
		assertEquals(List.of(LostRole.SWORDSMAN, LostRole.ARCHER, LostRole.ARCHER, LostRole.SWORDSMAN),
				LostLogic.followerRoles(4, sequence(0.1, 0.1, 0.1, 0.5)));
	}

	private static DoubleSupplier sequence(double... values) {
		int[] next = {0};
		return () -> values[next[0]++];
	}

	@Test
	void spearmanOnlyJabs() {
		assertEquals(Attack.POKE, LostLogic.choose(LostRole.SPEARMAN, 3.0, false, 0, 0, S.follower(), S.spearReach(), 0.0, 0.0));
		assertNull(LostLogic.choose(LostRole.SPEARMAN, S.spearReach() + 0.1, false, 0, 0, S.follower(), S.spearReach(), 0.0, 0.0));
		assertNull(LostLogic.choose(LostRole.ARCHER, 1.0, false, 0, 0, S.follower(), S.spearReach(), 0.0, 0.0));
	}

	@Test
	void followersNeverHeavyStrike() {
		assertEquals(Attack.COMBO, LostLogic.choose(LostRole.SWORDSMAN, 2.0, true, 0, 0, S.follower(), S.spearReach(), 0.0, 0.0));
		assertEquals(Attack.HEAVY, LostLogic.choose(LostRole.LEADER, 2.0, true, 0, 0, S.leader(), S.spearReach(), 0.0, 0.0));
	}

	@Test
	void guardStandsBetweenTargetAndArchers() {
		double[] spot = LostLogic.guardPoint(0.0, 0.0, 10.0, 0.0, 2.5, 0.0);
		assertEquals(2.5, spot[0], 1.0E-9);
		assertEquals(0.0, spot[1], 1.0E-9);
		// Sideways, at right angles to the line.
		double[] side = LostLogic.guardPoint(0.0, 0.0, 10.0, 0.0, 2.5, 1.5);
		assertEquals(2.5, side[0], 1.0E-9);
		assertEquals(1.5, side[1], 1.0E-9);
		// Never past the archers.
		assertEquals(1.0, LostLogic.guardPoint(0.0, 0.0, 1.0, 0.0, 2.5, 0.0)[0], 1.0E-9);
	}

	@Test
	void archerKeepsRange() {
		LostSettings.Archer archer = S.archer();
		assertEquals(LostLogic.ArcherMove.RETREAT, LostLogic.archerMove(3.0, true, archer));
		assertEquals(LostLogic.ArcherMove.HOLD, LostLogic.archerMove(10.0, true, archer));
		assertEquals(LostLogic.ArcherMove.APPROACH, LostLogic.archerMove(10.0, false, archer));
		assertEquals(LostLogic.ArcherMove.APPROACH, LostLogic.archerMove(20.0, true, archer));
	}

	@Test
	void archerTurnsAllTheWayToTheTarget() {
		// Facing away (after backing off) he ends up facing the player within a few ticks, not 40 degrees off.
		float yaw = 180.0F;
		for (int tick = 0; tick < 5; tick++) {
			yaw = LostLogic.turnTowards(yaw, 0.0F, 40.0F);
		}
		assertEquals(0.0F, yaw, 1.0E-4F);
		// A small error is closed at once.
		assertEquals(10.0F, LostLogic.turnTowards(35.0F, 10.0F, 40.0F), 1.0E-4F);
		// Across the -180/180 seam he takes the short way.
		assertEquals(190.0F, LostLogic.turnTowards(170.0F, -170.0F, 40.0F), 1.0E-4F);
		assertEquals(100.0F, LostLogic.turnTowards(60.0F, 120.0F, 40.0F), 1.0E-4F);
	}
}
