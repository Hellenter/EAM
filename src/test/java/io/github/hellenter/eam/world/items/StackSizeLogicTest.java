package io.github.hellenter.eam.world.items;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class StackSizeLogicTest {
	@ParameterizedTest
	@CsvSource({
			// path, placesBlock, food, vanillaMax, expected
			"cobblestone, true, false, 64, 4",
			"oak_log, true, false, 64, 4",
			"iron_ore, true, false, 64, 4",
			"deepslate_diamond_ore, true, false, 64, 4",
			"sand, true, false, 64, 4",
			"iron_block, true, false, 64, 4",
			"oak_planks, true, false, 64, 16",
			"glass, true, false, 64, 16",
			"torch, true, false, 64, 16",
			"stone_button, true, false, 64, 16",
			"redstone_torch, true, false, 64, 16",
			"oak_sign, true, false, 16, 16",
			"bread, false, true, 64, 8",
			"carrot, false, true, 64, 8",
			"rabbit_stew, false, true, 1, 1",
			"iron_ingot, false, false, 64, 16",
			"raw_iron, false, false, 64, 16",
			"diamond, false, false, 64, 16",
			"gold_nugget, false, false, 64, 32",
			"wheat_seeds, false, false, 64, 64",
			"redstone, false, false, 64, 64",
			"stick, false, false, 64, 64",
			"ender_pearl, false, false, 16, 16",
			"diamond_sword, false, false, 1, 1",
	})
	void maxStackSize(String path, boolean placesBlock, boolean food, int vanillaMax, int expected) {
		assertEquals(expected, StackSizeLogic.maxStackSize(path, placesBlock, food, vanillaMax));
	}
}
