package io.github.hellenter.eam.survival.bleeding;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BleedingLogicTest {
	@Test
	void threeSecondsTakeOneHeart() {
		BleedingSettings settings = BleedingSettings.DEFAULT;
		float damage = 0.0F;
		for (int duration = 60; duration > 0; duration--) {
			if (BleedingLogic.hurtsThisTick(duration, settings.damageInterval())) {
				damage += settings.damage();
			}
		}
		assertEquals(2.0F, damage);
	}

	@Test
	void drainsThreeTimesAsFast() {
		assertEquals(0.005F, BleedingLogic.extraDrain(0.0025F, 3.0F), 1.0E-7);
		assertEquals(0.0F, BleedingLogic.extraDrain(0.0025F, 1.0F));
		assertEquals(0.3F, BleedingLogic.activity(0.1F, true, 3.0F), 1.0E-6);
		assertEquals(0.1F, BleedingLogic.activity(0.1F, false, 3.0F));
	}
}
