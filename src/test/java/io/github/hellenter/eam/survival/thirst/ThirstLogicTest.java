package io.github.hellenter.eam.survival.thirst;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ThirstLogicTest {
	private static final ThirstSettings SETTINGS = ThirstSettings.DEFAULT;
	private static final float TEMPERATE = 0.8F;
	private static final ThirstLogic.Environment NORMAL = new ThirstLogic.Environment(false, TEMPERATE, false, false);
	private static final ThirstLogic.Environment HOT = new ThirstLogic.Environment(false, 2.0F, false, false);
	private static final ThirstLogic.Environment COLD = new ThirstLogic.Environment(false, 0.0F, false, false);
	private static final ThirstLogic.Environment POISONED = new ThirstLogic.Environment(false, TEMPERATE, true, false);
	private static final ThirstLogic.Environment PEACEFUL = new ThirstLogic.Environment(true, TEMPERATE, false, false);

	@Test
	void passiveDrainCostsOnePointPerThreshold() {
		Thirst thirst = Thirst.full();
		ThirstCounters counters = ThirstCounters.ZERO;
		int ticks = 0;
		while (thirst.water() == Thirst.MAX) {
			ThirstLogic.TickResult result = ThirstLogic.tick(thirst, counters, SETTINGS, NORMAL);
			thirst = result.thirst();
			counters = result.counters();
			ticks++;
		}
		int expected = (int) Math.ceil(SETTINGS.exhaustionPerPoint() / SETTINGS.passiveExhaustion());
		assertTrue(Math.abs(ticks - expected) <= 1, "ticks " + ticks + ", expected about " + expected);
	}

	@Test
	void heatDrainsFaster() {
		ThirstCounters counters = new ThirstCounters(1.0F, 0, 0, 0);
		float normal = ThirstLogic.tick(Thirst.full(), counters, SETTINGS, NORMAL).counters().exhaustion();
		float hot = ThirstLogic.tick(Thirst.full(), counters, SETTINGS, HOT).counters().exhaustion();
		assertEquals((normal - 1.0F) * SETTINGS.hotMultiplier(), hot - 1.0F, 1.0E-6F);
	}

	@Test
	void coldDrainsSlower() {
		ThirstCounters counters = new ThirstCounters(1.0F, 0, 0, 0);
		float normal = ThirstLogic.tick(Thirst.full(), counters, SETTINGS, NORMAL).counters().exhaustion();
		float cold = ThirstLogic.tick(Thirst.full(), counters, SETTINGS, COLD).counters().exhaustion();
		assertEquals((normal - 1.0F) * SETTINGS.coldMultiplier(), cold - 1.0F, 1.0E-6F);
		assertTrue(cold < normal);
	}

	@Test
	void climateFollowsBiomeTemperature() {
		assertEquals(SETTINGS.hotMultiplier(), ThirstLogic.climateMultiplier(SETTINGS, 2.0F));
		assertEquals(1.0F, ThirstLogic.climateMultiplier(SETTINGS, 0.25F));
		assertEquals(SETTINGS.coldMultiplier(), ThirstLogic.climateMultiplier(SETTINGS, -0.5F));
	}

	@Test
	void onlyRainKeepsCauldronWaterClean() {
		// Rain into an empty cauldron, then more rain.
		assertTrue(ThirstLogic.rainWaterAfter(false, 0, 1, true));
		assertTrue(ThirstLogic.rainWaterAfter(true, 1, 2, true));
		// Rain onto water poured by hand does not clean it.
		assertFalse(ThirstLogic.rainWaterAfter(false, 2, 3, true));
		// Any other water spoils rain water.
		assertFalse(ThirstLogic.rainWaterAfter(true, 2, 3, false));
		assertFalse(ThirstLogic.rainWaterAfter(false, 0, 3, false));
		// Taking water out keeps it as it was; an empty cauldron forgets.
		assertTrue(ThirstLogic.rainWaterAfter(true, 3, 2, false));
		assertFalse(ThirstLogic.rainWaterAfter(true, 1, 0, false));
	}

	@Test
	void activityAddsExhaustionUpToCap() {
		ThirstCounters counters = ThirstLogic.addActivity(ThirstCounters.ZERO, 0.1F, SETTINGS);
		assertEquals(0.1F * SETTINGS.activityMultiplier(), counters.exhaustion(), 1.0E-6F);
		counters = ThirstLogic.addActivity(counters, 1000.0F, SETTINGS);
		assertEquals(ThirstLogic.MAX_EXHAUSTION, counters.exhaustion());
	}

	@Test
	void zeroWaterHurtsOncePerInterval() {
		Thirst thirst = new Thirst(0);
		ThirstCounters counters = ThirstCounters.ZERO;
		int hits = 0;
		for (int i = 0; i < SETTINGS.dehydrationInterval() * 3; i++) {
			ThirstLogic.TickResult result = ThirstLogic.tick(thirst, counters, SETTINGS, NORMAL);
			counters = result.counters();
			if (result.dehydrate()) {
				hits++;
			}
		}
		assertEquals(3, hits);
	}

	@Test
	void waterResetsDehydrationTimer() {
		ThirstCounters counters = new ThirstCounters(0.0F, SETTINGS.dehydrationInterval() - 1, 0, 0);
		ThirstLogic.TickResult result = ThirstLogic.tick(new Thirst(1), counters, SETTINGS, NORMAL);
		assertFalse(result.dehydrate());
		assertEquals(0, result.counters().damageTimer());
	}

	@Test
	void peacefulRefillsWithoutDrain() {
		Thirst thirst = new Thirst(10);
		ThirstCounters counters = new ThirstCounters(3.9F, 0, 0, 0);
		for (int i = 0; i < SETTINGS.peacefulRefillInterval(); i++) {
			ThirstLogic.TickResult result = ThirstLogic.tick(thirst, counters, SETTINGS, PEACEFUL);
			assertFalse(result.dehydrate());
			thirst = result.thirst();
			counters = result.counters();
		}
		assertEquals(11, thirst.water());
		assertEquals(0.0F, counters.exhaustion());
	}

	@Test
	void sprintNeedsMoreThanThreshold() {
		assertFalse(ThirstLogic.canSprint(new Thirst(6)));
		assertTrue(ThirstLogic.canSprint(new Thirst(7)));
		assertFalse(ThirstLogic.canSprint(new Thirst(1, 6)));
		assertTrue(ThirstLogic.canSprint(new Thirst(2, 6)));
	}

	@Test
	void sipNeedsRoomAndNoCooldown() {
		assertFalse(ThirstLogic.canSip(Thirst.full(), ThirstCounters.ZERO));
		assertFalse(ThirstLogic.canSip(new Thirst(5), ThirstCounters.ZERO.withSipCooldown(3)));
		assertTrue(ThirstLogic.canSip(new Thirst(5), ThirstCounters.ZERO));
	}

	@Test
	void waterIsClamped() {
		assertEquals(Thirst.MAX, new Thirst(15).add(10).water());
		assertEquals(0, new Thirst(1).add(-5).water());
	}

	@Test
	void poisoningChanceDependsOnBiome() {
		PoisoningSettings poisoning = SETTINGS.poisoning();
		float between = (poisoning.chance() + poisoning.dirtyWaterChance()) / 2.0F;
		assertFalse(ThirstLogic.poisonsOnSip(ThirstCounters.ZERO, SETTINGS, false, between));
		assertTrue(ThirstLogic.poisonsOnSip(ThirstCounters.ZERO, SETTINGS, true, between));
		assertTrue(ThirstLogic.poisonsOnSip(ThirstCounters.ZERO, SETTINGS, false, 0.0F));
	}

	@Test
	void immunityStartsWhenPoisoningEnds() {
		ThirstCounters counters = ThirstCounters.ZERO;
		for (int i = 0; i < 100; i++) {
			counters = ThirstLogic.tick(Thirst.full(), counters, SETTINGS, POISONED).counters();
		}
		assertEquals(SETTINGS.poisoning().immunity(), counters.immunity());
		assertFalse(ThirstLogic.poisonsOnSip(counters, SETTINGS, true, 0.0F));

		for (int i = 0; i < SETTINGS.poisoning().immunity(); i++) {
			counters = ThirstLogic.tick(Thirst.full(), counters, SETTINGS, NORMAL).counters();
		}
		assertEquals(0, counters.immunity());
		assertTrue(ThirstLogic.poisonsOnSip(counters, SETTINGS, true, 0.0F));
	}

	@Test
	void poisoningDrainsThirst() {
		ThirstCounters counters = ThirstLogic.addPoisoning(ThirstCounters.ZERO, SETTINGS);
		assertEquals(SETTINGS.poisoning().thirstExhaustion(), counters.exhaustion(), 1.0E-6F);
	}

	@Test
	void gracePeriodStopsThirst() {
		ThirstLogic.Environment grace = new ThirstLogic.Environment(false, 2.0F, false, true);
		Thirst thirst = new Thirst(10);
		ThirstCounters counters = ThirstCounters.ZERO.withExhaustion(7.9F);
		for (int i = 0; i < 10_000; i++) {
			ThirstLogic.TickResult result = ThirstLogic.tick(thirst, counters, SETTINGS, grace);
			thirst = result.thirst();
			counters = result.counters();
		}
		assertEquals(10, thirst.water());
		assertEquals(0.0F, counters.exhaustion());
	}

	@Test
	void waterBarFollowsItsSize() {
		Thirst small = Thirst.full().withMax(6);
		assertEquals(6, small.water());
		assertTrue(small.isFull());
		assertEquals(6, small.add(5).water());
		assertEquals(6, small.withMax(8).water());
		assertFalse(small.withMax(8).isFull());
	}

	@Test
	void extraPassiveDrainScalesAndCaps() {
		ThirstSettings settings = ThirstSettings.DEFAULT;
		assertEquals(settings.passiveExhaustion() * 2.0F, ThirstLogic.addPassive(ThirstCounters.ZERO, settings, 2.0F).exhaustion(), 1.0E-7);
		ThirstCounters full = ThirstCounters.ZERO.withExhaustion(ThirstLogic.MAX_EXHAUSTION);
		assertEquals(ThirstLogic.MAX_EXHAUSTION, ThirstLogic.addPassive(full, settings, 2.0F).exhaustion());
	}
}
