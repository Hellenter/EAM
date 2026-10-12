package io.github.hellenter.eam.mobs.entity.crossbowman;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.hellenter.eam.mobs.entity.crossbowman.EliteCrossbowmanLogic.Attack;
import io.github.hellenter.eam.mobs.entity.crossbowman.EliteCrossbowmanLogic.Move;

import org.junit.jupiter.api.Test;

class EliteCrossbowmanLogicTest {
	private static final EliteCrossbowmanSettings S = EliteCrossbowmanSettings.DEFAULT;

	private static int[] ready() {
		return new int[Attack.values().length];
	}

	@Test
	void picksByWeight() {
		// Weights 6, 3, 2, 1 out of 12.
		assertEquals(Attack.NORMAL, EliteCrossbowmanLogic.choose(0.0F, ready(), S, 8.0, true));
		assertEquals(Attack.NORMAL, EliteCrossbowmanLogic.choose(5.9F / 12.0F, ready(), S, 8.0, true));
		assertEquals(Attack.BURST, EliteCrossbowmanLogic.choose(6.1F / 12.0F, ready(), S, 8.0, true));
		assertEquals(Attack.CHARGED, EliteCrossbowmanLogic.choose(9.5F / 12.0F, ready(), S, 8.0, true));
		assertEquals(Attack.NET, EliteCrossbowmanLogic.choose(11.5F / 12.0F, ready(), S, 8.0, true));
	}

	@Test
	void netOnlyAtPlayersInRange() {
		for (float roll = 0.0F; roll < 1.0F; roll += 0.01F) {
			assertNotEquals(Attack.NET, EliteCrossbowmanLogic.choose(roll, ready(), S, 8.0, false));
			assertNotEquals(Attack.NET, EliteCrossbowmanLogic.choose(roll, ready(), S, S.net().maxRange() + 1.0, true));
		}
	}

	@Test
	void skipsAttacksOnCooldown() {
		int[] cooldowns = ready();
		cooldowns[Attack.BURST.ordinal()] = 10;
		cooldowns[Attack.CHARGED.ordinal()] = 10;
		cooldowns[Attack.NET.ordinal()] = 10;
		for (float roll = 0.0F; roll < 1.0F; roll += 0.05F) {
			assertEquals(Attack.NORMAL, EliteCrossbowmanLogic.choose(roll, cooldowns, S, 8.0, true));
		}
		cooldowns[Attack.NORMAL.ordinal()] = 10;
		assertNull(EliteCrossbowmanLogic.choose(0.5F, cooldowns, S, 8.0, true));
	}

	@Test
	void keepsHisDistance() {
		assertEquals(Move.RETREAT, EliteCrossbowmanLogic.move(3.0, true, S));
		assertEquals(Move.HOLD, EliteCrossbowmanLogic.move(9.0, true, S));
		assertEquals(Move.APPROACH, EliteCrossbowmanLogic.move(20.0, true, S));
		assertEquals(Move.APPROACH, EliteCrossbowmanLogic.move(9.0, false, S));
	}

	@Test
	void chargedShotWaitsForItsWindup() {
		assertEquals(S.charged().windupTicks(), EliteCrossbowmanLogic.aimTicks(Attack.CHARGED, S));
		assertEquals(S.aimTicks(), EliteCrossbowmanLogic.aimTicks(Attack.NORMAL, S));
	}

	@Test
	void aimsLowerForFasterShots() {
		assertEquals(0.2, EliteCrossbowmanLogic.dropCompensation(1.6, EliteCrossbowmanLogic.ARROW_GRAVITY), 1.0E-9);
		assertEquals(0.05, EliteCrossbowmanLogic.dropCompensation(3.2, EliteCrossbowmanLogic.ARROW_GRAVITY), 1.0E-9);
		assertEquals(0.12, EliteCrossbowmanLogic.dropCompensation(1.6, EliteCrossbowmanLogic.THROWN_GRAVITY), 1.0E-9);
	}

	@Test
	void redBoltThroughShieldAtHalf() {
		float damage = S.normal().damage() * S.charged().damageMultiplier();
		assertEquals(10.0F, damage, 1.0E-6F);
		assertEquals(5.0F, EliteCrossbowmanLogic.redBoltDamage(damage, true, S.charged().shieldMultiplier()), 1.0E-6F);
		assertEquals(10.0F, EliteCrossbowmanLogic.redBoltDamage(damage, false, S.charged().shieldMultiplier()), 1.0E-6F);
	}

	@Test
	void baseDamageUndoesSpeed() {
		assertEquals(5.0, EliteCrossbowmanLogic.baseDamage(5.0F, 1.6F) * 1.6, 1.0E-6);
	}
}
