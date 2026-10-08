package io.github.hellenter.eam.world.mining;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BreakTimeLogicTest {
	@Test
	void breakingTakesFourTimesLonger() {
		assertEquals(0.01F, BreakTimeLogic.destroyProgress(0.04F), 1e-6F);
	}

	@Test
	void instantBlocksTakeHalfASecond() {
		assertEquals(0.1F, BreakTimeLogic.destroyProgress(Float.POSITIVE_INFINITY));
		assertEquals(0.1F, BreakTimeLogic.destroyProgress(1.0F));
	}

	@Test
	void unbreakableStaysUnbreakable() {
		assertEquals(0.0F, BreakTimeLogic.destroyProgress(0.0F));
	}
}
