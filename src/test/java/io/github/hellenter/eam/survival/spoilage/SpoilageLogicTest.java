package io.github.hellenter.eam.survival.spoilage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.util.List;

class SpoilageLogicTest {
	private static final long DAY = SpoilageLogic.DAY;

	@Test
	void mergedStackKeepsEveryItemsTime() {
		// Five apples with three days left and one with one day left, as in Maxim's example.
		List<Long> five = List.of(3 * DAY, 3 * DAY, 3 * DAY, 3 * DAY, 3 * DAY);
		List<Long> stack = SpoilageLogic.merge(five, List.of(DAY));
		assertEquals(6, stack.size());
		// A day later one apple has spoiled and five are left.
		assertEquals(1, SpoilageLogic.spoiled(stack, DAY));
		assertEquals(five, SpoilageLogic.withoutOldest(stack, 1));
	}

	@Test
	void takingItemsTakesTheOldest() {
		List<Long> stack = List.of(1L, 5L, 9L);
		assertEquals(List.of(1L, 5L), SpoilageLogic.oldest(stack, 2));
		assertEquals(List.of(9L), SpoilageLogic.withoutOldest(stack, 2));
	}

	@Test
	void fitDropsOldestOrCopiesOldest() {
		assertEquals(List.of(5L, 9L), SpoilageLogic.fit(List.of(1L, 5L, 9L), 2, 100L));
		assertEquals(List.of(5L, 5L, 9L), SpoilageLogic.fit(List.of(5L, 9L), 3, 100L));
		assertEquals(List.of(100L, 100L), SpoilageLogic.fit(List.of(), 2, 100L));
	}

	@Test
	void delayPutsOffEveryTime() {
		assertEquals(List.of(11L, 15L), SpoilageLogic.delay(List.of(1L, 5L), 10L));
	}

	@Test
	void fridgeMeltsOneIcePerDay() {
		// Two days with two ice: both melt and the food is kept the whole time.
		SpoilageLogic.Cooling cooling = SpoilageLogic.cool(2 * DAY, 0, 2, DAY);
		assertEquals(2 * DAY, cooling.cooled());
		assertEquals(2, cooling.melted());
		assertEquals(0, cooling.charge());
	}

	@Test
	void fridgeWithoutIceKeepsNothing() {
		SpoilageLogic.Cooling cooling = SpoilageLogic.cool(DAY, 0, 0, DAY);
		assertEquals(0, cooling.cooled());
		assertEquals(0, cooling.melted());
	}

	@Test
	void fridgeUsesLeftoverColdFirst() {
		SpoilageLogic.Cooling cooling = SpoilageLogic.cool(DAY, DAY / 2, 1, DAY);
		assertEquals(DAY, cooling.cooled());
		assertEquals(1, cooling.melted());
		assertEquals(DAY / 2, cooling.charge());
	}

	@Test
	void fridgeRunsOutOfIce() {
		SpoilageLogic.Cooling cooling = SpoilageLogic.cool(3 * DAY, 0, 1, DAY);
		assertEquals(DAY, cooling.cooled());
		assertEquals(1, cooling.melted());
	}

	@Test
	void batchesGroupByTenthOfADay() {
		List<SpoilageLogic.Batch> batches = SpoilageLogic.batches(List.of(DAY, DAY, 3 * DAY), 0);
		assertEquals(List.of(new SpoilageLogic.Batch(2, DAY), new SpoilageLogic.Batch(1, 3 * DAY)), batches);
	}

	@Test
	void unknownItemsAreUnstampedAndStampedFresh() {
		List<Long> taken = SpoilageLogic.take(List.of(5L), 3);
		assertEquals(List.of(5L, SpoilageLogic.UNSTAMPED, SpoilageLogic.UNSTAMPED), taken);
		assertEquals(0, SpoilageLogic.spoiled(List.of(SpoilageLogic.UNSTAMPED), Long.MAX_VALUE - 1));
		assertEquals(List.of(5L, 100L, 100L), SpoilageLogic.stamp(taken, 100L));
	}
}
