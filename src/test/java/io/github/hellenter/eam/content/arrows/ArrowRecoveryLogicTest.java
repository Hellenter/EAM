package io.github.hellenter.eam.content.arrows;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ArrowRecoveryLogicTest {
	@Test
	void eachLevelAddsTwelvePercent() {
		assertEquals(0.5F, ArrowRecoveryLogic.chance(0.5F, 0.12F, 0), 1e-6);
		assertEquals(0.62F, ArrowRecoveryLogic.chance(0.5F, 0.12F, 1), 1e-6);
		assertEquals(0.86F, ArrowRecoveryLogic.chance(0.5F, 0.12F, 3), 1e-6);
	}

	@Test
	void chanceStaysWithinZeroAndOne() {
		assertEquals(1.0F, ArrowRecoveryLogic.chance(0.5F, 0.12F, 10), 1e-6);
		assertEquals(0.0F, ArrowRecoveryLogic.chance(0.0F, 0.12F, -2), 1e-6);
	}
}
