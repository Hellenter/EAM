package io.github.hellenter.eam.world.crafting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CraftTimeLogicTest {
	@Test
	void plainCraftTakesItsSeconds() {
		assertEquals(20, CraftTimeLogic.ticks(1.0F, false, 4, 1.0F));
		assertEquals(200, CraftTimeLogic.ticks(10.0F, false, 4, 1.0F));
	}

	@Test
	void craftingTableHalvesTheTime() {
		assertEquals(10, CraftTimeLogic.ticks(1.0F, false, 9, 0.5F));
		assertEquals(50, CraftTimeLogic.ticks(5.0F, false, 3, 0.5F));
	}

	@Test
	void armorPaysPerIngredient() {
		// Iron chestplate: 8 ingots, 5 seconds each, 20 seconds on a table.
		assertEquals(800, CraftTimeLogic.ticks(5.0F, true, 8, 1.0F));
		assertEquals(400, CraftTimeLogic.ticks(5.0F, true, 8, 0.5F));
	}

	@Test
	void ticksFitTheSyncedShort() {
		assertEquals(CraftTimeLogic.MAX_TICKS, CraftTimeLogic.ticks(1000.0F, true, 9, 1.0F));
	}

	@Test
	void progressIsClamped() {
		assertEquals(0, CraftTimeLogic.progress(100, 120, 40));
		assertEquals(15, CraftTimeLogic.progress(115, 100, 40));
		assertEquals(40, CraftTimeLogic.progress(500, 100, 40));
	}

	@Test
	void readyOnlyAfterTheFullTime() {
		assertFalse(CraftTimeLogic.ready(39, 40));
		assertTrue(CraftTimeLogic.ready(40, 40));
		assertTrue(CraftTimeLogic.ready(0, 0));
	}
}
