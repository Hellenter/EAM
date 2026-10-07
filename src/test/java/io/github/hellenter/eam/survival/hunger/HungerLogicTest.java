package io.github.hellenter.eam.survival.hunger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.hellenter.eam.survival.hunger.HungerLogic.FoodState;
import io.github.hellenter.eam.survival.hunger.HungerLogic.TickContext;
import io.github.hellenter.eam.survival.hunger.HungerLogic.TickResult;

import org.junit.jupiter.api.Test;

class HungerLogicTest {
	private static final HungerRules RULES = HungerRules.DEFAULT;
	private static final TickContext HEALTHY = new TickContext(20, false, true, false);
	private static final TickContext HURT = new TickContext(20, true, true, false);

	@Test
	void foodBarGrowsEveryFiveLevels() {
		assertEquals(6, HungerLogic.maxFood(0, RULES));
		assertEquals(6, HungerLogic.maxFood(4, RULES));
		assertEquals(8, HungerLogic.maxFood(5, RULES));
		assertEquals(18, HungerLogic.maxFood(30, RULES));
		assertEquals(20, HungerLogic.maxFood(35, RULES));
		assertEquals(20, HungerLogic.maxFood(Integer.MAX_VALUE, RULES));
		assertEquals(6, HungerLogic.maxFood(-3, RULES));
	}

	@Test
	void eatingStopsAtTheFoodBarSize() {
		assertTrue(HungerLogic.needsFood(5, 6));
		assertFalse(HungerLogic.needsFood(6, 6));
	}

	@Test
	void sprintNeedsMoreThanThirtyPercent() {
		assertFalse(HungerLogic.canSprint(6, 20));
		assertTrue(HungerLogic.canSprint(7, 20));
		assertFalse(HungerLogic.canSprint(1, 6));
		assertTrue(HungerLogic.canSprint(2, 6));
	}

	@Test
	void shanksCoverTheFoodBar() {
		assertEquals(3, HungerLogic.shanks(6));
		assertEquals(10, HungerLogic.shanks(20));
		assertEquals(4, HungerLogic.shanks(7));
	}

	@Test
	void foodAboveTheBarIsClamped() {
		TickResult result = HungerLogic.tick(new FoodState(20, 10.0F, 0.0F, 0), new TickContext(6, false, true, false), RULES);
		assertEquals(6, result.state().food());
		assertEquals(6.0F, result.state().saturation());
	}

	@Test
	void hungerDrainsWhileStandingStill() {
		FoodState state = new FoodState(10, 0.0F, 0.0F, 0);
		int ticks = 0;
		while (state.food() == 10) {
			state = HungerLogic.tick(state, HEALTHY, RULES).state();
			ticks++;
		}
		// 4.0 exhaustion at 0.0025 per tick is about 1600 ticks, 80 seconds per point (float rounding aside).
		assertTrue(Math.abs(ticks - 1600) <= 10, "ticks: " + ticks);
	}

	@Test
	void saturationIsSpentBeforeFood() {
		TickResult result = HungerLogic.tick(new FoodState(10, 3.0F, 4.0F, 0), HEALTHY, RULES);
		assertEquals(10, result.state().food());
		assertEquals(2.0F, result.state().saturation());
	}

	@Test
	void peacefulNeverDrainsFood() {
		TickResult result = HungerLogic.tick(new FoodState(10, 0.0F, 4.0F, 0), new TickContext(20, false, true, true), RULES);
		assertEquals(10, result.state().food());
	}

	@Test
	void regenerationIsSlowAndIgnoresSaturation() {
		FoodState state = new FoodState(20, 20.0F, 0.0F, 0);
		for (int i = 1; i < RULES.regenIntervalTicks(); i++) {
			TickResult result = HungerLogic.tick(state, HURT, RULES);
			assertEquals(0.0F, result.heal(), "healed early at tick " + i);
			state = result.state();
		}
		TickResult result = HungerLogic.tick(state, HURT, RULES);
		assertEquals(1.0F, result.heal());
		assertEquals(0, result.state().tickTimer());
	}

	@Test
	void regenerationWorksWithLittleFood() {
		TickResult result = HungerLogic.tick(new FoodState(1, 0.0F, 0.0F, RULES.regenIntervalTicks() - 1), HURT, RULES);
		assertEquals(1.0F, result.heal());
	}

	@Test
	void noRegenerationWithoutTheGameRule() {
		TickContext noRegen = new TickContext(20, true, false, false);
		TickResult result = HungerLogic.tick(new FoodState(20, 0.0F, 0.0F, RULES.regenIntervalTicks() - 1), noRegen, RULES);
		assertEquals(0.0F, result.heal());
		assertEquals(0, result.state().tickTimer());
	}

	@Test
	void starvationHurtsOnEveryDifficulty() {
		TickResult result = HungerLogic.tick(new FoodState(0, 0.0F, 0.0F, RULES.starvationIntervalTicks() - 1), HEALTHY, RULES);
		assertEquals(RULES.starvationDamage(), result.starvationDamage());
		assertEquals(0, result.state().tickTimer());
	}

	@Test
	void exhaustionIsCapped() {
		TickResult result = HungerLogic.tick(new FoodState(10, 0.0F, 40.0F, 0), HEALTHY, RULES);
		assertTrue(result.state().exhaustion() <= HungerLogic.MAX_EXHAUSTION);
	}
}
