package io.github.hellenter.eam.mobs.vanilla.nemesis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NemesisLogicTest {
	@Test
	void armorAndDamageOutweighDurability() {
		double ironSword = NemesisLogic.score(5.0, 0.0, 0.0, 0, 250);
		double ironChestplate = NemesisLogic.score(0.0, 6.0, 0.0, 0, 240);
		double ironPickaxe = NemesisLogic.score(3.0, 0.0, 0.0, 0, 250);
		double bow = NemesisLogic.score(0.0, 0.0, 0.0, 0, 384);
		assertTrue(ironChestplate > ironSword);
		assertTrue(ironSword > ironPickaxe);
		assertTrue(ironPickaxe > bow);
	}

	@Test
	void enchantmentsCount() {
		assertTrue(NemesisLogic.score(0.0, 0.0, 0.0, 3, 384) > NemesisLogic.score(3.0, 0.0, 0.0, 0, 250) - 3.0);
		assertTrue(NemesisLogic.score(0.0, 0.0, 0.0, 2, 384) > NemesisLogic.score(0.0, 0.0, 0.0, 0, 384));
	}

	@Test
	void healthGrowsUpToTheCap() {
		assertEquals(0.5, NemesisLogic.healthBonus(1, 0.5, 4), 1.0E-9);
		assertEquals(2.0, NemesisLogic.healthBonus(4, 0.5, 4), 1.0E-9);
		assertEquals(2.0, NemesisLogic.healthBonus(9, 0.5, 4), 1.0E-9);
	}
}
