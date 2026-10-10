package io.github.hellenter.eam.mobs.entity.knight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.hellenter.eam.mobs.entity.knight.SkeletonKnightLogic.Attack;

import org.junit.jupiter.api.Test;

class SkeletonKnightLogicTest {
	private static final SkeletonKnightSettings S = SkeletonKnightSettings.DEFAULT;

	@Test
	void outOfReachNoAttack() {
		assertNull(SkeletonKnightLogic.choose(S.reach() + 0.1, false, 0, 0, S, 0.0, 0.0));
	}

	@Test
	void heavyWhenReadyAndRolled() {
		assertEquals(Attack.HEAVY, SkeletonKnightLogic.choose(2.0, false, 0, 0, S, 0.1, 0.9));
		// The same roll misses against an open player but hits one behind a shield.
		assertEquals(Attack.SLASH, SkeletonKnightLogic.choose(2.0, false, 50, 0, S, 0.5, 0.9));
		assertEquals(Attack.HEAVY, SkeletonKnightLogic.choose(2.0, true, 50, 0, S, 0.5, 0.9));
	}

	@Test
	void cooldownsBlockSpecials() {
		assertEquals(Attack.SLASH, SkeletonKnightLogic.choose(2.0, true, 10, 10, S, 0.0, 0.0));
		assertEquals(Attack.COMBO, SkeletonKnightLogic.choose(2.0, true, 0, 10, S, 0.0, 0.0));
	}

	@Test
	void axeDisablesShield() {
		assertEquals(100, SkeletonKnightLogic.shieldDisableTicks(5.0F, 1.0F));
		assertEquals(0, SkeletonKnightLogic.shieldDisableTicks(0.0F, 1.0F));
	}
}
