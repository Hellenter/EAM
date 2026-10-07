package io.github.hellenter.eam.survival.thirst;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FlaskLogicTest {
	private static final FlaskSettings SETTINGS = FlaskSettings.DEFAULT;
	private static final int NO_FILTER = -1;

	@Test
	void fillTopsUpAndRollsContamination() {
		FlaskWater clean = FlaskLogic.fill(FlaskWater.EMPTY, SETTINGS, SETTINGS.contaminationChance());
		assertEquals(FlaskLogic.CAPACITY, clean.amount());
		assertFalse(clean.contaminated());
		assertTrue(FlaskLogic.fill(FlaskWater.EMPTY, SETTINGS, 0.0F).contaminated());
	}

	@Test
	void dirtyWaterStaysDirtyWhenToppedUp() {
		FlaskWater dirty = new FlaskWater(4, true);
		assertTrue(FlaskLogic.fill(dirty, SETTINGS, 0.99F).contaminated());
	}

	@Test
	void fullFlaskGivesSixDrinksOfTwoHalves() {
		FlaskWater water = new FlaskWater(FlaskLogic.CAPACITY, false);
		int drinks = 0;
		int total = 0;
		while (!water.isEmpty()) {
			FlaskLogic.Drink drink = FlaskLogic.drink(water, NO_FILTER);
			total += drink.water();
			water = drink.remaining();
			drinks++;
		}
		assertEquals(6, drinks);
		assertEquals(FlaskLogic.CAPACITY, total);
	}

	@Test
	void dirtyWaterPoisonsWithoutFilter() {
		assertTrue(FlaskLogic.drink(new FlaskWater(6, true), NO_FILTER).poisons());
		assertFalse(FlaskLogic.drink(new FlaskWater(6, false), NO_FILTER).poisons());
	}

	@Test
	void filterCleansAndWearsOnePerHalf() {
		FlaskLogic.Drink drink = FlaskLogic.drink(new FlaskWater(6, true), FlaskLogic.FILTER_USES);
		assertFalse(drink.poisons());
		assertEquals(FlaskLogic.FILTER_USES - FlaskLogic.SIP, drink.filter());
	}

	@Test
	void spentFilterStopsCleaningButFlaskStillWorks() {
		FlaskLogic.Drink drink = FlaskLogic.drink(new FlaskWater(6, true), 0);
		assertTrue(drink.poisons());
		assertEquals(0, drink.filter());
		assertEquals(FlaskLogic.SIP, drink.water());
	}

	@Test
	void emptyFlaskForgetsContamination() {
		FlaskLogic.Drink drink = FlaskLogic.drink(new FlaskWater(2, true), NO_FILTER);
		assertTrue(drink.remaining().isEmpty());
		assertFalse(drink.remaining().contaminated());
	}
}
