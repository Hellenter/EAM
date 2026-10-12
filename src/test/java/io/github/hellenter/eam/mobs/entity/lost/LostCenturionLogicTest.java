package io.github.hellenter.eam.mobs.entity.lost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.hellenter.eam.mobs.entity.knight.SkeletonKnightLogic.Attack;
import io.github.hellenter.eam.mobs.entity.lost.LostCenturionLogic.Call;

import org.junit.jupiter.api.Test;

class LostCenturionLogicTest {
	private static final LostCenturionSettings.Horn HORN = LostCenturionSettings.Horn.DEFAULT;

	@Test
	void firstCallAlwaysBringsCavalry() {
		assertEquals(Call.CAVALRY, LostCenturionLogic.call(0, 0.0, HORN));
		assertEquals(Call.CAVALRY, LostCenturionLogic.call(0, 0.999, HORN));
	}

	@Test
	void secondCallIsHalfAndHalf() {
		assertEquals(Call.CAVALRY, LostCenturionLogic.call(1, 0.49, HORN));
		assertEquals(Call.SQUAD, LostCenturionLogic.call(1, 0.5, HORN));
	}

	@Test
	void laterCallsMostlyBringASquad() {
		assertEquals(Call.CAVALRY, LostCenturionLogic.call(2, 0.29, HORN));
		assertEquals(Call.SQUAD, LostCenturionLogic.call(2, 0.3, HORN));
		assertEquals(Call.SQUAD, LostCenturionLogic.call(7, 0.6, HORN));
	}

	@Test
	void thirtySecondsThenForty() {
		assertEquals(600, LostCenturionLogic.cooldownAfter(1, HORN));
		assertEquals(800, LostCenturionLogic.cooldownAfter(2, HORN));
		assertEquals(800, LostCenturionLogic.cooldownAfter(5, HORN));
	}

	@Test
	void centurionJabsWithHisLongSpear() {
		LostCenturionSettings c = LostSettings.DEFAULT.centurion();
		assertEquals(Attack.POKE, LostLogic.choose(LostRole.CENTURION, 3.8, false, 0, 0, c.fight(), c.spearReach(), 0.0, 0.0));
		assertEquals(null, LostLogic.choose(LostRole.CENTURION, 4.5, false, 0, 0, c.fight(), c.spearReach(), 0.0, 0.0));
	}

	@Test
	void centurionDoesNotGuardArchers() {
		assertFalse(LostRole.CENTURION.guards());
		assertTrue(LostRole.CENTURION.shielded());
		assertTrue(LostRole.SPEARMAN.guards());
		assertFalse(LostRole.LEADER.guards());
	}
}
