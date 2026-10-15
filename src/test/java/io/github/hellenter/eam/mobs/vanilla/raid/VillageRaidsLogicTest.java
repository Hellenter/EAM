package io.github.hellenter.eam.mobs.vanilla.raid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.IntUnaryOperator;

class VillageRaidsLogicTest {
	private static final IntUnaryOperator LOWEST = bound -> 0;
	private static final IntUnaryOperator HIGHEST = bound -> bound - 1;

	@Test
	void randomRaidsComeEvery15To25Days() {
		VillageRaidsSettings settings = VillageRaidsSettings.DEFAULT;
		assertEquals(15, VillageRaidsLogic.nextDay(0, settings.intervalMinDays(), settings.intervalMaxDays(), LOWEST));
		assertEquals(25, VillageRaidsLogic.nextDay(0, settings.intervalMinDays(), settings.intervalMaxDays(), HIGHEST));
		assertEquals(55, VillageRaidsLogic.nextDay(40, settings.intervalMinDays(), settings.intervalMaxDays(), LOWEST));
		// The average gap is 20 days.
		assertEquals(20.0, (settings.intervalMinDays() + settings.intervalMaxDays()) / 2.0);
	}

	@Test
	void raidIsDueFromItsDayOn() {
		assertFalse(VillageRaidsLogic.due(14, 15));
		assertTrue(VillageRaidsLogic.due(15, 15));
		assertTrue(VillageRaidsLogic.due(17, 15));
		assertFalse(VillageRaidsLogic.due(17, 0));
	}

	@Test
	void laterWavesKeepTheLastCount() {
		List<Integer> counts = List.of(0, 1, 2);
		assertEquals(0, VillageRaidsLogic.perWave(counts, 1));
		assertEquals(2, VillageRaidsLogic.perWave(counts, 3));
		assertEquals(2, VillageRaidsLogic.perWave(counts, 7));
		assertEquals(0, VillageRaidsLogic.perWave(counts, 0));
		assertEquals(0, VillageRaidsLogic.perWave(List.of(), 2));
	}

	@Test
	void raidersGlowAfterTwoMinutes() {
		assertFalse(VillageRaidsLogic.glows(1000, 1000 + 2399, 2400));
		assertTrue(VillageRaidsLogic.glows(1000, 1000 + 2400, 2400));
	}
}
