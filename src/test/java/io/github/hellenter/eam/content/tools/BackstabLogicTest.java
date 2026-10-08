package io.github.hellenter.eam.content.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BackstabLogicTest {
	@Test
	void attackerStraightBehindIsBehind() {
		// Yaw 0 faces +Z, so behind is -Z.
		assertTrue(BackstabLogic.isBehind(0.0F, 0.0, -2.0));
		// Yaw 90 faces -X, so behind is +X.
		assertTrue(BackstabLogic.isBehind(90.0F, 2.0, 0.0));
	}

	@Test
	void attackerInFrontOrAtTheSideIsNotBehind() {
		assertFalse(BackstabLogic.isBehind(0.0F, 0.0, 2.0));
		assertFalse(BackstabLogic.isBehind(0.0F, 2.0, 0.0));
	}

	@Test
	void coneIsNinetyDegrees() {
		assertTrue(BackstabLogic.isBehind(0.0F, 1.0, -1.1));
		assertFalse(BackstabLogic.isBehind(0.0F, 1.0, -0.9));
	}

	@Test
	void sneakingBackstabAddsHalf() {
		assertEquals(1.5F, BackstabLogic.bonusDamage(3.0F, true, true));
		assertEquals(0.0F, BackstabLogic.bonusDamage(3.0F, false, true));
		assertEquals(0.0F, BackstabLogic.bonusDamage(3.0F, true, false));
	}
}
