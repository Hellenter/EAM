package io.github.hellenter.eam.content.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class KnifeLootLogicTest {
	@Test
	void fractionalBonusIsAChance() {
		// 1 * 1.2: one more item with a 20% chance.
		assertEquals(2, KnifeLootLogic.bonusCount(1, 1.2, 64, 0.1));
		assertEquals(1, KnifeLootLogic.bonusCount(1, 1.2, 64, 0.3));
	}

	@Test
	void wholeBonusIsCertain() {
		// 5 * 1.2 = 6 exactly.
		assertEquals(6, KnifeLootLogic.bonusCount(5, 1.2, 64, 0.99));
	}

	@Test
	void neverAboveStackSizeOrBelowTheOriginal() {
		assertEquals(64, KnifeLootLogic.bonusCount(60, 1.2, 64, 0.0));
		assertEquals(1, KnifeLootLogic.bonusCount(1, 1.2, 1, 0.0));
		assertEquals(3, KnifeLootLogic.bonusCount(3, 1.0, 64, 0.0));
		assertEquals(0, KnifeLootLogic.bonusCount(0, 1.2, 64, 0.0));
	}
}
