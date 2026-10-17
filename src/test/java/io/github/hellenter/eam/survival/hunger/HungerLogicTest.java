package io.github.hellenter.eam.survival.hunger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.hellenter.eam.survival.hunger.HungerLogic.Deficiencies;
import io.github.hellenter.eam.survival.hunger.HungerLogic.FoodState;
import io.github.hellenter.eam.survival.hunger.HungerLogic.Stamina;
import io.github.hellenter.eam.survival.hunger.HungerLogic.TickContext;
import io.github.hellenter.eam.survival.hunger.HungerLogic.TickResult;

import org.junit.jupiter.api.Test;

class HungerLogicTest {
	private static final HungerRules RULES = HungerRules.DEFAULT;
	private static final TickContext HEALTHY = new TickContext(20, false, true, false, false, false, Deficiencies.NONE);
	private static final TickContext HURT = new TickContext(20, true, true, false, false, false, Deficiencies.NONE);

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
		TickResult result = HungerLogic.tick(new FoodState(20, 10.0F, 0.0F, 0), new TickContext(6, false, true, false, false, false, Deficiencies.NONE), RULES);
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
		TickResult result = HungerLogic.tick(new FoodState(10, 0.0F, 4.0F, 0), new TickContext(20, false, true, true, false, false, Deficiencies.NONE), RULES);
		assertEquals(10, result.state().food());
		assertFalse(result.pointSpent(), "nutrients drained in peaceful");
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
		TickContext noRegen = new TickContext(20, true, false, false, false, false, Deficiencies.NONE);
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

	@Test
	void balancedDietHalvesHunger() {
		TickContext balanced = new TickContext(20, false, true, false, true, false, Deficiencies.NONE);
		TickResult result = HungerLogic.tick(new FoodState(10, 0.0F, 7.0F, 0), balanced, RULES);
		assertEquals(10, result.state().food());
		assertFalse(result.pointSpent());

		result = HungerLogic.tick(new FoodState(10, 0.0F, 8.0F, 0), balanced, RULES);
		assertEquals(9, result.state().food());
		assertTrue(result.pointSpent());
	}

	@Test
	void balancedDietSpeedsUpRegeneration() {
		TickContext balancedHurt = new TickContext(20, true, true, false, true, false, Deficiencies.NONE);
		int interval = Math.round(RULES.regenIntervalTicks() / RULES.balancedRegenFactor());
		assertEquals(400, interval);
		TickResult result = HungerLogic.tick(new FoodState(10, 0.0F, 0.0F, interval - 1), balancedHurt, RULES);
		assertEquals(1.0F, result.heal());
	}

	@Test
	void nutrientsAreCappedAndDecay() {
		Nutrition full = HungerLogic.eat(Nutrition.of(90), 25, 5, 0);
		assertEquals(new Nutrition(100, 95, 90), full);
		assertEquals(new Nutrition(0, 0, 0), HungerLogic.decay(Nutrition.of(0.25F), 0.5F));
		assertEquals(Nutrition.of(49.5F), HungerLogic.decay(Nutrition.of(50), RULES.nutrientDecay()));
	}

	@Test
	void balanceNeedsAllThreeNutrients() {
		assertTrue(HungerLogic.balanced(Nutrition.of(70), 70));
		assertFalse(HungerLogic.balanced(new Nutrition(100, 100, 69), 70));
	}

	@Test
	void gracePeriodStopsHunger() {
		TickContext grace = new TickContext(20, false, true, false, false, true, Deficiencies.NONE);
		FoodState state = new FoodState(10, 0.0F, 3.9F, 0);
		for (int i = 0; i < 10_000; i++) {
			state = HungerLogic.tick(state, grace, RULES).state();
		}
		assertEquals(10, state.food());
		assertEquals(0.0F, state.exhaustion());
	}

	@Test
	void eachNutrientIsDeficientOnItsOwn() {
		assertEquals(Deficiencies.NONE, HungerLogic.deficiencies(Nutrition.of(0.5F), 0));
		assertEquals(new Deficiencies(false, true, false), HungerLogic.deficiencies(new Nutrition(50, 0, 50), 0));
		assertEquals(new Deficiencies(false, false, true),
				HungerLogic.deficiencies(HungerLogic.decay(new Nutrition(50, 50, 0.25F), RULES.nutrientDecay()), 0));
		assertEquals(new Deficiencies(true, false, true), HungerLogic.deficiencies(new Nutrition(10, 11, 3), 10));
	}

	@Test
	void fatDeficiencyDrainsHungerFasterByTheFactor() {
		TickContext noFat = new TickContext(20, false, true, false, false, false, new Deficiencies(false, true, false));
		float perPoint = RULES.exhaustionPerPoint() / RULES.deficiency().fatHungerFactor();
		TickResult under = HungerLogic.tick(new FoodState(10, 0.0F, perPoint - 0.01F, 0), noFat, RULES);
		assertFalse(under.pointSpent());
		TickResult over = HungerLogic.tick(new FoodState(10, 0.0F, perPoint + 0.01F, 0), noFat, RULES);
		assertTrue(over.pointSpent());
		assertEquals(9, over.state().food());
		TickResult healthy = HungerLogic.tick(new FoodState(10, 0.0F, perPoint + 0.01F, 0), HEALTHY, RULES);
		assertFalse(healthy.pointSpent());
	}

	@Test
	void sprintGivesOutWithoutCarbsAndNeedsRest() {
		DeficiencyRules rules = RULES.deficiency();
		Stamina stamina = Stamina.FRESH;
		for (int i = 0; i < rules.carbsSprintTicks() - 1; i++) {
			stamina = HungerLogic.stamina(stamina, true, true, rules);
			assertFalse(stamina.winded());
		}
		stamina = HungerLogic.stamina(stamina, true, true, rules);
		assertTrue(stamina.winded());
		for (int i = 0; i < rules.carbsRestTicks() - 1; i++) {
			stamina = HungerLogic.stamina(stamina, true, true, rules);
			assertTrue(stamina.winded());
		}
		stamina = HungerLogic.stamina(stamina, true, false, rules);
		assertEquals(Stamina.FRESH, stamina);
	}

	@Test
	void walkingRecoversAndCarbsResetStamina() {
		DeficiencyRules rules = RULES.deficiency();
		Stamina ran = new Stamina(100, 0);
		assertEquals(new Stamina(99, 0), HungerLogic.stamina(ran, true, false, rules));
		assertEquals(Stamina.FRESH, HungerLogic.stamina(new Stamina(0, 50), false, true, rules));
	}

	@Test
	void anyDeficiencySlowsRegenerationByAThird() {
		TickContext malnourished = new TickContext(20, true, true, false, false, false, new Deficiencies(true, false, false));
		int interval = Math.round(RULES.regenIntervalTicks() * RULES.malnourishedRegenFactor());
		assertEquals(900, interval);
		TickResult early = HungerLogic.tick(new FoodState(10, 0.0F, 0.0F, RULES.regenIntervalTicks() - 1), malnourished, RULES);
		assertEquals(0.0F, early.heal());
		TickResult due = HungerLogic.tick(new FoodState(10, 0.0F, 0.0F, interval - 1), malnourished, RULES);
		assertEquals(1.0F, due.heal());
	}

	@Test
	void restedHungerDrainsSlowerAndHealsFaster() {
		TickContext rested = new TickContext(20, true, true, false, false, false, Deficiencies.NONE, 0.9F, 1.1F);
		float perPoint = RULES.exhaustionPerPoint();
		// Just over the normal threshold spends a point, but not when rested.
		assertTrue(HungerLogic.tick(new FoodState(20, 0.0F, perPoint, 0), HURT, RULES).pointSpent());
		assertFalse(HungerLogic.tick(new FoodState(20, 0.0F, perPoint, 0), rested, RULES).pointSpent());
		int restedInterval = Math.round(RULES.regenIntervalTicks() / 1.1F);
		assertEquals(1.0F, HungerLogic.tick(new FoodState(20, 0.0F, 0.0F, restedInterval - 1), rested, RULES).heal());
		assertEquals(0.0F, HungerLogic.tick(new FoodState(20, 0.0F, 0.0F, restedInterval - 1), HURT, RULES).heal());
	}
}
