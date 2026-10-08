package io.github.hellenter.eam.content.brewing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SilverBrewingLogicTest {
	@Test
	void lightPotionsLastHalfAsLong() {
		assertEquals(1800, SilverBrewingLogic.lightDuration(3600));
		assertEquals(450, SilverBrewingLogic.lightDuration(900));
	}

	@Test
	void lightHealingIsHalfOfInstantHealth() {
		assertEquals(2, SilverBrewingLogic.lightHeal(0));
		assertEquals(4, SilverBrewingLogic.lightHeal(1));
		assertEquals(3, SilverBrewingLogic.lightHarm(0));
	}

	@Test
	void splashScalesWithDistance() {
		assertEquals(2, SilverBrewingLogic.scaled(2, 1.0));
		assertEquals(1, SilverBrewingLogic.scaled(2, 0.5));
		assertEquals(0, SilverBrewingLogic.scaled(2, 0.2));
	}
}
