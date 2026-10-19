package io.github.hellenter.eam.mobs.vanilla.evolution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.List;

class EvolutionLogicTest {
	private static final List<Integer> PHASES = List.of(10, 20, 40, 60);

	@Test
	void daysCountFromZero() {
		assertEquals(0, EvolutionLogic.daysPassed(23999));
		assertEquals(10, EvolutionLogic.daysPassed(240000));
	}

	@Test
	void phaseBeginsOnItsDay() {
		assertEquals(-1, EvolutionLogic.phase(PHASES, 9));
		assertEquals(0, EvolutionLogic.phase(PHASES, 10));
		assertEquals(1, EvolutionLogic.phase(PHASES, 39));
		assertEquals(3, EvolutionLogic.phase(PHASES, 500));
	}

	@Test
	void chancesSplitOneRoll() {
		List<Float> chances = List.of(0.2F, 0.125F);
		assertEquals(0, EvolutionLogic.pick(chances, 0.0F));
		assertEquals(0, EvolutionLogic.pick(chances, 0.19F));
		assertEquals(1, EvolutionLogic.pick(chances, 0.2F));
		assertEquals(1, EvolutionLogic.pick(chances, 0.32F));
		assertEquals(-1, EvolutionLogic.pick(chances, 0.33F));
		assertEquals(-1, EvolutionLogic.pick(List.of(), 0.0F));
	}

	@Test
	void skeletonDrawsCloseAndSheathesFar() {
		assertFalse(EvolutionLogic.wantsBlade(false, true, 5.0, 4.0F, 7.0F));
		assertTrue(EvolutionLogic.wantsBlade(false, true, 4.0, 4.0F, 7.0F));
		assertTrue(EvolutionLogic.wantsBlade(true, true, 6.5, 4.0F, 7.0F));
		assertFalse(EvolutionLogic.wantsBlade(true, true, 7.5, 4.0F, 7.0F));
		assertFalse(EvolutionLogic.wantsBlade(true, false, 1.0, 4.0F, 7.0F));
	}

	@Test
	void spiderShootsWebInRangeWhenReady() {
		assertTrue(EvolutionLogic.canShootWeb(6.0, true, 0, 3.0F, 12.0F));
		assertFalse(EvolutionLogic.canShootWeb(6.0, false, 0, 3.0F, 12.0F));
		assertFalse(EvolutionLogic.canShootWeb(6.0, true, 5, 3.0F, 12.0F));
		assertFalse(EvolutionLogic.canShootWeb(2.0, true, 0, 3.0F, 12.0F));
		assertFalse(EvolutionLogic.canShootWeb(13.0, true, 0, 3.0F, 12.0F));
	}
}
