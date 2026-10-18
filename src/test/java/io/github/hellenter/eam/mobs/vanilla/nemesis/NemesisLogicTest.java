package io.github.hellenter.eam.mobs.vanilla.nemesis;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class NemesisLogicTest {
	@Test
	void healthGrowsUpToTheCap() {
		assertEquals(0.5, NemesisLogic.healthBonus(1, 0.5, 4), 1.0E-9);
		assertEquals(2.0, NemesisLogic.healthBonus(4, 0.5, 4), 1.0E-9);
		assertEquals(2.0, NemesisLogic.healthBonus(9, 0.5, 4), 1.0E-9);
	}
}
