package io.github.hellenter.eam.mobs.entity.king;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.hellenter.eam.mobs.entity.king.PillagerKingLogic.Attack;
import io.github.hellenter.eam.mobs.entity.king.PillagerKingLogic.Reach;

import org.junit.jupiter.api.Test;

import java.util.function.DoubleSupplier;

class PillagerKingLogicTest {
	private static final PillagerKingSettings SETTINGS = PillagerKingSettings.DEFAULT;
	private static final DoubleSupplier ALWAYS = () -> 0.0;
	private static final DoubleSupplier NEVER = () -> 0.999;

	private static int[] ready() {
		return new int[Attack.values().length];
	}

	private static int[] cooling(Attack... attacks) {
		int[] cooldowns = ready();
		for (Attack attack : attacks) {
			cooldowns[attack.ordinal()] = 100;
		}
		return cooldowns;
	}

	@Test
	void outOfReachHeWalksUntilThePlayerKeepsAway() {
		int[] noRunNoBoomerang = cooling(Attack.CHARGE, Attack.BOOMERANG);
		assertNull(PillagerKingLogic.choose(6.0, false, 10, noRunNoBoomerang, SETTINGS, ALWAYS));
		assertEquals(Attack.THROW, PillagerKingLogic.choose(6.0, false, SETTINGS.axeThrow().after(), noRunNoBoomerang, SETTINGS, ALWAYS));
		// Too close for a throw at a player who does not block, too far for any.
		assertNull(PillagerKingLogic.choose(3.5, false, 100, ready(), SETTINGS, ALWAYS));
		assertNull(PillagerKingLogic.choose(20.0, true, 100, ready(), SETTINGS, ALWAYS));
	}

	@Test
	void aRaisedShieldDrawsTheThrow() {
		assertEquals(Attack.THROW, PillagerKingLogic.choose(6.0, true, 0, cooling(Attack.CHARGE, Attack.BOOMERANG), SETTINGS, ALWAYS));
		assertEquals(Attack.THROW, PillagerKingLogic.choose(2.0, true, 0, ready(), SETTINGS, ALWAYS));
		assertEquals(Attack.ONSLAUGHT, PillagerKingLogic.choose(2.0, true, 0, cooling(Attack.THROW), SETTINGS, ALWAYS));
	}

	@Test
	void inReachHePicksAnAxeAttack() {
		assertEquals(Attack.ONSLAUGHT, PillagerKingLogic.choose(2.0, false, 0, ready(), SETTINGS, ALWAYS));
		assertEquals(Attack.TORNADO, PillagerKingLogic.choose(2.0, false, 0, cooling(Attack.ONSLAUGHT), SETTINGS, ALWAYS));
		assertEquals(Attack.COMBO, PillagerKingLogic.choose(2.0, false, 0, cooling(Attack.ONSLAUGHT, Attack.TORNADO), SETTINGS, ALWAYS));
		assertEquals(Attack.SLASH, PillagerKingLogic.choose(2.0, false, 0, ready(), SETTINGS, NEVER));
	}

	@Test
	void aDashSavesAHitJustOutOfReach() {
		PillagerKingSettings.Dash dash = SETTINGS.dash();
		assertEquals(Reach.HITS, PillagerKingLogic.reach(2.5, 3.0, 0, dash));
		assertEquals(Reach.DASH, PillagerKingLogic.reach(5.0, 3.0, 0, dash));
		assertEquals(Reach.MISSES, PillagerKingLogic.reach(5.0, 3.0, 10, dash));
		assertEquals(Reach.MISSES, PillagerKingLogic.reach(3.0 + dash.range() + 0.1, 3.0, 0, dash));
		assertTrue(PillagerKingLogic.dashIn(6.0, 0, dash));
		assertFalse(PillagerKingLogic.dashIn(3.0, 0, dash));
		assertFalse(PillagerKingLogic.dashIn(6.0, 5, dash));
	}

	@Test
	void runningIsAThirdFasterOnTheGround() {
		double walk = SETTINGS.stats().speed();
		double run = walk * (1.0 + PillagerKingLogic.speedModifier(SETTINGS.run().bonus()));
		// Ground speed goes with the square of the attribute.
		assertEquals(1.33, run * run / (walk * walk), 1.0E-9);
		// Walking he is 15% slower than a sprinting player (attribute 0.13, full input 0.98).
		assertEquals(0.85, walk * walk * 0.98 / (0.13 * 0.98), 0.005);
		assertFalse(PillagerKingLogic.runs(109, SETTINGS.run()));
		assertTrue(PillagerKingLogic.runs(110, SETTINGS.run()));
	}

	@Test
	void theTotemSavesHimOnce() {
		assertTrue(PillagerKingLogic.totemSaves(0.0F, false, false));
		assertFalse(PillagerKingLogic.totemSaves(0.0F, true, false));
		assertFalse(PillagerKingLogic.totemSaves(0.0F, false, true));
		assertFalse(PillagerKingLogic.totemSaves(3.0F, false, false));
	}

	@Test
	void onlyTheStartOfTheComboCanBeParried() {
		assertTrue(PillagerKingLogic.parries(KingAction.COMBO, 0));
		assertTrue(PillagerKingLogic.parries(KingAction.COMBO, KingAction.COMBO_PARRY_END - 1));
		assertFalse(PillagerKingLogic.parries(KingAction.COMBO, KingAction.COMBO_PARRY_END));
		assertFalse(PillagerKingLogic.parries(KingAction.SLASH, 0));
		assertEquals(KingAction.COMBO_PARRY_END, KingAction.COMBO_HITS[0]);
	}

	@Test
	void fromAfarHeChargesOrThrowsTheBoomerang() {
		assertEquals(Attack.CHARGE, PillagerKingLogic.choose(6.0, false, 0, ready(), SETTINGS, ALWAYS));
		assertEquals(Attack.BOOMERANG, PillagerKingLogic.choose(14.0, false, SETTINGS.axeThrow().after(), ready(), SETTINGS, ALWAYS));
		assertEquals(Attack.BOOMERANG, PillagerKingLogic.choose(6.0, true, 0, cooling(Attack.CHARGE), SETTINGS, ALWAYS));
		// In reach he fights with the axes.
		assertEquals(Attack.SLASH, PillagerKingLogic.choose(SETTINGS.reach(), false, 0, ready(), SETTINGS, NEVER));
	}

	@Test
	void theOnslaughtCanBeParriedOnlyInTheLeap() {
		assertFalse(PillagerKingLogic.parries(KingAction.ONSLAUGHT, KingAction.ONSLAUGHT_RUSH_END));
		assertTrue(PillagerKingLogic.parries(KingAction.ONSLAUGHT, KingAction.ONSLAUGHT_PARRY_START));
		assertFalse(PillagerKingLogic.parries(KingAction.ONSLAUGHT, KingAction.ONSLAUGHT_HIT));
	}

	@Test
	void aShieldOnlySoftensTheOnslaught() {
		assertEquals(25.0F, PillagerKingLogic.onslaughtDamage(25.0F, false, 0.25F), 1.0E-6);
		assertEquals(18.75F, PillagerKingLogic.onslaughtDamage(25.0F, true, 0.25F), 1.0E-6);
	}

	@Test
	void theLeapLandsWhereItAims() {
		double v = PillagerKingLogic.leapSpeed(5.0, 10);
		double travelled = 0.0;
		for (int i = 0; i < 10; i++) {
			travelled += v;
			v *= 0.91;
		}
		assertEquals(5.0, travelled, 1.0E-9);
	}
}
