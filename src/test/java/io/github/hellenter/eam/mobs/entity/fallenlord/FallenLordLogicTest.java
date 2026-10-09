package io.github.hellenter.eam.mobs.entity.fallenlord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLordLogic.Attack;
import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLordLogic.BlockStreak;
import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLordLogic.HitStreak;
import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLordLogic.Situation;
import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLordLogic.Wound;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
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
	void throwsFromFourAndAHalfBlocks() {
		int after = SETTINGS.phaseOne().throwAfter();
		assertTrue(histogram(1, new Situation(4.6, true, true, after, 1), ready(), null).containsKey(Attack.THROW));
		assertFalse(histogram(1, new Situation(4.4, true, true, after, 1), ready(), null).containsKey(Attack.THROW));
	}

	@Test
	void jabReachIsAThirdOfABlockShorter() {
		assertEquals(Attack.POKE, FallenLordLogic.choose(1, new Situation(3.2, true, true, 0, 1), ready(), Attack.DASH, SETTINGS, 0.0));
		int[] cooldowns = ready();
		cooldowns[Attack.DASH.ordinal()] = 10;
		assertNull(FallenLordLogic.choose(1, new Situation(3.4, true, true, 0, 1), cooldowns, null, SETTINGS, 0.5));
	}

	@Test
	void swordDashClosesTheGapInPhaseTwo() {
		assertTrue(histogram(2, new Situation(6.0, true, true, 0, 1), ready(), null).containsKey(Attack.SWORD_DASH));
		assertFalse(histogram(2, new Situation(2.0, true, true, 0, 1), ready(), null).containsKey(Attack.SWORD_DASH));
		assertFalse(histogram(1, new Situation(6.0, true, true, 0, 1), ready(), null).containsKey(Attack.SWORD_DASH));
		assertEquals(SETTINGS.swordDash().cooldown(), FallenLordLogic.cooldown(Attack.SWORD_DASH, SETTINGS));
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

	@Test
	void leapsBackAfterLosingAThirdQuickly() {
		FallenLordSettings.Burst burst = SETTINGS.burst();
		float max = 100.0F;
		List<Wound> quick = List.of(new Wound(100L, 12.0F), new Wound(130L, 10.0F), new Wound(160L, 9.0F));
		assertTrue(FallenLordLogic.lostQuickly(quick, 160L, max, burst));
		assertFalse(FallenLordLogic.lostQuickly(quick.subList(0, 2), 160L, max, burst));
		// The first wound is too old by now.
		assertFalse(FallenLordLogic.lostQuickly(quick, 100L + burst.window() + 1, max, burst));
		assertFalse(FallenLordLogic.lostQuickly(List.of(), 0L, max, burst));
	}

	@Test
	void knocksTheShieldOutAfterTwoOrThreeBlocks() {
		FallenLordSettings.ShieldBreak settings = SETTINGS.shieldBreak();
		BlockStreak low = FallenLordLogic.addBlock(BlockStreak.NONE, 100L, settings, 0.0);
		assertEquals(2, low.needed());
		assertFalse(FallenLordLogic.breaksShield(low));
		assertTrue(FallenLordLogic.breaksShield(FallenLordLogic.addBlock(low, 120L, settings, 0.9)));

		BlockStreak high = FallenLordLogic.addBlock(BlockStreak.NONE, 100L, settings, 0.99);
		assertEquals(3, high.needed());
		high = FallenLordLogic.addBlock(high, 120L, settings, 0.0);
		assertFalse(FallenLordLogic.breaksShield(high));
		assertTrue(FallenLordLogic.breaksShield(FallenLordLogic.addBlock(high, 140L, settings, 0.0)));
	}

	@Test
	void blocksFarApartStartTheCountAgain() {
		FallenLordSettings.ShieldBreak settings = SETTINGS.shieldBreak();
		BlockStreak first = FallenLordLogic.addBlock(BlockStreak.NONE, 100L, settings, 0.0);
		BlockStreak late = FallenLordLogic.addBlock(first, 100L + settings.window() + 1, settings, 0.0);
		assertEquals(1, late.hits());
		assertFalse(FallenLordLogic.breaksShield(late));
	}

	@Test
	void scalesWithPlayers() {
		FallenLordSettings.PlayerScaling scaling = SETTINGS.players();
		assertEquals(0.0, FallenLordLogic.scaling(1, scaling).damage(), 1.0E-9);
		assertEquals(0.0, FallenLordLogic.scaling(1, scaling).health(), 1.0E-9);
		assertEquals(0.0, FallenLordLogic.scaling(0, scaling).damage(), 1.0E-9);
		assertEquals(0.15, FallenLordLogic.scaling(2, scaling).health(), 1.0E-9);
		assertEquals(0.15, FallenLordLogic.scaling(2, scaling).damage(), 1.0E-9);
		assertEquals(0.35, FallenLordLogic.scaling(3, scaling).health(), 1.0E-9);
		assertEquals(0.30, FallenLordLogic.scaling(3, scaling).damage(), 1.0E-9);
		assertEquals(0.60, FallenLordLogic.scaling(4, scaling).health(), 1.0E-9);
		assertEquals(0.10, FallenLordLogic.scaling(4, scaling).speed(), 1.0E-9);
		assertEquals(0.45, FallenLordLogic.scaling(9, scaling).damage(), 1.0E-9);
	}
}
