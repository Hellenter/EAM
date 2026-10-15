package io.github.hellenter.eam.mobs.vanilla.predators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PredatorLogicTest {
	private static final PredatorSettings SETTINGS = PredatorSettings.DEFAULT;

	@Test
	void nobodyHuntsByDay() {
		assertFalse(PredatorLogic.mayHunt(false, 0, SETTINGS.hunters()));
		assertFalse(PredatorLogic.mayHunt(false, 0, SETTINGS.raiders()));
	}

	@Test
	void lightKeepsRaidersOutButNotWolves() {
		assertTrue(PredatorLogic.mayHunt(true, 7, SETTINGS.raiders()));
		assertFalse(PredatorLogic.mayHunt(true, 8, SETTINGS.raiders()));
		assertTrue(PredatorLogic.mayHunt(true, 15, SETTINGS.hunters()));
	}

	@Test
	void searchRangeNeverPassesFollowRange() {
		assertEquals(16.0, PredatorLogic.searchRange(35.0, SETTINGS.raiders()));
		assertEquals(16.0, PredatorLogic.searchRange(16.0, SETTINGS.hunters()));
	}
}
