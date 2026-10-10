package io.github.hellenter.eam.world.swamp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SwampyForestLogicTest {
	/** A world with the same climate everywhere, except continentalness can be set per call. */
	private record FlatClimate(float temperature, float humidity, float continents) implements SwampyForestLogic.Climate {
		@Override
		public float temperature(int x, int z) {
			return this.temperature;
		}

		@Override
		public float humidity(int x, int z) {
			return this.humidity;
		}

		@Override
		public float continents(int x, int z) {
			return this.continents;
		}
	}

	private static final SwampyForestLogic.Climate TEMPERATE = new FlatClimate(0.0F, 0.0F, 0.3F);

	@Test
	void everyTemperateCellHoldsOneSwampAboutAThousandBlocksAcross() {
		int cell = SwampyForestLogic.CELL;
		int widest = 0;
		for (int cz = 0; cz < 3; cz++) {
			for (int cx = 0; cx < 3; cx++) {
				int swampColumns = 0;
				int minX = Integer.MAX_VALUE;
				int maxX = Integer.MIN_VALUE;
				for (int z = cz * cell; z < (cz + 1) * cell; z += 16) {
					for (int x = cx * cell; x < (cx + 1) * cell; x += 16) {
						if (SwampyForestLogic.inSwamp(SwampyForestLogic.weight(TEMPERATE, x, z))) {
							swampColumns++;
							minX = Math.min(minX, x);
							maxX = Math.max(maxX, x);
						}
					}
				}
				float area = swampColumns * 256.0F;
				assertTrue(area > 600_000 && area < 1_000_000, "a round swamp ~1000 across, area " + area);
				widest = Math.max(widest, maxX - minX);
			}
		}
		assertTrue(widest > 900 && widest < 1100, "about 1000 blocks across: " + widest);
	}

	@Test
	void noSwampInHotColdOrOceanCells() {
		for (SwampyForestLogic.Climate climate : new SwampyForestLogic.Climate[] {new FlatClimate(0.6F, 0.0F, 0.3F),
				new FlatClimate(-0.7F, 0.0F, 0.3F), new FlatClimate(0.0F, 0.0F, -0.5F)}) {
			for (int x = 0; x < SwampyForestLogic.CELL; x += 32) {
				assertEquals(0.0F, SwampyForestLogic.weight(climate, x, SwampyForestLogic.CELL / 2));
			}
		}
	}

	@Test
	void humidityPicksThePart() {
		assertEquals(SwampyForestLogic.Part.HUMMOCKS, SwampyForestLogic.part(-0.5F));
		assertEquals(SwampyForestLogic.Part.FOREST, SwampyForestLogic.part(0.0F));
		assertEquals(SwampyForestLogic.Part.LAKES, SwampyForestLogic.part(0.6F));
	}

	@Test
	void plainIsLowLandAndShallowLakes() {
		int sea = SwampyForestLogic.SEA_LEVEL;
		int lake = 0;
		int land = 0;
		for (int x = 0; x < 2000; x += 3) {
			for (int z = 0; z < 2000; z += 7) {
				float height = SwampyForestLogic.surfaceHeight(x, z, 0.0F);
				// Ground is solid below the height: the top solid block is the one under it.
				int top = (int) Math.ceil(height) - 1;
				assertTrue(top >= sea - 4 && top <= sea + 5, "top block " + top);
				if (top < sea - 1) {
					lake++;
					assertTrue(sea - 1 - top <= 3, "lakes at most 3 deep");
				} else {
					land++;
				}
			}
		}
		assertTrue(lake > 0 && land > lake, "mostly land with lakes: " + land + " / " + lake);
		int wetLake = 0;
		for (int x = 0; x < 2000; x += 3) {
			for (int z = 0; z < 2000; z += 7) {
				if (SwampyForestLogic.surfaceHeight(x, z, 0.5F) < sea - 1) {
					wetLake++;
				}
			}
		}
		assertTrue(wetLake > lake * 2, "the wet part has more water: " + wetLake + " / " + lake);
	}

	@Test
	void valueNoiseStaysInRange() {
		for (int i = 0; i < 1000; i++) {
			float value = SwampyForestLogic.valueNoise(i * 0.37F, i * 0.11F, 0);
			assertTrue(value >= -1.0F && value <= 1.0F);
		}
	}

	@Test
	void groundEndsAtThePlainAndKeepsVanillaUnderTheCrust() {
		float height = 65.5F;
		assertTrue(SwampyForestLogic.groundDensity(-1.0F, 1.0F, height, 65) > 0.0F, "solid below the surface even over a vanilla valley");
		assertTrue(SwampyForestLogic.groundDensity(1.0F, 1.0F, height, 66) < 0.0F, "air above it even inside a vanilla mountain");
		assertTrue(SwampyForestLogic.groundDensity(-1.0F, 1.0F, height, 30) < 0.0F, "vanilla caves stay under the crust");
		assertEquals(0.7F, SwampyForestLogic.groundDensity(0.7F, 0.0F, height, 100), "outside the swamp nothing changes");
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
