package io.github.hellenter.eam.world.bloodmoon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.hellenter.eam.world.bloodmoon.BloodMoonLogic.Piece;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntUnaryOperator;

class BloodMoonLogicTest {
	private static final BloodMoonSettings SETTINGS = BloodMoonSettings.DEFAULT;
	private static final IntUnaryOperator LOWEST = bound -> 0;
	private static final IntUnaryOperator HIGHEST = bound -> bound - 1;

	@Test
	void daysStartAtOne() {
		assertEquals(1, BloodMoonLogic.dayNumber(0));
		assertEquals(1, BloodMoonLogic.dayNumber(23_999));
		assertEquals(2, BloodMoonLogic.dayNumber(24_000));
	}

	@Test
	void firstBloodMoonComesOnDays15To20() {
		assertEquals(15, BloodMoonLogic.firstDay(SETTINGS, LOWEST));
		assertEquals(20, BloodMoonLogic.firstDay(SETTINGS, HIGHEST));
	}

	@Test
	void laterBloodMoonsCome10To20DaysApart() {
		assertEquals(27, BloodMoonLogic.nextDay(17, 17, SETTINGS, LOWEST));
		assertEquals(37, BloodMoonLogic.nextDay(17, 17, SETTINGS, HIGHEST));
	}

	@Test
	void skippedDaysPushTheNextBloodMoonPastToday() {
		assertEquals(110, BloodMoonLogic.nextDay(17, 100, SETTINGS, LOWEST));
	}

	@Test
	void activeOnlyOnTheNightOfTheScheduledDay() {
		long day15 = 14L * BloodMoonLogic.DAY_LENGTH;
		assertFalse(BloodMoonLogic.active(15, 0, day15 + 11_999, SETTINGS.startTime()));
		assertTrue(BloodMoonLogic.active(15, 0, day15 + 12_000, SETTINGS.startTime()));
		assertTrue(BloodMoonLogic.active(15, 0, day15 + 23_999, SETTINGS.startTime()));
		assertFalse(BloodMoonLogic.active(15, 0, day15 + 24_000, SETTINGS.startTime()));
		assertFalse(BloodMoonLogic.active(16, 0, day15 + 13_000, SETTINGS.startTime()));
	}

	@Test
	void forcedBloodMoonLastsUntilTheDayEnds() {
		long day3 = 2L * BloodMoonLogic.DAY_LENGTH;
		assertTrue(BloodMoonLogic.active(15, 3, day3 + 1_000, SETTINGS.startTime()));
		assertFalse(BloodMoonLogic.active(15, 3, day3 + 24_000, SETTINGS.startTime()));
	}

	@Test
	void armorAlwaysStartsWithAChestplateThenAHelmet() {
		assertEquals(List.of(Piece.CHESTPLATE), BloodMoonLogic.armorPieces(1, LOWEST));
		assertEquals(List.of(Piece.CHESTPLATE, Piece.HELMET), BloodMoonLogic.armorPieces(2, LOWEST));
		assertEquals(List.of(Piece.CHESTPLATE, Piece.HELMET, Piece.LEGGINGS), BloodMoonLogic.armorPieces(3, LOWEST));
		assertEquals(List.of(Piece.CHESTPLATE, Piece.HELMET, Piece.BOOTS), BloodMoonLogic.armorPieces(3, HIGHEST));
		assertEquals(List.of(Piece.values()), BloodMoonLogic.armorPieces(4, LOWEST));
	}

	@Test
	void pickFollowsWeights() {
		Map<String, Integer> weights = new LinkedHashMap<>();
		weights.put("a", 1);
		weights.put("b", 0);
		weights.put("c", 2);
		assertEquals("a", BloodMoonLogic.pick(weights, bound -> 0));
		assertEquals("c", BloodMoonLogic.pick(weights, bound -> 1));
		assertEquals("c", BloodMoonLogic.pick(weights, bound -> 2));
		assertNull(BloodMoonLogic.pick(Map.of("a", 0), LOWEST));
	}

	@Test
	void hostileDamageGrowsByAFifth() {
		assertEquals(6.0F, BloodMoonLogic.damage(5.0F, SETTINGS.damageMultiplier()), 1e-5);
	}
}
