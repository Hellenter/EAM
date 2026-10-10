package io.github.hellenter.eam.content.talisman;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TalismanLogicTest {
	@Test
	void fireLightsPortalOnlyOutsideTheOverworld() {
		assertFalse(TalismanLogic.fireLightsPortal(true, true));
		assertTrue(TalismanLogic.fireLightsPortal(false, true));
		// With the rule turned off, vanilla fire works everywhere.
		assertTrue(TalismanLogic.fireLightsPortal(true, false));
	}

	@Test
	void trailReachesThePortalOnIgnitionAndComesBack() {
		assertEquals(0.0, TalismanLogic.trailProgress(0));
		assertEquals(1.0, TalismanLogic.trailProgress(TalismanLogic.IGNITE_TICK));
		assertEquals(0.0, TalismanLogic.trailProgress(TalismanLogic.ACTIVATION_TICKS));
		assertTrue(TalismanLogic.trailProgress(5) < TalismanLogic.trailProgress(10));
		assertTrue(TalismanLogic.trailProgress(30) > TalismanLogic.trailProgress(35));
	}

	@Test
	void frameGlowPeaksOnIgnition() {
		assertEquals(1.0, TalismanLogic.frameGlow(TalismanLogic.IGNITE_TICK));
		assertTrue(TalismanLogic.frameGlow(10) < 1.0);
		assertEquals(0.0, TalismanLogic.frameGlow(TalismanLogic.ACTIVATION_TICKS));
	}

	@Test
	void activationMatchesTheTotemAnimation() {
		assertEquals(40, TalismanLogic.ACTIVATION_TICKS);
		assertTrue(TalismanLogic.IGNITE_TICK < TalismanLogic.ACTIVATION_TICKS);
	}
}
