package io.github.hellenter.eam.world.structures.lostcastle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.hellenter.eam.mobs.entity.lost.LostRole;
import io.github.hellenter.eam.world.structures.village.DuergarVillageLogic;

import org.junit.jupiter.api.Test;

class LostCastleLogicTest {
	private static final int HALF = 18;

	/** A cave with its floor at {@code floor} and its roof at {@code roof}, rock below and above. */
	private static LostCastleLogic.Columns cave(int floor, int roof) {
		return (x, z) -> y -> y <= floor || y >= roof ? DuergarVillageLogic.SOLID : DuergarVillageLogic.AIR;
	}

	@Test
	void wideFloorIsASite() {
		LostCastleLogic.Site site = LostCastleLogic.site(cave(80, 110), 0, 0, 80, HALF);
		assertNotNull(site);
		assertEquals(80, site.ground());
		assertEquals(49, site.open());
	}

	@Test
	void lowRoofIsNoSite() {
		assertNull(LostCastleLogic.site(cave(80, 88), 0, 0, 80, HALF));
	}

	@Test
	void standsOnTheHighestFloorWithinTheStep() {
		LostCastleLogic.Columns sloped = (x, z) -> y -> y <= 80 + (x > 0 ? 3 : 0) || y >= 120 ? DuergarVillageLogic.SOLID : DuergarVillageLogic.AIR;
		LostCastleLogic.Site site = LostCastleLogic.site(sloped, 0, 0, 80, HALF);
		assertNotNull(site);
		assertEquals(83, site.ground());
	}

	@Test
	void aWallAcrossTheFootprintIsNoSite() {
		// Rock fills the eastern third: too few samples are open.
		LostCastleLogic.Columns walled = (x, z) -> y -> y <= 80 || y >= 120 || x > 6 ? DuergarVillageLogic.SOLID : DuergarVillageLogic.AIR;
		assertNull(LostCastleLogic.site(walled, 0, 0, 80, HALF));
	}

	@Test
	void prefersMoreOpenFloorThenTheMiddle() {
		LostCastleLogic.Site near = new LostCastleLogic.Site(8, 0, 80, 45);
		LostCastleLogic.Site far = new LostCastleLogic.Site(16, 0, 80, 45);
		LostCastleLogic.Site open = new LostCastleLogic.Site(16, 16, 80, 49);
		assertTrue(LostCastleLogic.better(near, null, 0, 0));
		assertTrue(LostCastleLogic.better(near, far, 0, 0));
		assertFalse(LostCastleLogic.better(far, near, 0, 0));
		assertTrue(LostCastleLogic.better(open, near, 0, 0));
	}

	@Test
	void garrisonIsMostlyShieldBearers() {
		assertEquals(LostRole.ARCHER, LostCastleLogic.garrisonRole(0.1));
		assertEquals(LostRole.SPEARMAN, LostCastleLogic.garrisonRole(0.45));
		assertEquals(LostRole.SWORDSMAN, LostCastleLogic.garrisonRole(0.9));
	}
}
