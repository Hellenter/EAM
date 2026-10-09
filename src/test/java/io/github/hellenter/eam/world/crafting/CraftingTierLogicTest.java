package io.github.hellenter.eam.world.crafting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.List;

class CraftingTierLogicTest {
	private static final List<Float> BONUSES = List.of(0.15F, 0.25F, 0.35F);

	@Test
	void metalNeedsTheTierBelow() {
		// No metal and flint need nothing beyond any tool.
		assertTrue(CraftingTierLogic.canCraft(0, 0));
		assertTrue(CraftingTierLogic.canCraft(1, 0));
		// Copper and silver need flint or better.
		assertFalse(CraftingTierLogic.canCraft(2, 0));
		assertTrue(CraftingTierLogic.canCraft(2, 1));
		assertTrue(CraftingTierLogic.canCraft(2, 3));
		// Iron needs copper or silver or better.
		assertFalse(CraftingTierLogic.canCraft(3, 1));
		assertTrue(CraftingTierLogic.canCraft(3, 2));
		// Mithril needs iron or better.
		assertFalse(CraftingTierLogic.canCraft(4, 2));
		assertTrue(CraftingTierLogic.canCraft(4, 3));
	}

	@Test
	void toolsStackTheirSpeed() {
		assertEquals(1.0F, CraftingTierLogic.timeFactor(List.of(), BONUSES), 1e-6);
		assertEquals(1.0F / 1.15F, CraftingTierLogic.timeFactor(List.of(1), BONUSES), 1e-6);
		assertEquals(1.0F / 1.75F, CraftingTierLogic.timeFactor(List.of(1, 2, 3), BONUSES), 1e-6);
		assertEquals(1.0F / 2.05F, CraftingTierLogic.timeFactor(List.of(3, 3, 3), BONUSES), 1e-6);
	}

	@Test
	void unknownTiersAddNothing() {
		assertEquals(1.0F, CraftingTierLogic.timeFactor(List.of(0, 4), BONUSES), 1e-6);
	}

	@Test
	void differentToolsStackTheirBonuses() {
		// A copper knife and an iron saw on a wooden craft: 1 + 0.25 + 0.5.
		float bonus = CraftingTierLogic.speedBonus(List.of(2), BONUSES)
				+ CraftingTierLogic.speedBonus(List.of(3), List.of(0.0F, 0.35F, 0.5F));
		assertEquals(0.75F, bonus, 1e-6);
		assertEquals(1.0F / 1.75F, CraftingTierLogic.timeFactor(bonus), 1e-6);
	}
}
