package io.github.hellenter.eam.content.feeder;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
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
	void grassFedAnimalComesForOneFeederMealADay() {
		long now = 5 * FeederLogic.DAY + 1000;
		LivestockState grazed = FeederLogic.bite(FeederLogic.bite(null, 5, THIN_AFTER), 5, THIN_AFTER);
		assertFalse(FeederLogic.canEat(grazed, 5));
		assertTrue(FeederLogic.wantsFeeder(grazed, 5, now));
		LivestockState kept = FeederLogic.feederMeal(grazed, 5, now, THIN_AFTER);
		// The extra meal is not a second day's meal: hunger and the meal day stay.
		assertEquals(grazed.lastMealDay(), kept.lastMealDay());
		assertEquals(grazed.hunger(), kept.hunger());
		assertFalse(FeederLogic.wantsFeeder(kept, 5, now + 10));
		assertTrue(FeederLogic.wantsFeeder(kept, 5, now + FeederLogic.DAY));
	}

	@Test
	void growsFasterForADayAfterAFeederMeal() {
		long now = 3 * FeederLogic.DAY;
		assertEquals(1.0, FeederLogic.growthSpeed(null, now, 1.5));
		LivestockState kept = FeederLogic.feederMeal(null, 3, now, THIN_AFTER);
		assertEquals(1.5, FeederLogic.growthSpeed(kept, now + FeederLogic.DAY - 1, 1.5));
		assertEquals(1.0, FeederLogic.growthSpeed(kept, now + FeederLogic.DAY, 1.5));
		// Eating and grazing keep the feeder time.
		assertEquals(now, FeederLogic.eat(kept, 4, THIN_AFTER).feederTime());
		assertEquals(now, FeederLogic.bite(kept, 4, THIN_AFTER).feederTime());
	}

	@Test
	void feederPairsAnimalsHalfAsOftenAgain() {
		assertTrue(FeederLogic.canBreedAgain(LivestockState.NEVER, 0, 1.5));
		assertFalse(FeederLogic.canBreedAgain(1000, 1000 + 15999, 1.5));
		assertTrue(FeederLogic.canBreedAgain(1000, 1000 + 16000, 1.5));
	}

	@Test
	void fallsSickAfterTwoBadDaysAndRecoversAfterTwoGood() {
		LivestockState state = FeederLogic.feederMeal(null, 1, FeederLogic.DAY, THIN_AFTER);
		state = FeederLogic.checkNeeds(state, 1, true, 2);
		assertFalse(state.sick());
		// Checked once a day.
		assertSame(state, FeederLogic.checkNeeds(state, 1, true, 2));
		state = FeederLogic.checkNeeds(state, 2, true, 2);
		assertTrue(state.sick());
		assertEquals(1.0, FeederLogic.growthSpeed(state, 2 * FeederLogic.DAY + 10, 1.5));
		state = FeederLogic.checkNeeds(state, 3, false, 2);
		assertTrue(state.sick());
		state = FeederLogic.checkNeeds(state, 4, false, 2);
		assertFalse(state.sick());
		// Feeding keeps the sickness.
		LivestockState sick = FeederLogic.checkNeeds(FeederLogic.checkNeeds(state, 5, true, 2), 6, true, 2);
		assertTrue(FeederLogic.eat(sick, 7, THIN_AFTER).sick());
	}

	@Test
	void crowdedPastTheLimit() {
		assertFalse(FeederLogic.crowded(6, 6));
		assertTrue(FeederLogic.crowded(7, 6));
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
		// Default reserve: one stack of carrots to plant and 12 points to breed.
		assertEquals(4, FeederLogic.farmerGives(16, 1, 16, 12, 8));
		assertEquals(8, FeederLogic.farmerGives(16, 1, 20, 12, 8));
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
