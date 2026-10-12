package io.github.hellenter.eam.dimension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.hellenter.eam.dimension.UnderworldLogic.Place;
import org.junit.jupiter.api.Test;

class UnderworldLogicTest {
	@Test
	void netherIsReachedOnlyThroughTheLowerLevel() {
		assertEquals(Place.UNDERWORLD_UPPER, UnderworldLogic.portalTarget(Place.OVERWORLD));
		assertEquals(Place.OVERWORLD, UnderworldLogic.portalTarget(Place.UNDERWORLD_UPPER));
		assertEquals(Place.NETHER, UnderworldLogic.portalTarget(Place.UNDERWORLD_LOWER));
		assertEquals(Place.UNDERWORLD_LOWER, UnderworldLogic.portalTarget(Place.NETHER));
		assertEquals(Place.OTHER, UnderworldLogic.portalTarget(Place.OTHER));
	}

	@Test
	void levelsSplitInsideTheBedrockBand() {
		assertTrue(UnderworldLogic.LEVEL_SPLIT > UnderworldLogic.BAND_BOTTOM);
		assertTrue(UnderworldLogic.LEVEL_SPLIT <= UnderworldLogic.BAND_TOP);
		assertEquals(Place.UNDERWORLD_LOWER, UnderworldLogic.underworldLevel(20));
		assertEquals(Place.UNDERWORLD_UPPER, UnderworldLogic.underworldLevel(100));
		assertTrue(UnderworldLogic.inLevel(Place.UNDERWORLD_LOWER, UnderworldLogic.LEVEL_SPLIT - 1));
		assertFalse(UnderworldLogic.inLevel(Place.UNDERWORLD_LOWER, UnderworldLogic.LEVEL_SPLIT));
	}

	@Test
	void arrivalStaysInsideTheTargetLevel() {
		assertEquals(UnderworldLogic.UPPER_PORTAL_MIN_Y, UnderworldLogic.arrivalY(Place.UNDERWORLD_UPPER, -40));
		assertEquals(UnderworldLogic.UPPER_PORTAL_MAX_Y, UnderworldLogic.arrivalY(Place.UNDERWORLD_UPPER, 300));
		assertEquals(80, UnderworldLogic.arrivalY(Place.UNDERWORLD_UPPER, 80));
		assertEquals(UnderworldLogic.LOWER_PORTAL_MAX_Y, UnderworldLogic.arrivalY(Place.UNDERWORLD_LOWER, 100));
		assertEquals(70, UnderworldLogic.arrivalY(Place.NETHER, 70));
		// A portal built at the top of either range still sits below the level's ceiling.
		assertTrue(UnderworldLogic.portalMaxY(Place.UNDERWORLD_LOWER) + 5 < UnderworldLogic.BAND_BOTTOM);
		assertTrue(UnderworldLogic.portalMinY(Place.UNDERWORLD_UPPER) > UnderworldLogic.BAND_TOP);
	}

	@Test
	void portalSearchWidensFromVanillaRadius() {
		int[] radii = UnderworldLogic.PORTAL_SEARCH_RADII;
		assertEquals(16, radii[0]);
		for (int i = 1; i < radii.length; i++) {
			assertTrue(radii[i] > radii[i - 1]);
		}
	}
}
