package io.github.hellenter.eam.world.swamp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SwampyForestLogicTest {
	@Test
	void regionsNeedForestTemperatureWetAndInland() {
		assertTrue(SwampyForestLogic.inRegion(0.0F, 0.2F, 0.3F));
		assertFalse(SwampyForestLogic.inRegion(-0.5F, 0.2F, 0.3F), "too cold");
		assertFalse(SwampyForestLogic.inRegion(0.5F, 0.2F, 0.3F), "too hot");
		assertFalse(SwampyForestLogic.inRegion(0.0F, -0.1F, 0.3F), "too dry");
		assertFalse(SwampyForestLogic.inRegion(0.0F, 0.2F, -0.5F), "ocean");
	}

	@Test
	void humidityPicksThePart() {
		assertEquals(SwampyForestLogic.Part.HUMMOCKS, SwampyForestLogic.part(-0.5F));
		assertEquals(SwampyForestLogic.Part.FOREST, SwampyForestLogic.part(0.0F));
		assertEquals(SwampyForestLogic.Part.LAKES, SwampyForestLogic.part(0.6F));
	}

	@Test
	void outsideARegionNothingChanges() {
		assertNull(SwampyForestLogic.choose(0.9F, 0.2F, 0.3F, 0.0F));
		assertEquals(SwampyForestLogic.Part.FOREST, SwampyForestLogic.choose(0.0F, 0.2F, 0.3F, 0.0F));
	}

	@Test
	void regionMapIsStretchedThreeTimes() {
		assertEquals(0, SwampyForestLogic.regionCoordinate(2));
		assertEquals(1, SwampyForestLogic.regionCoordinate(3));
		assertEquals(-1, SwampyForestLogic.regionCoordinate(-1));
		assertEquals(100, SwampyForestLogic.regionCoordinate(300));
	}
}
