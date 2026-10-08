package io.github.hellenter.eam.content.slingshot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SlingshotLogicTest {
	@Test
	void fullDrawTakesOneSecond() {
		assertEquals(1.0F, SlingshotLogic.powerForTime(20));
		assertEquals(1.0F, SlingshotLogic.powerForTime(100));
		assertTrue(SlingshotLogic.powerForTime(19) < 1.0F);
		assertEquals(0.0F, SlingshotLogic.powerForTime(0));
	}

	@Test
	void tapIsTooWeakToShoot() {
		assertTrue(SlingshotLogic.powerForTime(2) < SlingshotLogic.MIN_POWER);
		assertTrue(SlingshotLogic.powerForTime(3) >= SlingshotLogic.MIN_POWER);
	}

	@Test
	void fullShotFallsFromFourToOneOverTwentyFourBlocks() {
		assertEquals(4.0F, SlingshotLogic.damage(1.0F, 0.0), 1e-6);
		assertEquals(2.5F, SlingshotLogic.damage(1.0F, 12.0), 1e-6);
		assertEquals(1.0F, SlingshotLogic.damage(1.0F, 24.0), 1e-6);
		assertEquals(1.0F, SlingshotLogic.damage(1.0F, 60.0), 1e-6);
	}

	@Test
	void partialDrawScalesDamage() {
		assertEquals(2.0F, SlingshotLogic.damage(0.5F, 0.0), 1e-6);
		assertEquals(0.5F, SlingshotLogic.damage(0.5F, 24.0), 1e-6);
	}
}
