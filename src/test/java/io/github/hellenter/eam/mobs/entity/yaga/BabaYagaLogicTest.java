package io.github.hellenter.eam.mobs.entity.yaga;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.hellenter.eam.mobs.entity.yaga.BabaYagaLogic.Attack;
import io.github.hellenter.eam.mobs.entity.yaga.BabaYagaLogic.Buff;
import io.github.hellenter.eam.mobs.entity.yaga.BabaYagaLogic.Creature;
import io.github.hellenter.eam.mobs.entity.yaga.BabaYagaLogic.GroupKind;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

class BabaYagaLogicTest {
	private static final BabaYagaSettings SETTINGS = BabaYagaSettings.DEFAULT;

	@Test
	void waveGroupSplitsInHalvesWithTheExtraCreatureFirst() {
		List<Creature> group = List.of(Creature.WIGHT, Creature.SOLDIER, Creature.SOLDIER);
		List<List<Creature>> halves = BabaYagaLogic.halves(group, () -> 0.7);
		assertEquals(2, halves.get(0).size());
		assertEquals(1, halves.get(1).size());
		List<Creature> all = new java.util.ArrayList<>(halves.get(0));
		all.addAll(halves.get(1));
		Collections.sort(all);
		List<Creature> expected = new java.util.ArrayList<>(group);
		Collections.sort(expected);
		assertEquals(expected, all);
		assertEquals(List.of(List.of(Creature.SOLDIER), List.of()), BabaYagaLogic.halves(List.of(Creature.SOLDIER), () -> 0.99));
	}

	@Test
	void hurtGroupGetsAbsorptionFarGroupSpeedNearGroupStrength() {
		BabaYagaSettings.Spell spell = SETTINGS.spell();
		assertEquals(Buff.ABSORPTION, BabaYagaLogic.buffFor(30.0, true, spell));
		assertEquals(Buff.SPEED, BabaYagaLogic.buffFor(spell.farDistance() + 1.0, false, spell));
		assertEquals(Buff.STRENGTH, BabaYagaLogic.buffFor(2.0, false, spell));
	}

	@Test
	void spellGoesToTheHurtGroupThenTheNearestSkippingBuffedAndDeadOnes() {
		int[] alive = {3, 2, 4};
		double[] distance = {20.0, 5.0, 10.0};
		assertEquals(1, BabaYagaLogic.groupToBuff(alive, distance, new boolean[3], new boolean[3]));
		assertEquals(2, BabaYagaLogic.groupToBuff(alive, distance, new boolean[] {false, false, true}, new boolean[3]));
		assertEquals(2, BabaYagaLogic.groupToBuff(alive, distance, new boolean[3], new boolean[] {false, true, false}));
		assertEquals(-1, BabaYagaLogic.groupToBuff(new int[] {0, 0, 0}, distance, new boolean[3], new boolean[3]));
	}

	@Test
	void noMoreWightsThanTheLimit() {
		assertEquals(Creature.WIGHT, BabaYagaLogic.creature(0, 1, 0, 2, 3, () -> 0.5));
		assertEquals(Creature.SOLDIER, BabaYagaLogic.creature(0, 1, 1, 3, 3, () -> 0.0));
		assertEquals(Creature.LURKER, BabaYagaLogic.creature(4, 3, 4, 0, 3, () -> 0.0));
		assertEquals(Creature.SOLDIER, BabaYagaLogic.creature(4, 3, 4, 0, 3, () -> 0.999));
	}

	@Test
	void noMoreLurkersThanTheLimit() {
		assertEquals(Creature.LURKER, BabaYagaLogic.creature(4, 3, 4, 0, 1, 0, 1, () -> 0.0));
		assertEquals(Creature.WIGHT, BabaYagaLogic.creature(4, 3, 4, 1, 1, 0, 1, () -> 0.0));
		assertEquals(Creature.SOLDIER, BabaYagaLogic.creature(4, 3, 4, 1, 1, 1, 1, () -> 0.0));
	}

	@Test
	void groupsHaveThreeToFive() {
		assertEquals(3, BabaYagaLogic.groupSize(3, 5, () -> 0.0));
		assertEquals(4, BabaYagaLogic.groupSize(3, 5, () -> 0.5));
		assertEquals(5, BabaYagaLogic.groupSize(3, 5, () -> 0.99999));
	}

	@Test
	void parryWindowIsTheTicksJustBeforeTheStrike() {
		assertFalse(BabaYagaLogic.parries(17, 30, 12));
		assertTrue(BabaYagaLogic.parries(18, 30, 12));
		assertTrue(BabaYagaLogic.parries(29, 30, 12));
		assertFalse(BabaYagaLogic.parries(30, 30, 12));
	}

	@Test
	void fivePerfectParriesEndPhaseOne() {
		assertEquals(5, (int) Math.ceil(SETTINGS.stats().phaseOneHealth() / SETTINGS.swoop().parryDamage()));
	}

	@Test
	void diveArrivesOnTheStrikeTick() {
		assertEquals(1.0, BabaYagaLogic.closingSpeed(10.0, 10, 2.0), 1.0E-9);
		assertEquals(2.0, BabaYagaLogic.closingSpeed(100.0, 10, 2.0), 1.0E-9);
		assertEquals(0.0, BabaYagaLogic.closingSpeed(-1.0, 10, 2.0), 1.0E-9);
	}

	@Test
	void behindIsOppositeTheFacing() {
		// Facing south (+z): straight behind is north.
		double[] spot = BabaYagaLogic.behind(0.0F, 0.0F, 6.0);
		assertArrayEquals(new double[] {0.0, -6.0}, spot, 1.0E-9);
		// Facing east (yaw -90, +x): behind is west.
		spot = BabaYagaLogic.behind(-90.0F, 0.0F, 6.0);
		assertArrayEquals(new double[] {-6.0, 0.0}, spot, 1.0E-9);
	}

	@Test
	void escapeAndEatNeedTheirConditions() {
		BabaYagaSettings.Escape escape = SETTINGS.escape();
		assertTrue(BabaYagaLogic.escapes(escape.hits(), escape, 0));
		assertFalse(BabaYagaLogic.escapes(escape.hits() - 1, escape, 0));
		assertFalse(BabaYagaLogic.escapes(escape.hits(), escape, 10));
		BabaYagaSettings.Eat eat = SETTINGS.eat();
		assertTrue(BabaYagaLogic.eats(50.0F, 0, true, eat));
		assertFalse(BabaYagaLogic.eats(70.0F, 0, true, eat));
		assertFalse(BabaYagaLogic.eats(50.0F, 5, true, eat));
		assertFalse(BabaYagaLogic.eats(50.0F, 0, false, eat));
	}

	@Test
	void beamDealsItsWholeDamageOverItsLength() {
		BabaYagaSettings.Beam beam = SETTINGS.beam();
		int every = 5;
		assertEquals(beam.damage(), BabaYagaLogic.beamTickDamage(beam, every) * ((beam.ticks() + every - 1) / every), 1.0E-4);
	}

	@Test
	void attacksNeedSightAndRangeExceptTheClaws() {
		int[] ready = new int[Attack.values().length];
		assertEquals(Attack.CLAWS, BabaYagaLogic.chooseAttack(10.0, false, false, ready, SETTINGS, () -> 0.5));
		assertNull(BabaYagaLogic.chooseAttack(2.0, false, false, ready, SETTINGS, () -> 0.5));
		assertEquals(Attack.ACID, BabaYagaLogic.chooseAttack(10.0, true, false, ready, SETTINGS, () -> 0.0));
		int[] acidCooling = new int[Attack.values().length];
		acidCooling[Attack.ACID.ordinal()] = 20;
		assertEquals(Attack.THREADS, BabaYagaLogic.chooseAttack(10.0, true, false, acidCooling, SETTINGS, () -> 0.0));
		assertEquals(Attack.BEAM, BabaYagaLogic.chooseAttack(10.0, true, true, acidCooling, SETTINGS, () -> 0.0));
	}

	@Test
	void spellArcPeaksHalfway() {
		assertEquals(0.0, BabaYagaLogic.arc(0.0, 3.0), 1.0E-9);
		assertEquals(3.0, BabaYagaLogic.arc(0.5, 3.0), 1.0E-9);
		assertEquals(0.0, BabaYagaLogic.arc(1.0, 3.0), 1.0E-9);
	}

	@Test
	void waveGroupsTakeTurnsSoTwoGroupsAreOneOfEach() {
		assertEquals(GroupKind.LURKERS, BabaYagaLogic.waveGroupKind(0, GroupKind.LURKERS));
		assertEquals(GroupKind.WIGHTS, BabaYagaLogic.waveGroupKind(1, GroupKind.LURKERS));
		assertEquals(GroupKind.WIGHTS, BabaYagaLogic.waveGroupKind(0, GroupKind.WIGHTS));
		assertEquals(GroupKind.LURKERS, BabaYagaLogic.waveGroupKind(1, GroupKind.WIGHTS));
	}

	@Test
	void lurkerGroupIsOneToTwoLurkersAndOneToThreeSoldiers() {
		BabaYagaSettings.Summon summon = SETTINGS.summon();
		List<Creature> fewest = BabaYagaLogic.waveGroup(GroupKind.LURKERS, summon, () -> 0.0);
		assertEquals(1, Collections.frequency(fewest, Creature.LURKER));
		assertEquals(1, Collections.frequency(fewest, Creature.SOLDIER));
		List<Creature> most = BabaYagaLogic.waveGroup(GroupKind.LURKERS, summon, () -> 0.999);
		assertEquals(2, Collections.frequency(most, Creature.LURKER));
		assertEquals(3, Collections.frequency(most, Creature.SOLDIER));
		assertFalse(most.contains(Creature.WIGHT));
	}

	@Test
	void wightGroupIsOneWightAndTwoToThreeSoldiers() {
		BabaYagaSettings.Summon summon = SETTINGS.summon();
		List<Creature> fewest = BabaYagaLogic.waveGroup(GroupKind.WIGHTS, summon, () -> 0.0);
		assertEquals(1, Collections.frequency(fewest, Creature.WIGHT));
		assertEquals(2, Collections.frequency(fewest, Creature.SOLDIER));
		List<Creature> most = BabaYagaLogic.waveGroup(GroupKind.WIGHTS, summon, () -> 0.999);
		assertEquals(1, Collections.frequency(most, Creature.WIGHT));
		assertEquals(3, Collections.frequency(most, Creature.SOLDIER));
		assertFalse(most.contains(Creature.LURKER));
	}

	@Test
	void reachingTheCapLocksSummoningUntilAlmostAllAreDead() {
		BabaYagaSettings.PhaseTwo two = SETTINGS.phaseTwo();
		assertTrue(BabaYagaLogic.maySummon(3, 0, two));
		assertFalse(BabaYagaLogic.maySummon(4, 0, two));
		int lockout = BabaYagaLogic.summonLockout(4, 0, two);
		assertEquals(300, lockout);
		// Still locked with fewer than four, counting down.
		lockout = BabaYagaLogic.summonLockout(3, lockout, two);
		assertEquals(299, lockout);
		assertFalse(BabaYagaLogic.maySummon(3, lockout, two));
		// One left: the lockout ends at once.
		lockout = BabaYagaLogic.summonLockout(1, lockout, two);
		assertEquals(0, lockout);
		assertTrue(BabaYagaLogic.maySummon(1, lockout, two));
		assertEquals(1, BabaYagaLogic.underCap(4, 3, two));
		assertEquals(0, BabaYagaLogic.underCap(3, 6, two));
	}

	@Test
	void dataFileMatchesTheDefaults() throws Exception {
		try (var reader = new java.io.InputStreamReader(BabaYagaLogicTest.class.getResourceAsStream("/data/eam/mobs/baba_yaga.json"))) {
			var json = com.google.gson.JsonParser.parseReader(reader);
			BabaYagaSettings parsed = BabaYagaSettings.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, json).getOrThrow();
			assertEquals(SETTINGS.claws(), parsed.claws());
			assertEquals(SETTINGS.beam(), parsed.beam());
			assertEquals(SETTINGS.phaseTwo().summonCap(), parsed.phaseTwo().summonCap());
			assertEquals(SETTINGS.phaseTwo().capLockout(), parsed.phaseTwo().capLockout());
			assertEquals(SETTINGS.phaseTwo().lockoutResetBelow(), parsed.phaseTwo().lockoutResetBelow());
		}
	}

	@Test
	void dataFileHasTheGroupMixes() throws Exception {
		try (var reader = new java.io.InputStreamReader(BabaYagaLogicTest.class.getResourceAsStream("/data/eam/mobs/baba_yaga.json"))) {
			var json = com.google.gson.JsonParser.parseReader(reader);
			BabaYagaSettings parsed = BabaYagaSettings.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, json).getOrThrow();
			assertEquals(SETTINGS.summon(), parsed.summon());
		}
	}
}
