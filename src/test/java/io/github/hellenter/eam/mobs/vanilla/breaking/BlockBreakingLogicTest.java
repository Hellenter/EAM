package io.github.hellenter.eam.mobs.vanilla.breaking;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BlockBreakingLogicTest {
	@Test
	void dirtTakesFifteenSeconds() {
		assertEquals(300, BlockBreakingLogic.breakTicks(0.5F, true, 600));
	}

	@Test
	void blocksPlayersCannotBreakByHandAreUnbreakable() {
		assertEquals(-1, BlockBreakingLogic.breakTicks(1.5F, false, 600));
		assertEquals(-1, BlockBreakingLogic.breakTicks(-1.0F, true, 600));
	}

	@Test
	void instantBlocksTakeOneTick() {
		assertEquals(1, BlockBreakingLogic.breakTicks(0.0F, false, 600));
	}

	@Test
	void stagesGoFromZeroToNine() {
		assertEquals(0, BlockBreakingLogic.stage(0.0F, 300));
		assertEquals(5, BlockBreakingLogic.stage(150.0F, 300));
		assertEquals(9, BlockBreakingLogic.stage(299.0F, 300));
	}

	@Test
	void damageHealsOnlyAfterTheDelay() {
		assertEquals(100.0F, BlockBreakingLogic.decay(100.0F, 150, 200, 0.25F, 20));
		assertEquals(95.0F, BlockBreakingLogic.decay(100.0F, 250, 200, 0.25F, 20));
		assertEquals(0.0F, BlockBreakingLogic.decay(2.0F, 250, 200, 0.25F, 20));
	}
}
