package io.github.hellenter.eam.content.arrows;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ArrowHeadLogicTest {
	@Test
	void betterHeadsHitHarder() {
		// Vanilla arrows start at 2 base damage.
		assertEquals(2.0, ArrowHeadLogic.baseDamage(2.0, 0.0F, 0.0F, false), 1e-6);
		assertEquals(2.4, ArrowHeadLogic.baseDamage(2.0, 0.2F, 0.0F, false), 1e-6);
		assertEquals(2.8, ArrowHeadLogic.baseDamage(2.0, 0.4F, 0.0F, true), 1e-6);
		assertEquals(3.2, ArrowHeadLogic.baseDamage(2.0, 0.6F, 0.0F, false), 1e-6);
	}

	@Test
	void silverAddsHalfTheBaseAgainstUndeadOnly() {
		assertEquals(3.4, ArrowHeadLogic.baseDamage(2.0, 0.2F, 0.5F, true), 1e-6);
		assertEquals(2.4, ArrowHeadLogic.baseDamage(2.0, 0.2F, 0.5F, false), 1e-6);
	}

	@Test
	void noDamageStaysNoDamage() {
		assertEquals(0.0, ArrowHeadLogic.baseDamage(0.0, 0.6F, 0.5F, true), 1e-6);
	}
}
