package io.github.hellenter.eam.world.items;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class StackSizeLogicTest {
	@ParameterizedTest
	@CsvSource({
			// path, placesBlock, food, vanillaMax, expected
			"cobblestone, true, false, 64, 8",
			"oak_log, true, false, 64, 8",
			"iron_ore, true, false, 64, 8",
			"iron_block, true, false, 64, 8",
			"oak_planks, true, false, 64, 8",
			"torch, true, false, 64, 8",
			"oak_sign, true, false, 16, 8",
			"bread, false, true, 64, 8",
			"carrot, false, true, 64, 8",
			"rabbit_stew, false, true, 1, 1",
			"iron_ingot, false, false, 64, 16",
			"raw_iron, false, false, 64, 16",
			"diamond, false, false, 64, 16",
			"gold_nugget, false, false, 64, 64",
			"potion, false, false, 1, 4",
			"splash_potion, false, false, 1, 4",
			"lingering_potion, false, false, 1, 4",
			"wheat_seeds, false, false, 64, 32",
			"redstone, false, false, 64, 32",
			"stick, false, false, 64, 32",
			"arrow, false, false, 64, 32",
			"ender_pearl, false, false, 16, 16",
			"diamond_sword, false, false, 1, 1",
	})
	void maxStackSize(String path, boolean placesBlock, boolean food, int vanillaMax, int expected) {
		assertEquals(expected, StackSizeLogic.maxStackSize(path, placesBlock, food, vanillaMax));
	}
}
