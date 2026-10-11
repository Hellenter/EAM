package io.github.hellenter.eam.mobs.entity.bat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VampireBatLogicTest {
	@Test
	void flockComesOnlyWhileFewBatsAreAround() {
		assertTrue(VampireBatLogic.flockDue(0, 10, 0.05F, 0.12F));
		assertFalse(VampireBatLogic.flockDue(0, 10, 0.5F, 0.12F));
		assertFalse(VampireBatLogic.flockDue(10, 10, 0.0F, 0.12F));
	}

	@Test
	void flockHasFourToSixBats() {
		assertEquals(4, VampireBatLogic.flockSize(4, 6, 0.0F));
		assertEquals(5, VampireBatLogic.flockSize(4, 6, 0.5F));
		assertEquals(6, VampireBatLogic.flockSize(4, 6, 0.999F));
		assertEquals(5, VampireBatLogic.flockSize(5, 5, 0.7F));
	}

	@Test
	void flockAppearsFortyToFiftyBlocksAway() {
		assertEquals(40.0, VampireBatLogic.flockDistance(40.0, 50.0, 0.0F));
		assertEquals(45.0, VampireBatLogic.flockDistance(40.0, 50.0, 0.5F), 1.0E-6);
	}

	@Test
	void steersTowardTheTarget() {
		assertEquals(0.05, VampireBatLogic.steer(0.0, 3.0, 0.5, 0.1), 1.0E-9);
		assertEquals(-0.05, VampireBatLogic.steer(0.0, -3.0, 0.5, 0.1), 1.0E-9);
	}
}
