package io.github.hellenter.eam.mobs.entity.guard;

import io.github.hellenter.eam.mobs.entity.guard.PillagerGuardLogic.Attack;
import io.github.hellenter.eam.mobs.entity.king.KingAction;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PillagerGuardLogicTest {
	private static final PillagerGuardSettings SETTINGS = PillagerGuardSettings.DEFAULT;

	@Test
	void onslaughtAtMidRangeWhenReady() {
		assertEquals(Attack.ONSLAUGHT, PillagerGuardLogic.choose(4.0, 0, 0, true, SETTINGS, () -> 0.0));
	}

	@Test
	void noOnslaughtWhileCoolingDown() {
		assertNull(PillagerGuardLogic.choose(4.0, 10, 50, true, SETTINGS, () -> 0.0));
	}

	@Test
	void throwsAtFarTargets() {
		assertEquals(Attack.THROW, PillagerGuardLogic.choose(10.0, 0, 0, true, SETTINGS, () -> 0.0));
		assertNull(PillagerGuardLogic.choose(13.0, 0, 0, true, SETTINGS, () -> 0.0));
	}

	@Test
	void plainSwingsUpClose() {
		assertNull(PillagerGuardLogic.choose(1.5, 0, 0, true, SETTINGS, () -> 0.0));
	}

	@Test
	void nothingWithoutHisAxe() {
		assertNull(PillagerGuardLogic.choose(4.0, 0, 0, false, SETTINGS, () -> 0.0));
	}

	@Test
	void failedRollsKeepToSwings() {
		assertNull(PillagerGuardLogic.choose(4.0, 0, 0, true, SETTINGS, () -> 0.99));
	}

	@Test
	void parriedOnlyInTheAir() {
		assertFalse(PillagerGuardLogic.parries(GuardAction.ONSLAUGHT, KingAction.ONSLAUGHT_PARRY_START - 1));
		assertTrue(PillagerGuardLogic.parries(GuardAction.ONSLAUGHT, KingAction.ONSLAUGHT_PARRY_START));
		assertFalse(PillagerGuardLogic.parries(GuardAction.ONSLAUGHT, KingAction.ONSLAUGHT_PARRY_END));
		assertFalse(PillagerGuardLogic.parries(GuardAction.THROW, KingAction.ONSLAUGHT_PARRY_START));
	}

	@Test
	void aFifthOfPillagersBecomeGuards() {
		assertTrue(PillagerGuardLogic.becomesGuard(0.19, 0.2F));
		assertFalse(PillagerGuardLogic.becomesGuard(0.21, 0.2F));
	}
}
