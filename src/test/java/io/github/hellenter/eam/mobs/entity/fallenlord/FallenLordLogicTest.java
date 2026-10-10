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
		cooldowns[Attack.HOP_BACK.ordinal()] = 10;
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
		cooldowns[Attack.HEAVY.ordinal()] = 5;
		cooldowns[Attack.BACKSTAB.ordinal()] = 5;
		// A hop back alone (the sword dash is ready) does not stand in for an attack.
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

	@Test
	void hopsInAtAPlayerWhoStaysAway() {
		assertTrue(histogram(1, new Situation(10.0, true, true, 0, 1), ready(), null).containsKey(Attack.HOP));
		assertTrue(histogram(2, new Situation(10.0, true, true, 0, 1), ready(), null).containsKey(Attack.HOP));
		assertFalse(histogram(1, new Situation(20.0, true, true, 0, 1), ready(), null).containsKey(Attack.HOP));
		assertFalse(histogram(1, new Situation(10.0, false, true, 0, 1), ready(), null).containsKey(Attack.HOP));
	}

	@Test
	void hopsInAtAPlayerHeHasNotHitForAWhile() {
		int after = SETTINGS.movement().hopInAfter();
		assertFalse(histogram(1, new Situation(5.0, true, true, 0, 1, after - 1, false, false), ready(), null).containsKey(Attack.HOP));
		assertTrue(histogram(1, new Situation(5.0, true, true, 0, 1, after, false, false), ready(), null).containsKey(Attack.HOP));
		// Not when the player is already close.
		assertFalse(histogram(1, new Situation(3.0, true, true, 0, 1, after, false, false), ready(), null).containsKey(Attack.HOP));
	}

	@Test
	void hopsBackOnceHisShieldIsKnockedOut() {
		int[] cooldowns = ready();
		cooldowns[Attack.THROW.ordinal()] = 50;
		cooldowns[Attack.DASH.ordinal()] = 50;
		Situation crowded = new Situation(2.0, true, true, 0, 1, 0, true, false);
		Map<Attack, Integer> picks = histogram(1, crowded, cooldowns, null);
		assertTrue(picks.get(Attack.HOP_BACK) > 700);
		// Shield up and nothing to follow the hop with: he stands his ground.
		assertFalse(histogram(1, new Situation(2.0, true, true, 0, 1), cooldowns, null).containsKey(Attack.HOP_BACK));
	}

	@Test
	void hopBackLeadsIntoAThrowOrADash() {
		int[] cooldowns = ready();
		assertEquals(Attack.THROW, FallenLordLogic.afterHopBack(1, true, cooldowns));
		cooldowns[Attack.THROW.ordinal()] = 10;
		assertEquals(Attack.DASH, FallenLordLogic.afterHopBack(1, true, cooldowns));
		assertNull(FallenLordLogic.afterHopBack(1, false, ready()));
		assertEquals(Attack.SWORD_DASH, FallenLordLogic.afterHopBack(2, false, ready()));
		cooldowns[Attack.SWORD_DASH.ordinal()] = 10;
		assertNull(FallenLordLogic.afterHopBack(2, false, cooldowns));
	}

	@Test
	void hopsAreTwoToThreeAndAHalfBlocks() {
		FallenLordSettings.Movement movement = SETTINGS.movement();
		assertEquals(2.0, FallenLordLogic.hopLength(false, 3.0, movement, 0.0), 1.0E-9);
		assertEquals(3.5, FallenLordLogic.hopLength(false, 3.0, movement, 1.0), 1.0E-9);
		assertEquals(3.5, FallenLordLogic.hopLength(true, 10.0, movement, 1.0), 1.0E-9);
		// Never onto the player.
		assertEquals(3.0, FallenLordLogic.hopLength(true, 5.0, movement, 1.0), 1.0E-9);
	}

	@Test
	void backstabIsLikelierAgainstARaisedShield() {
		Situation open = new Situation(4.0, true, true, 0, 1, 0, false, false);
		Situation blocking = new Situation(4.0, true, true, 0, 1, 0, false, true);
		assertTrue(histogram(2, blocking, ready(), null).get(Attack.BACKSTAB) > histogram(2, open, ready(), null).get(Attack.BACKSTAB) + 200);
		assertFalse(histogram(1, blocking, ready(), null).containsKey(Attack.BACKSTAB));
	}

	@Test
	void heavyStrikeOnlyWithinItsReach() {
		double reach = FallenLordLogic.SWORD_REACH + SETTINGS.heavyStrike().extraReach();
		assertTrue(histogram(2, new Situation(reach, true, true, 0, 1), ready(), null).containsKey(Attack.HEAVY));
		assertFalse(histogram(2, new Situation(reach + 0.1, true, true, 0, 1), ready(), null).containsKey(Attack.HEAVY));
		assertFalse(histogram(1, new Situation(2.0, true, true, 0, 1), ready(), null).containsKey(Attack.HEAVY));
	}

	@Test
	void backstabGoesRoundTheSideHeIsOn() {
		// The player faces south (+Z); their right is west (-X). He stands east of them, on their left.
		double[] path = FallenLordLogic.backstabPath(0.0F, 3.0, 1.0);
		assertTrue(path[0] > 1.9);
		assertEquals(0.0, path[1], 1.0E-9);
		assertEquals(0.0, path[2], 1.0E-9);
		assertEquals(-FallenLordLogic.BACKSTAB_BEHIND, path[3], 1.0E-9);
		// On their right he passes the west side.
		assertTrue(FallenLordLogic.backstabPath(0.0F, -3.0, 1.0)[0] < -1.9);
	}

	@Test
	void deathXpGrowsWithPlayersUpToFour() {
		FallenLordSettings.Death death = SETTINGS.death();
		assertEquals(750, FallenLordLogic.deathXp(1, death));
		assertEquals(750, FallenLordLogic.deathXp(0, death));
		assertEquals(950, FallenLordLogic.deathXp(2, death));
		assertEquals(1350, FallenLordLogic.deathXp(4, death));
		assertEquals(1350, FallenLordLogic.deathXp(7, death));
	}

	@Test
	void deathOrbsAreSmallAndShareTheXpThirtySeventy() {
		FallenLordSettings.Death death = SETTINGS.death();
		int orbs = FallenLordLogic.orbCount(750, death);
		assertEquals(94, orbs);
		int[] values = FallenLordLogic.orbValues(750, orbs);
		assertEquals(750, java.util.Arrays.stream(values).sum());
		assertTrue(java.util.Arrays.stream(values).max().getAsInt() <= death.orbXp());
		assertEquals(28, FallenLordLogic.innerOrbs(orbs, death));
		assertEquals(1, FallenLordLogic.orbValues(3, FallenLordLogic.orbCount(3, death)).length);
		assertEquals(FallenLordLogic.MAX_ORBS, FallenLordLogic.orbCount(100000, death));
	}

	@Test
	void spearDashGrowsAlongItsRun() {
		FallenLordSettings.SpearDash dash = SETTINGS.later().spearDash();
		assertEquals(0.697, FallenLordLogic.alongDash(dash.startReach(), dash.endReach(), 0.0), 1.0E-9);
		assertEquals(1.08, FallenLordLogic.alongDash(dash.startReach(), dash.endReach(), 1.0), 1.0E-9);
		assertEquals(1.3, FallenLordLogic.alongDash(dash.startDamage(), dash.endDamage(), 2.0), 1.0E-6);
		assertEquals(1.075, FallenLordLogic.alongDash(dash.startDamage(), dash.endDamage(), 0.5), 1.0E-6);
	}

	@Test
	void dashBreaksTwoBlockWallButNotThree() {
		double max = SETTINGS.later().dashBreak().maxDepth();
		// Straight along +X: a wall two blocks thick, two blocks tall (same spans stacked) is 2 deep.
		List<FallenLordLogic.Span> two = List.of(FallenLordLogic.blockSpan(10.5, 1.0, 0.0), FallenLordLogic.blockSpan(10.5, 1.0, 0.0),
				FallenLordLogic.blockSpan(11.5, 1.0, 0.0), FallenLordLogic.blockSpan(11.5, 1.0, 0.0));
		assertEquals(2.0, FallenLordLogic.dashDepth(two), 1.0E-9);
		assertTrue(FallenLordLogic.dashDepth(two) <= max);
		List<FallenLordLogic.Span> three = new java.util.ArrayList<>(two);
		three.add(FallenLordLogic.blockSpan(12.5, 1.0, 0.0));
		assertTrue(FallenLordLogic.dashDepth(three) > max);
		// Two thin walls with a gap add up.
		assertEquals(2.0, FallenLordLogic.dashDepth(List.of(FallenLordLogic.blockSpan(3.5, 1.0, 0.0), FallenLordLogic.blockSpan(8.5, 1.0, 0.0))), 1.0E-9);
		// Corner to corner a block is longer.
		double d = Math.sqrt(0.5);
		assertEquals(2.0 * d, FallenLordLogic.dashDepth(List.of(FallenLordLogic.blockSpan(0.0, d, d))), 1.0E-9);
		assertTrue(FallenLordLogic.inDashPath(0.8, 0.35));
		assertFalse(FallenLordLogic.inDashPath(1.2, 0.35));
	}

	@Test
	void breatherAfterManyHitsOrSixHearts() {
		FallenLordSettings.Breather breather = SETTINGS.later().breather();
		List<Wound> three = List.of(new Wound(100, 2.0F), new Wound(110, 2.0F), new Wound(120, 2.0F));
		assertFalse(FallenLordLogic.needsBreather(three, 150, 4, breather));
		List<Wound> four = List.of(new Wound(100, 1.0F), new Wound(110, 1.0F), new Wound(120, 1.0F), new Wound(170, 1.0F));
		assertTrue(FallenLordLogic.needsBreather(four, 170, 4, breather));
		assertFalse(FallenLordLogic.needsBreather(four, 170, 5, breather));
		// Older than four seconds no longer counts.
		assertFalse(FallenLordLogic.needsBreather(four, 190, 4, breather));
		List<Wound> heavy = List.of(new Wound(100, 7.0F), new Wound(130, 5.0F));
		assertTrue(FallenLordLogic.needsBreather(heavy, 140, 5, breather));
		assertEquals(4, FallenLordLogic.breatherHits(breather, 0.0));
		assertEquals(5, FallenLordLogic.breatherHits(breather, 0.99));
	}

	@Test
	void leapBackGetsLikelierWithEveryAttack() {
		FallenLordSettings.LeapBack leapBack = SETTINGS.later().leapBack();
		assertEquals(0.0, FallenLordLogic.leapBackChance(1, leapBack), 1.0E-9);
		assertEquals(0.25, FallenLordLogic.leapBackChance(2, leapBack), 1.0E-9);
		assertEquals(0.5, FallenLordLogic.leapBackChance(3, leapBack), 1.0E-9);
		assertEquals(1.0, FallenLordLogic.leapBackChance(9, leapBack), 1.0E-9);
	}

	@Test
	void stanceBreaksWhenThePlayerRunsOrHeals() {
		FallenLordSettings.Stance stance = SETTINGS.later().stance();
		assertFalse(FallenLordLogic.stanceBroken(5.0, 4.0, 10.0F, 10.0F, false, stance));
		assertTrue(FallenLordLogic.stanceBroken(7.0, 4.0, 10.0F, 10.0F, false, stance));
		assertTrue(FallenLordLogic.stanceBroken(4.0, 4.0, 11.0F, 10.0F, false, stance));
		assertTrue(FallenLordLogic.stanceBroken(4.0, 4.0, 10.0F, 10.0F, true, stance));
	}

	@Test
	void leapsUpAtAPlayerOnAPillar() {
		FallenLordSettings.LeapUp leapUp = SETTINGS.later().leapUp();
		assertFalse(FallenLordLogic.shouldLeapUp(1.0, 1.0, leapUp));
		assertTrue(FallenLordLogic.shouldLeapUp(4.0, 1.0, leapUp));
		assertFalse(FallenLordLogic.shouldLeapUp(4.0, 9.0, leapUp));
		assertFalse(FallenLordLogic.shouldLeapUp(12.0, 1.0, leapUp));
	}

	@Test
	void leapUpRisesFastAndSlowsToTheTop() {
		assertEquals(0.0, FallenLordLogic.leapUpHeight(0, 8), 1.0E-9);
		assertTrue(FallenLordLogic.leapUpHeight(4, 8) > 0.5);
		assertEquals(1.0, FallenLordLogic.leapUpHeight(8, 8), 1.0E-9);
		assertEquals(1.0, FallenLordLogic.leapUpHeight(12, 8), 1.0E-9);
	}

	@Test
	void aPlayerComingAtTheStanceGetsTheLunge() {
		FallenLordSettings.Stance stance = SETTINGS.later().stance();
		assertTrue(FallenLordLogic.stanceApproached(3.0, 3.5, stance));
		assertTrue(FallenLordLogic.stanceApproached(5.0, 6.0, stance));
		assertFalse(FallenLordLogic.stanceApproached(5.0, 5.2, stance));
		assertFalse(FallenLordLogic.stanceApproached(7.0, 9.0, stance));
	}

	@Test
	void aPassivePlayerGetsTheDashInItsRange() {
		assertFalse(FallenLordLogic.dashFromStance(2.0));
		assertTrue(FallenLordLogic.dashFromStance(5.0));
		assertFalse(FallenLordLogic.dashFromStance(12.0));
	}

	@Test
	void higherJumpsNeedMoreSpeed() {
		assertEquals(0.42, FallenLordLogic.jumpSpeed(1.0), 1.0E-9);
		double two = FallenLordLogic.jumpSpeed(2.2);
		double three = FallenLordLogic.jumpSpeed(3.2);
		assertTrue(two > 0.42 && three > two && three < 1.5);
	}

	@Test
	void aPlayerHeCannotCatchCountsUpUntilInReach() {
		assertEquals(5, FallenLordLogic.chaseTicks(4, 6.0, 1, false));
		assertEquals(4, FallenLordLogic.chaseTicks(4, 6.0, 1, true));
		assertEquals(0, FallenLordLogic.chaseTicks(40, 3.5, 1, false));
		assertEquals(0, FallenLordLogic.chaseTicks(40, 3.5, 2, true));
		assertEquals(41, FallenLordLogic.chaseTicks(40, 4.0, 2, false));
	}

	@Test
	void inHisRoomHeBreaksAnythingSofterThanObsidian() {
		assertTrue(FallenLordLogic.breakableInArena(1.5F));
		assertTrue(FallenLordLogic.breakableInArena(0.0F));
		assertTrue(FallenLordLogic.breakableInArena(49.9F));
		assertFalse(FallenLordLogic.breakableInArena(50.0F));
		assertFalse(FallenLordLogic.breakableInArena(-1.0F));
	}
}
