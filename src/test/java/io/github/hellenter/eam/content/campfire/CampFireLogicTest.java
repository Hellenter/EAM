package io.github.hellenter.eam.content.campfire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.hellenter.eam.content.campfire.CampFireLogic.FoodStep;

import org.junit.jupiter.api.Test;

class CampFireLogicTest {
	private static final CampFireSettings SETTINGS = CampFireSettings.DEFAULT;

	@Test
	void holdsUpToNineSticks() {
		assertTrue(CampFireLogic.canAddStick(3));
		assertTrue(CampFireLogic.canAddStick(8));
		assertFalse(CampFireLogic.canAddStick(9));
	}

	@Test
	void lightsWithSixSticks() {
		assertFalse(CampFireLogic.canLight(5, SETTINGS));
		assertTrue(CampFireLogic.canLight(6, SETTINGS));
		assertTrue(CampFireLogic.canLight(9, SETTINGS));
	}

	@Test
	void burnsThreeSticksAMinute() {
		assertEquals(3, 20 * 60 / SETTINGS.ticksPerStick());
		assertFalse(CampFireLogic.burnsStick(399, SETTINGS));
		assertTrue(CampFireLogic.burnsStick(400, SETTINGS));
	}

	@Test
	void foodCooksInFortyFiveSecondsThenBurns() {
		assertEquals(FoodStep.WAIT, CampFireLogic.foodStep(false, 899, SETTINGS));
		assertEquals(FoodStep.COOK, CampFireLogic.foodStep(false, 900, SETTINGS));
		assertEquals(FoodStep.WAIT, CampFireLogic.foodStep(true, 899, SETTINGS));
		assertEquals(FoodStep.BURN, CampFireLogic.foodStep(true, 900, SETTINGS));
	}

	@Test
	void oneCharcoalPerThreeSticksUpToSixteen() {
		assertEquals(0, CampFireLogic.charcoal(2, SETTINGS));
		assertEquals(5, CampFireLogic.charcoal(15, SETTINGS));
		assertEquals(5, CampFireLogic.charcoal(17, SETTINGS));
		assertEquals(16, CampFireLogic.charcoal(48, SETTINGS));
		assertEquals(16, CampFireLogic.charcoal(300, SETTINGS));
	}

	@Test
	void burningFireKeepsItsSticks() {
		assertEquals(7, CampFireLogic.sticksDropped(7, false));
		assertEquals(0, CampFireLogic.sticksDropped(7, true));
	}
}
