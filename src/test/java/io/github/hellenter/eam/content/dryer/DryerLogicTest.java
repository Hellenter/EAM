package io.github.hellenter.eam.content.dryer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DryerLogicTest {
	private static final DryerSettings SETTINGS = new DryerSettings(100, 1);

	@Test
	void rainStopsDryingButKeepsProgress() {
		assertEquals(11, DryerLogic.advance(10, false));
		assertEquals(10, DryerLogic.advance(10, true));
	}

	@Test
	void doneAfterDryTicks() {
		assertFalse(DryerLogic.done(99, SETTINGS));
		assertTrue(DryerLogic.done(100, SETTINGS));
	}

	@Test
	void onlyDryableFoodThatIsNotDriedYet() {
		assertTrue(DryerLogic.canDry(true, false));
		assertFalse(DryerLogic.canDry(true, true));
		assertFalse(DryerLogic.canDry(false, false));
	}
}
