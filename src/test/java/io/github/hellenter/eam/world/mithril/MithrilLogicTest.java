package io.github.hellenter.eam.world.mithril;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MithrilLogicTest {
	@Test
	void obsidianIsSofter() {
		assertEquals(15.0F, MithrilLogic.destroySpeed(true, 50.0F));
		assertEquals(3.0F, MithrilLogic.destroySpeed(false, 3.0F));
	}

	@Test
	void mithrilCooksOnlyInTheObsidianFurnace() {
		assertTrue(MithrilLogic.mayCook(true, true));
		assertFalse(MithrilLogic.mayCook(true, false));
		assertTrue(MithrilLogic.mayCook(false, false));
		assertTrue(MithrilLogic.mayCook(false, true));
	}
}
