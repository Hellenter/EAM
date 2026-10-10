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

	@Test
	void terrainIsFlatInsideARegionAndVanillaAtItsBorder() {
		assertEquals(1.0F, SwampyForestLogic.flatness(0.0F, 0.5F, 0.5F, 0.5F), 1.0E-6F);
		assertEquals(0.0F, SwampyForestLogic.flatness(0.5F, 0.5F, 0.5F, 0.5F), "outside the region");
		assertEquals(0.0F, SwampyForestLogic.flatness(0.0F, SwampyForestLogic.MIN_HUMIDITY, 0.5F, 0.5F), "on the border");
		float edge = SwampyForestLogic.flatness(0.0F, SwampyForestLogic.MIN_HUMIDITY + SwampyForestLogic.FLAT_MARGIN / 2, 0.5F, 0.5F);
		assertTrue(edge > 0.0F && edge < 1.0F, "blends in over the margin");
		assertEquals(0.0F, SwampyForestLogic.flatness(0.0F, 0.5F, 0.5F, -0.4F), "real ocean stays ocean");
	}

	@Test
	void flatOffsetSqueezesHillsAndRiversTowardSeaLevel() {
		float flat = SwampyForestLogic.FLAT_OFFSET;
		assertEquals(flat, SwampyForestLogic.flatOffset(flat), 1.0E-6F);
		float hill = SwampyForestLogic.flatOffset(0.2F);
		assertTrue(hill > flat && hill < 0.04F, "a hill shrinks to a few blocks");
		assertEquals(SwampyForestLogic.flatOffset(0.3F), SwampyForestLogic.flatOffset(1.0F), 1.0E-6F, "mountains are clamped");
		assertTrue(SwampyForestLogic.flatOffset(-0.2F) > -0.05F, "a river valley becomes shallow");
	}

	@Test
	void duckweedIsASolidCarpetAtTheBankAndThinsOut() {
		assertEquals(4, SwampyForestLogic.duckweedCoverage(1, 5, -1.0F, -0.5F));
		assertTrue(SwampyForestLogic.duckweedCoverage(2, 5, -1.0F, -0.5F) >= 3);
		assertTrue(SwampyForestLogic.duckweedCoverage(5, 5, 0.0F, 0.0F) <= 2);
		assertEquals(0, SwampyForestLogic.duckweedCoverage(9, 5, 0.0F, 0.4F), "open water stays open");
		assertTrue(SwampyForestLogic.duckweedCoverage(9, 5, 0.9F, 0.0F) > 0, "but has a few patches");
		for (int d = 1; d < 12; d++) {
			int stage = SwampyForestLogic.duckweedCoverage(d, 6, 1.0F, 0.5F);
			assertTrue(stage >= 0 && stage <= 4);
		}
	}
}
