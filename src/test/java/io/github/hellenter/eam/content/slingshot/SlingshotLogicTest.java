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

	@Test
	void ironPowerSlingshotHitsHalfAsHardAgain() {
		float scale = SlingshotLogic.IRON.damageScale();
		assertEquals(6.0F, SlingshotLogic.damage(1.0F, 0.0, scale), 1e-6);
		assertEquals(1.5F, SlingshotLogic.damage(1.0F, 24.0, scale), 1e-6);
		assertEquals(3.0F, SlingshotLogic.damage(0.5F, 0.0, scale), 1e-6);
	}

	@Test
	void betterMetalShootsFasterAndHarder() {
		SlingshotLogic.PowerTier[] tiers = {SlingshotLogic.COPPER, SlingshotLogic.IRON, SlingshotLogic.ANCIENT_METAL,
				SlingshotLogic.MITHRIL, SlingshotLogic.ADAMANTIUM};
		for (int i = 1; i < tiers.length; i++) {
			assertTrue(tiers[i].pebbleSpeed() > tiers[i - 1].pebbleSpeed());
			assertTrue(tiers[i].damageScale() > tiers[i - 1].damageScale());
			assertTrue(tiers[i].potionSpeed() > tiers[i - 1].potionSpeed());
		}
	}
}
