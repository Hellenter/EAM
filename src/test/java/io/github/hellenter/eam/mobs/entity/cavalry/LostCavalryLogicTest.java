package io.github.hellenter.eam.mobs.entity.cavalry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.hellenter.eam.mobs.entity.cavalry.LostCavalryLogic.FootAttack;
import io.github.hellenter.eam.mobs.entity.cavalry.LostCavalryLogic.Mounted;
import io.github.hellenter.eam.mobs.entity.knight.SkeletonKnightSettings;

import org.junit.jupiter.api.Test;

class LostCavalryLogicTest {
	private static final LostCavalrySettings S = LostCavalrySettings.DEFAULT;
	private static final SkeletonKnightSettings AXE = S.axe();
	private static final LostCavalrySettings.Moves MOVES = S.moves();
	private static final LostCavalrySettings.Lance LANCE = S.lance();
	private static final LostCavalrySettings.Throwing THROWING = S.throwing();

	@Test
	void speedsFollowTheSquareOfTheAttribute() {
		// A skeleton's own speed, and three quarters of its ground speed.
		assertEquals(0.25, LostCavalryLogic.speedOfSkeletonWalk(1.0), 1.0E-9);
		double foot = LostCavalryLogic.speedOfSkeletonWalk(0.75);
		assertEquals(0.75, (foot * foot) / (0.25 * 0.25), 1.0E-9);
		// Twice a walking player's ground speed: the attribute squared is twice the player's attribute.
		double gallop = LostCavalryLogic.speedOfPlayerWalk(2.0);
		assertEquals(0.2, gallop * gallop, 1.0E-9);
		// Running twice as fast needs the square root of two on the navigation speed.
		assertEquals(Math.sqrt(2.0), LostCavalryLogic.modifierFor(2.0), 1.0E-9);
	}

	@Test
	void footOutOfReachNothing() {
		assertNull(LostCavalryLogic.chooseFoot(AXE.reach() + 0.1, true, 0, 0, 0, AXE, MOVES, 0.0, 0.0));
	}

	@Test
	void againstAShieldSlashesHeavyOrSweep() {
		assertEquals(FootAttack.HEAVY, LostCavalryLogic.chooseFoot(2.0, true, 0, 0, 0, AXE, MOVES, 0.1, 0.9));
		assertEquals(FootAttack.SWEEP, LostCavalryLogic.chooseFoot(2.0, true, 0, 0, 0, AXE, MOVES, 0.9, 0.1));
		assertEquals(FootAttack.SLASH, LostCavalryLogic.chooseFoot(2.0, true, 0, 0, 0, AXE, MOVES, 0.9, 0.9));
		// Never the double cut at a raised shield, however the dice fall.
		assertEquals(FootAttack.SLASH, LostCavalryLogic.chooseFoot(2.0, true, 10, 0, 10, AXE, MOVES, 0.0, 0.0));
	}

	@Test
	void withoutAShieldTheDoubleCut() {
		assertEquals(FootAttack.DOUBLE, LostCavalryLogic.chooseFoot(2.0, false, 0, 0, 0, AXE, MOVES, 0.9, 0.1));
		// Never the sweep at a player with no shield: it is for going round one.
		assertEquals(FootAttack.SLASH, LostCavalryLogic.chooseFoot(2.0, false, 0, 10, 0, AXE, MOVES, 0.9, 0.0));
		assertEquals(FootAttack.HEAVY, LostCavalryLogic.chooseFoot(2.0, false, 0, 10, 0, AXE, MOVES, 0.0, 0.9));
	}

	@Test
	void sprintAfterLongChaseOnly() {
		LostCavalrySettings.Speed speed = S.speed();
		assertFalse(LostCavalryLogic.shouldSprint(speed.sprintAfter() - 1, 0, speed));
		assertTrue(LostCavalryLogic.shouldSprint(speed.sprintAfter(), 0, speed));
		assertFalse(LostCavalryLogic.shouldSprint(speed.sprintAfter(), 1, speed));
	}

	@Test
	void chargeWhenReadyInRange() {
		assertEquals(Mounted.CHARGE, LostCavalryLogic.chooseMounted(12.0, true, 0, 0, 2, LANCE, THROWING, 1.0));
		assertEquals(Mounted.RIDE, LostCavalryLogic.chooseMounted(LANCE.minDistance() - 1.0, true, 0, 0, 2, LANCE, THROWING, 1.0));
		assertEquals(Mounted.RIDE, LostCavalryLogic.chooseMounted(12.0, true, 5, 0, 2, LANCE, THROWING, 1.0));
	}

	@Test
	void throwNowAndThenOrAtOnceWhenOutOfRiding() {
		assertEquals(Mounted.THROW, LostCavalryLogic.chooseMounted(12.0, true, 5, 0, 2, LANCE, THROWING, 0.0));
		assertEquals(Mounted.RIDE, LostCavalryLogic.chooseMounted(12.0, true, 5, 0, 2, LANCE, THROWING, 0.5));
		// A player up a ledge gets the lance at the next check, whatever the roll.
		assertEquals(Mounted.THROW, LostCavalryLogic.chooseMounted(12.0, false, 0, 0, 2, LANCE, THROWING, 0.99));
		assertEquals(Mounted.RIDE, LostCavalryLogic.chooseMounted(12.0, false, 0, 0, 2, LANCE, THROWING, 1.0));
		// No spares left, or too far.
		assertEquals(Mounted.RIDE, LostCavalryLogic.chooseMounted(12.0, false, 0, 0, 0, LANCE, THROWING, 0.0));
		assertEquals(Mounted.RIDE, LostCavalryLogic.chooseMounted(THROWING.maxRange() + 1.0, false, 0, 0, 2, LANCE, THROWING, 0.0));
	}

	@Test
	void coveringChargesMoreOftenAndCloser() {
		assertTrue(LostCavalryLogic.chargeCooldown(LANCE, true) < LostCavalryLogic.chargeCooldown(LANCE, false));
		assertTrue(LostCavalryLogic.keepDistance(LANCE, true, 0.5) < LostCavalryLogic.keepDistance(LANCE, false, 0.5));
		assertTrue(LostCavalryLogic.keepDistance(LANCE, true, 0.0) >= LANCE.minDistance());
	}

	@Test
	void shieldTakesMostOfTheLance() {
		assertEquals(LANCE.damage(), LostCavalryLogic.lanceDamage(LANCE, false), 1.0E-6);
		assertEquals(LANCE.damage() * LANCE.blockedShare(), LostCavalryLogic.lanceDamage(LANCE, true), 1.0E-6);
	}

	@Test
	void dataFileMatchesDefaults() throws Exception {
		try (var reader = new java.io.InputStreamReader(LostCavalryLogicTest.class.getResourceAsStream("/data/eam/mobs/lost_cavalry.json"))) {
			var json = com.google.gson.JsonParser.parseReader(reader);
			LostCavalrySettings parsed = LostCavalrySettings.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, json).getOrThrow();
			assertEquals(S.moves(), parsed.moves());
			assertEquals(S.body(), parsed.body());
			assertEquals(S.speed(), parsed.speed());
			assertEquals(S.lance(), parsed.lance());
			assertEquals(S.throwing(), parsed.throwing());
			assertEquals(S.axe().reach(), parsed.axe().reach(), 1.0E-9);
		}
	}
}
