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
	void burnsOneStickEveryThirtySeconds() {
		assertEquals(30 * 20, SETTINGS.ticksPerStick());
		assertFalse(CampFireLogic.burnsStick(599, SETTINGS));
		assertTrue(CampFireLogic.burnsStick(600, SETTINGS));
	}

	@Test
	void foodCooksInFortyFiveSecondsBurnsAfterAMinuteThenTurnsToAsh() {
		assertEquals(FoodStep.WAIT, CampFireLogic.foodStep(CampFireLogic.RAW, 899, SETTINGS));
		assertEquals(FoodStep.COOK, CampFireLogic.foodStep(CampFireLogic.RAW, 900, SETTINGS));
		assertEquals(FoodStep.WAIT, CampFireLogic.foodStep(CampFireLogic.COOKED, 1199, SETTINGS));
		assertEquals(FoodStep.BURN, CampFireLogic.foodStep(CampFireLogic.COOKED, 1200, SETTINGS));
		assertEquals(FoodStep.WAIT, CampFireLogic.foodStep(CampFireLogic.BURNT, 1199, SETTINGS));
		assertEquals(FoodStep.ASH, CampFireLogic.foodStep(CampFireLogic.BURNT, 1200, SETTINGS));
		assertEquals(FoodStep.WAIT, CampFireLogic.foodStep(CampFireLogic.DONE, 100000, SETTINGS));
	}

	@Test
	void handTakesCookedFoodFirstAndRawLast() {
		assertTrue(CampFireLogic.takePriority(CampFireLogic.COOKED) > CampFireLogic.takePriority(CampFireLogic.BURNT));
		assertTrue(CampFireLogic.takePriority(CampFireLogic.BURNT) > CampFireLogic.takePriority(CampFireLogic.DONE));
		assertTrue(CampFireLogic.takePriority(CampFireLogic.DONE) > CampFireLogic.takePriority(CampFireLogic.RAW));
	}

	@Test
	void burntFoodKeepsTwoThirds() {
		assertEquals(3, CampFireLogic.burntValue(5));
		assertEquals(3, CampFireLogic.burntValue(4));
		assertEquals(2, CampFireLogic.burntValue(3));
		assertEquals(9, CampFireLogic.burntValue(13));
		assertEquals(0, CampFireLogic.burntValue(0));
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

	@Test
	void coalsGrowWithBurntSticks() {
		assertEquals(0, CampFireLogic.coalStage(0));
		assertEquals(1, CampFireLogic.coalStage(1));
		assertEquals(1, CampFireLogic.coalStage(3));
		assertEquals(2, CampFireLogic.coalStage(4));
		assertEquals(3, CampFireLogic.coalStage(7));
		assertEquals(3, CampFireLogic.coalStage(40));
	}

	@Test
	void sticksBlackenTheLongerTheFireBurns() {
		assertEquals(CampFireBlock.FRESH, CampFireLogic.charStage(0, SETTINGS));
		assertEquals(CampFireBlock.FRESH, CampFireLogic.charStage(599, SETTINGS));
		assertEquals(CampFireBlock.SCORCHED, CampFireLogic.charStage(600, SETTINGS));
		assertEquals(CampFireBlock.CHARRED, CampFireLogic.charStage(1800, SETTINGS));
	}

	@Test
	void moreSticksCookFasterUpToAQuarter() {
		assertEquals(900, CampFireLogic.cookTicks(1, SETTINGS));
		assertEquals(720, CampFireLogic.cookTicks(9, SETTINGS));
		assertTrue(CampFireLogic.cookTicks(6, SETTINGS) < CampFireLogic.cookTicks(5, SETTINGS));
		assertEquals(FoodStep.COOK, CampFireLogic.foodStep(CampFireLogic.RAW, 720, SETTINGS, 9));
		assertEquals(FoodStep.WAIT, CampFireLogic.foodStep(CampFireLogic.RAW, 720, SETTINGS, 1));
		// Only cooking speeds up: cooked food burns on the same timer.
		assertEquals(FoodStep.WAIT, CampFireLogic.foodStep(CampFireLogic.COOKED, 1199, SETTINGS, 9));
	}
}
