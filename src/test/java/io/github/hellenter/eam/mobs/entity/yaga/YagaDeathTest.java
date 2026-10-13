package io.github.hellenter.eam.mobs.entity.yaga;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class YagaDeathTest {
	@Test
	void sheCharsBlackThenGoesAshGrey() {
		assertEquals(0xFFFFFF, YagaDeath.tint(0.0F));
		int charred = YagaDeath.tint(YagaDeath.CRUMBLE_START - 1);
		assertTrue((charred >> 16 & 0xFF) < 0x50, "charred black while she burns");
		int ash = YagaDeath.tint(YagaDeath.DEATH_TICKS);
		assertTrue((ash >> 16 & 0xFF) > 0x80, "ash grey once crumbled");
	}

	@Test
	void fitsDieDownAsSheCrumbles() {
		assertEquals(1.0F, YagaDeath.convulsion(20.0F));
		assertEquals(0.0F, YagaDeath.convulsion(YagaDeath.CRUMBLE_START + 10));
		assertEquals(0.0F, YagaDeath.crumble(YagaDeath.CRUMBLE_START));
		assertEquals(1.0F, YagaDeath.crumble(YagaDeath.DEATH_TICKS - 4));
		assertTrue(YagaDeath.burning(1.0F));
		assertFalse(YagaDeath.burning(YagaDeath.DEATH_TICKS - 1));
	}
}
