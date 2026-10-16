package io.github.hellenter.eam.world.forging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ForgingLogicTest {
	@Test
	void coolsOneMinuteAfterLeavingTheFurnace() {
		long coolsAt = ForgingLogic.coolsAt(1000);
		assertEquals(1000 + 1200, coolsAt);
		assertFalse(ForgingLogic.isCooled(coolsAt, 1000 + 1199));
		assertTrue(ForgingLogic.isCooled(coolsAt, 1000 + 1200));
	}

	@Test
	void ironInTheFurnaceOutputStaysHot() {
		assertFalse(ForgingLogic.isCooled(ForgingLogic.NOT_STARTED, Long.MAX_VALUE - 1));
		assertEquals(60, ForgingLogic.secondsLeft(ForgingLogic.NOT_STARTED, 0));
	}

	@Test
	void secondsLeftRoundUp() {
		assertEquals(60, ForgingLogic.secondsLeft(1200, 0));
		assertEquals(1, ForgingLogic.secondsLeft(1200, 1199));
		assertEquals(0, ForgingLogic.secondsLeft(1200, 1200));
		assertEquals(0, ForgingLogic.secondsLeft(1200, 5000));
	}

	@Test
	void anvilForgesOnePlateFromHotIngotsInBothSlots() {
		assertTrue(ForgingLogic.canForge(1, 1, false));
		assertTrue(ForgingLogic.canForge(5, 3, false));
		assertFalse(ForgingLogic.canForge(2, 0, false));
		assertFalse(ForgingLogic.canForge(0, 2, false));
		assertFalse(ForgingLogic.canForge(1, 1, true));
	}

	@Test
	void plateTakesFiveSeconds() {
		assertFalse(ForgingLogic.forged(1099, 1000, ForgingLogic.FORGE_TICKS));
		assertTrue(ForgingLogic.forged(1100, 1000, ForgingLogic.FORGE_TICKS));
		assertTrue(ForgingLogic.forged(1000, 1000, 0));
		assertEquals(50, ForgingLogic.progress(1050, 1000, ForgingLogic.FORGE_TICKS));
		assertEquals(100, ForgingLogic.progress(1500, 1000, ForgingLogic.FORGE_TICKS));
	}

	@Test
	void anvilsWorkOnTheirTierAndBelow() {
		// Copper and silver anvils: flint and copper gear, not iron.
		assertTrue(ForgingLogic.canWorkOn(2, 0));
		assertTrue(ForgingLogic.canWorkOn(2, 2));
		assertFalse(ForgingLogic.canWorkOn(2, 3));
		assertTrue(ForgingLogic.canWorkOn(3, 3));
	}

	@Test
	void hammersForgeFaster() {
		assertEquals(ForgingLogic.FORGE_TICKS, ForgingLogic.forgeTicks(0));
		assertEquals(80, ForgingLogic.forgeTicks(2));
		assertEquals(67, ForgingLogic.forgeTicks(3));
		assertEquals(57, ForgingLogic.forgeTicks(4));
		assertEquals(50, ForgingLogic.forgeTicks(5));
	}

	@Test
	void mithrilPlatesNeedTheMithrilAnvil() {
		assertTrue(ForgingLogic.canForgeOn(3, ForgingLogic.PLATE_ANVIL_TIER));
		assertTrue(ForgingLogic.canForgeOn(4, ForgingLogic.PLATE_ANVIL_TIER));
		assertFalse(ForgingLogic.canForgeOn(2, ForgingLogic.PLATE_ANVIL_TIER));
		assertFalse(ForgingLogic.canForgeOn(3, ForgingLogic.MITHRIL_PLATE_ANVIL_TIER));
		assertTrue(ForgingLogic.canForgeOn(4, ForgingLogic.MITHRIL_PLATE_ANVIL_TIER));
		assertTrue(ForgingLogic.canWorkOn(4, 4));
		assertFalse(ForgingLogic.canWorkOn(3, 4));
	}

	@Test
	void adamantiumPlatesNeedTheAdamantiumAnvil() {
		assertFalse(ForgingLogic.canForgeOn(4, ForgingLogic.ADAMANTIUM_PLATE_ANVIL_TIER));
		assertTrue(ForgingLogic.canForgeOn(5, ForgingLogic.ADAMANTIUM_PLATE_ANVIL_TIER));
		assertTrue(ForgingLogic.canWorkOn(5, 5));
		assertFalse(ForgingLogic.canWorkOn(4, 5));
	}
}
