package io.github.hellenter.eam.mobs.entity.fallenlord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLordLogic.Attack;
import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLordLogic.HitStreak;
import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLordLogic.Situation;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

class FallenLordLogicTest {
	private static final FallenLordSettings SETTINGS = FallenLordSettings.DEFAULT;

	private static int[] ready() {
		return new int[Attack.values().length];
	}

	private static Map<Attack, Integer> histogram(int phase, Situation situation, int[] cooldowns, Attack last) {
		Map<Attack, Integer> counts = new EnumMap<>(Attack.class);
		for (int i = 0; i < 1000; i++) {
			Attack attack = FallenLordLogic.choose(phase, situation, cooldowns, last, SETTINGS, i / 1000.0);
			if (attack != null) {
				counts.merge(attack, 1, Integer::sum);
			}
		}
		return counts;
	}

	@Test
	void jabsWhenAPlayerComesClose() {
		Situation close = new Situation(2.0, true, true, 0, 1);
		assertEquals(Attack.POKE, FallenLordLogic.choose(1, close, ready(), null, SETTINGS, 0.5));
	}

	@Test
	void doesNothingWithoutTheSpear() {
		assertNull(FallenLordLogic.choose(1, new Situation(2.0, true, false, 500, 1), ready(), null, SETTINGS, 0.5));
	}

	@Test
	void throwsOnlyAtAPlayerWhoKeepsAway() {
		Situation fresh = new Situation(10.0, true, true, 20, 1);
		assertFalse(histogram(1, fresh, ready(), null).containsKey(Attack.THROW));
		Situation away = new Situation(10.0, true, true, SETTINGS.phaseOne().throwAfter(), 1);
		assertTrue(histogram(1, away, ready(), null).get(Attack.THROW) > 500);
	}

	@Test
	void lastAttackIsLessLikely() {
		Situation mid = new Situation(3.0, true, true, 0, 1);
		Map<Attack, Integer> afterPoke = histogram(1, mid, ready(), Attack.POKE);
		assertTrue(afterPoke.get(Attack.DASH) > afterPoke.get(Attack.POKE) * 3);
	}

	@Test
	void cooldownsRuleAttacksOut() {
		int[] cooldowns = ready();
		cooldowns[Attack.DASH.ordinal()] = 10;
		Situation mid = new Situation(3.0, true, true, 0, 1);
		assertEquals(Map.of(Attack.POKE, 1000), histogram(1, mid, cooldowns, Attack.POKE));
	}

	@Test
	void slashIsOnlyAFallback() {
		Situation close = new Situation(2.0, true, true, 0, 1);
		assertFalse(histogram(2, close, ready(), null).containsKey(Attack.SLASH));
		int[] cooldowns = ready();
		cooldowns[Attack.COMBO.ordinal()] = 5;
		cooldowns[Attack.WHIRL.ordinal()] = 5;
		assertEquals(Attack.SLASH, FallenLordLogic.choose(2, close, cooldowns, Attack.SLASH, SETTINGS, 0.3));
	}

	@Test
	void whirlIsPreferredWithACrowd() {
		Situation alone = new Situation(2.0, true, true, 0, 1);
		Situation crowd = new Situation(2.0, true, true, 0, 3);
		assertTrue(histogram(2, crowd, ready(), null).get(Attack.WHIRL) > histogram(2, alone, ready(), null).get(Attack.WHIRL) + 200);
	}

	@Test
	void shieldCoversTheFrontOnly() {
		// Yaw 0 faces south (+Z).
		assertTrue(FallenLordLogic.shieldCovers(0.0F, 0.0, 2.0));
		assertTrue(FallenLordLogic.shieldCovers(0.0F, 2.0, 0.5));
		assertFalse(FallenLordLogic.shieldCovers(0.0F, 0.0, -2.0));
		assertTrue(FallenLordLogic.shieldCovers(90.0F, -2.0, 0.0));
		assertFalse(FallenLordLogic.shieldCovers(90.0F, 2.0, 0.0));
	}

	@Test
	void axeBreaksTheShieldTwiceAsLong() {
		assertEquals(200, FallenLordLogic.shieldDisableTicks(5.0F, 2.0F));
	}

	@Test
	void quickHitsTriggerTheSwipe() {
		HitStreak streak = HitStreak.NONE;
		streak = FallenLordLogic.addHit(streak, 100, 60);
		streak = FallenLordLogic.addHit(streak, 120, 60);
		assertFalse(FallenLordLogic.shouldSwipe(streak, SETTINGS));
		assertTrue(FallenLordLogic.shouldSwipe(FallenLordLogic.addHit(streak, 150, 60), SETTINGS));
		assertFalse(FallenLordLogic.shouldSwipe(FallenLordLogic.addHit(streak, 200, 60), SETTINGS));
	}

	@Test
	void drinksOnlyWhenHurtEnough() {
		float max = SETTINGS.phaseOne().health();
		assertFalse(FallenLordLogic.shouldDrink(max * 0.6F, max, 0, 0, SETTINGS));
		assertTrue(FallenLordLogic.shouldDrink(max * 0.5F, max, 0, 0, SETTINGS));
	}

	@Test
	void drinksTwicePerPhaseWithACooldown() {
		float max = SETTINGS.phaseTwo().health();
		assertTrue(FallenLordLogic.shouldDrink(10.0F, max, 1, 0, SETTINGS));
		assertFalse(FallenLordLogic.shouldDrink(10.0F, max, 1, 1, SETTINGS));
		assertFalse(FallenLordLogic.shouldDrink(10.0F, max, SETTINGS.regen().usesPerPhase(), 0, SETTINGS));
	}
}
