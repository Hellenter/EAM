package io.github.hellenter.eam.content.feeder;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FeederLogicTest {
	private static final int THIN_AFTER = 7;

	@Test
	void eatsOnceADay() {
		assertTrue(FeederLogic.canEat(null, 5));
		LivestockState fed = FeederLogic.eat(null, 5, THIN_AFTER);
		assertFalse(FeederLogic.canEat(fed, 5));
		assertTrue(FeederLogic.canEat(fed, 6));
	}

	@Test
	void wildAnimalsAreNeverThin() {
		assertFalse(FeederLogic.isThin(null, 1000, THIN_AFTER));
	}

	@Test
	void getsThinAfterSevenDaysWithoutFood() {
		LivestockState fed = FeederLogic.eat(null, 10, THIN_AFTER);
		// Can eat on day 11; days 11..17 missed is seven days, day 18 the eighth.
		assertFalse(FeederLogic.isThin(fed, 18, THIN_AFTER));
		assertTrue(FeederLogic.isThin(fed, 19, THIN_AFTER));
	}

	@Test
	void fattensUpAfterAsManyDaysOfMeals() {
		LivestockState state = FeederLogic.eat(null, 0, THIN_AFTER);
		// Ten days missed, then a meal on day 11.
		state = FeederLogic.eat(state, 11, THIN_AFTER);
		assertTrue(state.thin());
		assertEquals(9, state.hunger());
		for (long day = 12; day <= 19; day++) {
			state = FeederLogic.eat(state, day, THIN_AFTER);
			assertTrue(FeederLogic.isThin(state, day, THIN_AFTER), "day " + day);
		}
		state = FeederLogic.eat(state, 20, THIN_AFTER);
		assertFalse(state.thin());
		assertEquals(0, state.hunger());
	}

	@Test
	void shortHungerIsMadeUpWithoutGettingThin() {
		LivestockState state = FeederLogic.eat(null, 0, THIN_AFTER);
		state = FeederLogic.eat(state, 4, THIN_AFTER);
		assertFalse(state.thin());
		assertEquals(2, state.hunger());
	}

	@Test
	void clockGoingBackDoesNotCount() {
		LivestockState fed = FeederLogic.eat(null, 10, THIN_AFTER);
		assertEquals(0, FeederLogic.missedDays(fed, 3));
		assertTrue(FeederLogic.canEat(fed, 3));
	}

	@Test
	void fillSteps() {
		assertEquals(0, FeederLogic.fill(0, 64));
		assertEquals(1, FeederLogic.fill(1, 64));
		assertEquals(1, FeederLogic.fill(21, 64));
		assertEquals(2, FeederLogic.fill(22, 64));
		assertEquals(3, FeederLogic.fill(64, 64));
	}

	@Test
	void troughsStayThreeLong() {
		assertArrayEquals(new boolean[] {false, false}, FeederLogic.join(0, 0));
		assertArrayEquals(new boolean[] {true, false}, FeederLogic.join(1, 0));
		assertArrayEquals(new boolean[] {true, true}, FeederLogic.join(1, 1));
		assertArrayEquals(new boolean[] {true, false}, FeederLogic.join(2, 1));
		assertArrayEquals(new boolean[] {false, true}, FeederLogic.join(3, 2));
		assertArrayEquals(new boolean[] {false, false}, FeederLogic.join(3, 3));
	}

	@Test
	void farmerKeepsHisShare() {
		assertEquals(0, FeederLogic.farmerGives(20, 1, 20, 24, 0));
		assertEquals(6, FeederLogic.farmerGives(30, 1, 30, 24, 0));
		assertEquals(10, FeederLogic.farmerGives(10, 1, 64, 24, 0));
		// Bread covers his food, but he keeps potatoes to plant.
		assertEquals(4, FeederLogic.farmerGives(20, 1, 44, 24, 16));
		assertEquals(0, FeederLogic.farmerGives(10, 1, 64, 24, 16));
	}

	@Test
	void breedsWhileTheTroughHasRoom() {
		assertTrue(FeederLogic.roomToBreed(9, 1, 10));
		assertFalse(FeederLogic.roomToBreed(10, 1, 10));
		assertTrue(FeederLogic.roomToBreed(29, 3, 10));
	}

	@Test
	void twoGrassBitesMakeAMeal() {
		LivestockState state = FeederLogic.bite(null, 5, THIN_AFTER);
		assertTrue(FeederLogic.canEat(state, 5));
		assertEquals(1, state.bites());
		state = FeederLogic.bite(state, 5, THIN_AFTER);
		assertFalse(FeederLogic.canEat(state, 5));
		assertEquals(5, state.lastMealDay());
		assertEquals(0, state.bites());
		// Fed for today: more grass changes nothing.
		assertEquals(state, FeederLogic.bite(state, 5, THIN_AFTER));
	}

	@Test
	void yesterdaysBiteIsLost() {
		LivestockState state = FeederLogic.eat(null, 0, THIN_AFTER);
		state = FeederLogic.bite(state, 1, THIN_AFTER);
		state = FeederLogic.bite(state, 2, THIN_AFTER);
		assertTrue(FeederLogic.canEat(state, 2));
		assertEquals(1, state.bites());
	}

	@Test
	void aBiteThenTheFeederMakesAMeal() {
		LivestockState state = FeederLogic.bite(FeederLogic.eat(null, 0, THIN_AFTER), 1, THIN_AFTER);
		state = FeederLogic.eat(state, 1, THIN_AFTER);
		assertEquals(0, state.bites());
		assertFalse(FeederLogic.canEat(state, 1));
	}
}
