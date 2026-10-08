package io.github.hellenter.eam.content.silver;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SilverLogicTest {
	@Test
	void silverHitsUndeadHalfAgainAsHard() {
		// A silver sword: 1 + 3 + 1 = 5 base damage, Smite-like extra on top of any enchantments.
		assertEquals(7.5F, SilverLogic.attackDamage(5.0F, 5.0F, 0.5F, true), 1e-6);
		assertEquals(10.0F, SilverLogic.attackDamage(5.0F, 7.5F, 0.5F, true), 1e-6);
	}

	@Test
	void noBonusAgainstTheLivingOrWithoutSilver() {
		assertEquals(5.0F, SilverLogic.attackDamage(5.0F, 5.0F, 0.5F, false), 1e-6);
		assertEquals(5.0F, SilverLogic.attackDamage(5.0F, 5.0F, 0.0F, true), 1e-6);
	}

	@Test
	void eachArmorPieceReflectsItsShare() {
		float piece = SilverGear.UNDEAD_THORNS_PER_PIECE;
		assertEquals(0.33F, SilverLogic.reflectedDamage(1.0F, piece, piece, piece, piece), 1e-6);
		assertEquals(0.165F, SilverLogic.reflectedDamage(1.0F, piece, 0.0F, piece, 0.0F), 1e-6);
		assertEquals(0.0F, SilverLogic.reflectedDamage(4.0F, 0.0F, 0.0F, 0.0F, 0.0F), 1e-6);
		assertEquals(0.0F, SilverLogic.reflectedDamage(0.0F, piece, piece, piece, piece), 1e-6);
	}
}
