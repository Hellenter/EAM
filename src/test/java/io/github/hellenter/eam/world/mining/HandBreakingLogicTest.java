package io.github.hellenter.eam.world.mining;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HandBreakingLogicTest {
	@Test
	void stoneByHandGetsNoProgress() {
		assertEquals(0.0F, HandBreakingLogic.destroyProgress(0.0067F, false, false, 1.5F));
	}

	@Test
	void toolKeepsVanillaProgress() {
		assertEquals(0.05F, HandBreakingLogic.destroyProgress(0.05F, true, false, 1.5F));
	}

	@Test
	void handBreakableBlockKeepsVanillaProgress() {
		assertEquals(0.033F, HandBreakingLogic.destroyProgress(0.033F, false, true, 0.5F));
	}

	@Test
	void instantBlocksStayBreakable() {
		assertEquals(1.0F, HandBreakingLogic.destroyProgress(1.0F, false, false, 0.0F));
	}
}
