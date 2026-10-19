package io.github.hellenter.eam.world.bluemoon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.hellenter.eam.world.bloodmoon.BloodMoonSettings;

import org.junit.jupiter.api.Test;

import java.util.function.IntUnaryOperator;

class BlueMoonLogicTest {
	private static final BlueMoonSettings SETTINGS = BlueMoonSettings.DEFAULT;
	private static final IntUnaryOperator LOWEST = bound -> 0;
	private static final IntUnaryOperator HIGHEST = bound -> bound - 1;

	@Test
	void firstBlueMoonComesOnDays8To14() {
		assertEquals(8, BlueMoonLogic.firstDay(SETTINGS, LOWEST));
		assertEquals(14, BlueMoonLogic.firstDay(SETTINGS, HIGHEST));
	}

	@Test
	void laterBlueMoonsCome8To15DaysApart() {
		assertEquals(28, BlueMoonLogic.nextDay(20, 20, SETTINGS, LOWEST));
		assertEquals(35, BlueMoonLogic.nextDay(20, 20, SETTINGS, HIGHEST));
		// A world loaded long after the scheduled day rolls from today.
		assertEquals(108, BlueMoonLogic.nextDay(20, 100, SETTINGS, LOWEST));
	}

	@Test
	void comesAbout30PercentMoreOftenThanTheBloodMoon() {
		BloodMoonSettings blood = BloodMoonSettings.DEFAULT;
		double bloodInterval = (blood.intervalMinDays() + blood.intervalMaxDays()) / 2.0;
		double blueInterval = (SETTINGS.intervalMinDays() + SETTINGS.intervalMaxDays()) / 2.0;
		double ratio = bloodInterval / blueInterval;
		assertTrue(ratio > 1.25 && ratio < 1.35, "ratio " + ratio);
	}

	@Test
	void neverSharesANightWithTheBloodMoon() {
		assertEquals(21, BlueMoonLogic.avoid(20, 20));
		assertEquals(20, BlueMoonLogic.avoid(20, 25));
		assertEquals(0, BlueMoonLogic.avoid(0, 0));
	}

	@Test
	void harvestKeepsTheFractionWithItsOdds() {
		assertEquals(1, BlueMoonLogic.harvestCount(1, 1.5, 0.7));
		assertEquals(2, BlueMoonLogic.harvestCount(1, 1.5, 0.2));
		assertEquals(6, BlueMoonLogic.harvestCount(4, 1.5, 0.99));
		assertEquals(4, BlueMoonLogic.harvestCount(3, 1.5, 0.6));
		assertEquals(3, BlueMoonLogic.harvestCount(3, 1.0, 0.0));
	}
}
