package io.github.hellenter.eam.mobs.vanilla.villager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VillagerAlarmLogicTest {
	@Test
	void onlyGrownNonGuardsHurtByMonstersRunToTheBell() {
		assertTrue(VillagerAlarmLogic.raisesAlarm(true, false, true));
		assertFalse(VillagerAlarmLogic.raisesAlarm(false, false, true));
		assertFalse(VillagerAlarmLogic.raisesAlarm(true, true, true));
		assertFalse(VillagerAlarmLogic.raisesAlarm(true, false, false));
	}

	@Test
	void runnerGivesUpAfterTimeout() {
		assertFalse(VillagerAlarmLogic.givesUp(500, 100, 400));
		assertTrue(VillagerAlarmLogic.givesUp(501, 100, 400));
	}

	@Test
	void walksUntilCloseEnough() {
		assertTrue(VillagerAlarmLogic.walksToBell(26.0, 5.0));
		assertFalse(VillagerAlarmLogic.walksToBell(25.0, 5.0));
	}

	@Test
	void needGrowsWithLostHealthAndMissingPotions() {
		assertEquals(0.0F, VillagerAlarmLogic.need(20.0F, 20.0F, 4, 4));
		assertEquals(1.0F, VillagerAlarmLogic.need(20.0F, 20.0F, 3, 4));
		assertEquals(0.0F, VillagerAlarmLogic.need(20.0F, 20.0F, 0, 0));
		assertEquals(7.0F, VillagerAlarmLogic.need(14.0F, 20.0F, 0, 4));
	}
}
