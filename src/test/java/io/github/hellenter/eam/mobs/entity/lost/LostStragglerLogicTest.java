package io.github.hellenter.eam.mobs.entity.lost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LostStragglerLogicTest {
	@Test
	void fightCountsUpAndFadesFaster() {
		assertEquals(11, LostStragglerLogic.fightTicks(10, true));
		assertEquals(8, LostStragglerLogic.fightTicks(10, false));
		assertEquals(0, LostStragglerLogic.fightTicks(1, false));
	}

	@Test
	void callsOnceAfterALongFightWithoutASquad() {
		assertFalse(LostStragglerLogic.callDue(199, 200, false, false));
		assertTrue(LostStragglerLogic.callDue(200, 200, false, false));
		assertFalse(LostStragglerLogic.callDue(500, 200, true, false));
		assertFalse(LostStragglerLogic.callDue(500, 200, false, true));
	}

	@Test
	void spawnsApartAndFew() {
		assertTrue(LostStragglerLogic.spawnAllowed(0, 1, 2));
		assertFalse(LostStragglerLogic.spawnAllowed(1, 1, 2));
		assertFalse(LostStragglerLogic.spawnAllowed(0, 2, 2));
	}

	@Test
	void stragglerHasNoShieldAndPressesIn() {
		assertFalse(LostRole.STRAGGLER.shielded());
		assertFalse(LostRole.STRAGGLER.guards());
		assertFalse(LostRole.STRAGGLER.shooter());
		assertFalse(LostRole.STRAGGLER.spear());
	}
}
