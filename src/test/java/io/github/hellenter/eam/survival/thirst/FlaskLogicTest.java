package io.github.hellenter.eam.survival.thirst;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FlaskLogicTest {
	private static final FlaskSettings SETTINGS = FlaskSettings.DEFAULT;
	private static final int NO_FILTER = -1;
	private static final int CAPACITY = FlaskLogic.LEATHER.capacity();

	@Test
	void fillTopsUpAndRollsContamination() {
		FlaskWater clean = FlaskLogic.fill(FlaskWater.EMPTY, FlaskLogic.LEATHER, SETTINGS, SETTINGS.contaminationChance());
		assertEquals(FlaskLogic.LEATHER.capacity(), clean.amount());
		assertFalse(clean.contaminated());
		assertTrue(FlaskLogic.fill(FlaskWater.EMPTY, FlaskLogic.LEATHER, SETTINGS, 0.0F).contaminated());
	}

	@Test
	void dirtyWaterStaysDirtyWhenToppedUp() {
		FlaskWater dirty = new FlaskWater(4, true);
		assertTrue(FlaskLogic.fill(dirty, FlaskLogic.LEATHER, SETTINGS, 0.99F).contaminated());
	}

	@Test
	void fullFlaskGivesSixDrinksOfTwoHalves() {
		FlaskWater water = new FlaskWater(FlaskLogic.LEATHER.capacity(), false);
		int drinks = 0;
		int total = 0;
		while (!water.isEmpty()) {
			FlaskLogic.Drink drink = FlaskLogic.drink(water, FlaskLogic.LEATHER, NO_FILTER);
			total += drink.water();
			water = drink.remaining();
			drinks++;
		}
		assertEquals(6, drinks);
		assertEquals(FlaskLogic.LEATHER.capacity(), total);
	}

	@Test
	void glassFlaskHoldsAndPoursHalfAsMuchAgain() {
		assertEquals(FlaskLogic.LEATHER.capacity() * 3 / 2, FlaskLogic.GLASS.capacity());
		assertEquals(FlaskLogic.LEATHER.sip() * 3 / 2, FlaskLogic.GLASS.sip());
		FlaskWater full = FlaskLogic.fill(FlaskWater.EMPTY, FlaskLogic.GLASS, SETTINGS, 0.99F);
		assertEquals(FlaskLogic.GLASS.capacity(), full.amount());
		assertTrue(full.isFull(FlaskLogic.GLASS.capacity()));
		FlaskLogic.Drink drink = FlaskLogic.drink(full, FlaskLogic.GLASS, FlaskLogic.GLASS.filterUses());
		assertEquals(FlaskLogic.GLASS.sip(), drink.water());
		assertEquals(FlaskLogic.GLASS.filterUses() - FlaskLogic.GLASS.sip(), drink.filter());
	}

	@Test
	void dirtyWaterPoisonsWithoutFilter() {
		assertTrue(FlaskLogic.drink(new FlaskWater(6, true), FlaskLogic.LEATHER, NO_FILTER).poisons());
		assertFalse(FlaskLogic.drink(new FlaskWater(6, false), FlaskLogic.LEATHER, NO_FILTER).poisons());
	}

	@Test
	void filterCleansAndWearsOnePerHalf() {
		FlaskLogic.Drink drink = FlaskLogic.drink(new FlaskWater(6, true), FlaskLogic.LEATHER, FlaskLogic.LEATHER.filterUses());
		assertFalse(drink.poisons());
		assertEquals(FlaskLogic.LEATHER.filterUses() - FlaskLogic.LEATHER.sip(), drink.filter());
	}

	@Test
	void spentFilterStopsCleaningButFlaskStillWorks() {
		FlaskLogic.Drink drink = FlaskLogic.drink(new FlaskWater(6, true), FlaskLogic.LEATHER, 0);
		assertTrue(drink.poisons());
		assertEquals(0, drink.filter());
		assertEquals(FlaskLogic.LEATHER.sip(), drink.water());
	}

	@Test
	void emptyFlaskForgetsContamination() {
		FlaskLogic.Drink drink = FlaskLogic.drink(new FlaskWater(2, true), FlaskLogic.LEATHER, NO_FILTER);
		assertTrue(drink.remaining().isEmpty());
		assertFalse(drink.remaining().contaminated());
	}

	@Test
	void barIsLightBlueWhenFullAndDarkensAsWaterRunsOut() {
		assertEquals(13, FlaskLogic.barWidth(FlaskLogic.LEATHER.capacity(), FlaskLogic.LEATHER.capacity()));
		assertEquals(FlaskLogic.BAR_COLOR_FULL, FlaskLogic.barColor(FlaskLogic.LEATHER.capacity(), CAPACITY));
		assertEquals(FlaskLogic.BAR_COLOR_LOW, FlaskLogic.barColor(1, CAPACITY));
		for (int amount = 2; amount <= FlaskLogic.LEATHER.capacity(); amount++) {
			assertTrue(brightness(FlaskLogic.barColor(amount, CAPACITY)) > brightness(FlaskLogic.barColor(amount - 1, CAPACITY)));
		}
	}

	private static int brightness(int rgb) {
		return (rgb >> 16 & 0xFF) + (rgb >> 8 & 0xFF) + (rgb & 0xFF);
	}

	@Test
	void gulpsDuringTheDrinkNotOnlyAtTheEnd() {
		int gulps = 0;
		for (int remaining = 24; remaining > 0; remaining--) {
			if (FlaskLogic.isGulpTick(24, remaining)) {
				gulps++;
			}
		}
		assertEquals(4, gulps);
		assertFalse(FlaskLogic.isGulpTick(24, 24));
	}
}
