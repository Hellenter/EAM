package io.github.hellenter.eam.content.oven;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ClayOvenLogicTest {
	@Test
	void defaultCooksASmokerRecipeInTwentySeconds() {
		float speed = ClayOvenLogic.speedMultiplier(1.0F, ClayOvenSettings.DEFAULT.cookTimeMultiplier());
		assertEquals(400, (int) Math.ceil(200 / speed));
	}

	@Test
	void fuelLastsLonger() {
		assertEquals(2400, ClayOvenLogic.burnDuration(1600, 1.5F));
		assertEquals(150, ClayOvenLogic.burnDuration(100, 1.5F));
	}

	@Test
	void nonFuelStaysNonFuel() {
		assertEquals(0, ClayOvenLogic.burnDuration(0, 1.5F));
	}
}
