package io.github.hellenter.eam.world.swamp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SwampyForestLogicTest {
	/** A world with the same climate everywhere. */
	private record FlatClimate(float temperature, float humidity, float continents, float relief) implements SwampyForestLogic.Climate {
		/** Lowland a few blocks above the sea. */
		FlatClimate(float temperature, float humidity, float continents) {
			this(temperature, humidity, continents, 70.0F);
		}

		@Override
		public float relief(int x, int z) {
			return this.relief;
		}

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
				assertTrue(area > 600_000 && area < 1_300_000, "a swamp ~1000 across, area " + area);
				widest = Math.max(widest, maxX - minX);
			}
		}
		assertTrue(widest > 850 && widest < 1400, "about 1000 blocks across: " + widest);
	}

	@Test
	void noSwampInHillsOrMountains() {
		for (SwampyForestLogic.Climate climate : new SwampyForestLogic.Climate[] {new FlatClimate(0.0F, 0.0F, 0.3F, 95.0F),
				new FlatClimate(0.0F, 0.0F, 0.3F, 160.0F)}) {
			for (int z = 0; z < SwampyForestLogic.CELL; z += 32) {
				for (int x = 0; x < SwampyForestLogic.CELL; x += 32) {
					assertEquals(0.0F, SwampyForestLogic.weight(climate, x, z));
				}
			}
		}
	}

	@Test
	void ordinaryForestRingsTheSwamp() {
		int cell = SwampyForestLogic.CELL;
		int rim = 0;
		int nearSwamp = 0;
		for (int z = 0; z < cell; z += 8) {
			for (int x = 0; x < cell; x += 8) {
				float weight = SwampyForestLogic.weight(TEMPERATE, x, z);
				boolean inRim = SwampyForestLogic.inRim(TEMPERATE, x, z, weight);
				assertFalse(inRim && SwampyForestLogic.inSwamp(weight), "the rim is outside the swamp");
				if (inRim) {
					rim++;
					// Every rim column has swamp within the rim's width plus the blend zone.
					boolean swampClose = false;
					for (int a = 0; a < 16 && !swampClose; a++) {
						double angle = a * Math.PI / 8;
						for (int r = 16; r <= SwampyForestLogic.RIM_WIDTH + 140 && !swampClose; r += 16) {
							int sx = x + (int) (Math.cos(angle) * r);
							int sz = z + (int) (Math.sin(angle) * r);
							swampClose = SwampyForestLogic.inSwamp(SwampyForestLogic.weight(TEMPERATE, sx, sz));
						}
					}
					if (swampClose) {
						nearSwamp++;
					}
				}
			}
		}
		assertTrue(rim * 64.0F > 250_000, "a band a couple of hundred blocks wide: " + rim * 64.0F);
		assertTrue(nearSwamp > rim * 0.97, "the rim hugs the swamp: " + nearSwamp + " of " + rim);
	}

	@Test
	void theRimClimbsFoothillsButNotMountains() {
		SwampyForestLogic.Climate hills = new SwampyForestLogic.Climate() {
			@Override
			public float temperature(int x, int z) {
				return 0.0F;
			}

			@Override
			public float humidity(int x, int z) {
				return 0.0F;
			}

			@Override
			public float continents(int x, int z) {
				return 0.3F;
			}

			// Low land in the cell's middle, rising steadily east of x = 1000.
			@Override
			public float relief(int x, int z) {
				return x < 1000 ? 70.0F : 70.0F + (x - 1000) * 0.2F;
			}
		};
		boolean foothills = false;
		for (int z = 0; z < SwampyForestLogic.CELL; z += 16) {
			for (int x = 1000; x < SwampyForestLogic.CELL; x += 16) {
				float relief = hills.relief(x, z);
				boolean inRim = SwampyForestLogic.inRim(hills, x, z, SwampyForestLogic.weight(hills, x, z));
				if (relief > SwampyForestLogic.RIM_LOWLAND_TO + 30) {
					assertFalse(inRim, "no forest rim on mountains at " + x + ", " + z);
				}
				foothills |= inRim && relief > 90.0F;
			}
		}
		assertTrue(foothills, "the rim climbs onto the foothills");
	}

	@Test
	void noRimWithoutASwamp() {
		SwampyForestLogic.Climate hot = new FlatClimate(0.7F, 0.0F, 0.3F);
		for (int z = 0; z < SwampyForestLogic.CELL; z += 32) {
			for (int x = 0; x < SwampyForestLogic.CELL; x += 32) {
				assertFalse(SwampyForestLogic.inRim(hot, x, z, SwampyForestLogic.weight(hot, x, z)));
			}
		}
	}

	@Test
	void rimIsMostlyPlainForestWithBirchAndDarkPatches() {
		int[] counts = new int[SwampyForestLogic.RimForest.values().length];
		for (int z = 0; z < 4000; z += 8) {
			for (int x = 0; x < 4000; x += 8) {
				counts[SwampyForestLogic.rimForest(x, z).ordinal()]++;
			}
		}
		int total = 500 * 500;
		assertTrue(counts[SwampyForestLogic.RimForest.FOREST.ordinal()] > total * 0.5);
		assertTrue(counts[SwampyForestLogic.RimForest.BIRCH.ordinal()] > total * 0.08);
		assertTrue(counts[SwampyForestLogic.RimForest.DARK.ordinal()] > total * 0.04);
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
					assertTrue(sea - 1 - top <= 2, "lakes at most 2 deep");
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
	void duckweedGrowsInSmallPondsAndOnlySomeBigLakes() {
		assertTrue(SwampyForestLogic.duckweedLake(0.1F, -0.5F));
		assertFalse(SwampyForestLogic.duckweedLake(0.8F, 0.0F));
		assertTrue(SwampyForestLogic.duckweedLake(0.8F, 0.7F));
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
	void theRimIsASlopeNotAWall() {
		// A vanilla hill (solid up to y 110) is cut to a ceiling that rises steadily as the weight drops toward the rim.
		float height = 66.0F;
		int previous = 65;
		for (float w = 1.0F; w > 0.05F; w -= 0.05F) {
			int top = 0;
			for (int y = 0; y < 200; y++) {
				float vanilla = Math.clamp((110 - y) * 0.25F, -1.0F, 1.0F);
				if (SwampyForestLogic.groundDensity(vanilla, w, height, y) > 0.0F) {
					top = y;
				}
			}
			assertTrue(top >= previous, "the ground only rises toward the rim");
			assertTrue(top - previous <= 12, "no wall: " + previous + " -> " + top + " at weight " + w);
			previous = top;
		}
		assertEquals(109, previous, "and meets the hill");
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

	@Test
	void hutStandsInFullSwampNearTheMiddle() {
		int[] spot = SwampyForestLogic.hutSpot(TEMPERATE, 2, -3);
		assertTrue(spot != null);
		assertEquals(2, Math.floorDiv(spot[0], SwampyForestLogic.CELL));
		assertEquals(-3, Math.floorDiv(spot[1], SwampyForestLogic.CELL));
		assertTrue(SwampyForestLogic.weight(TEMPERATE, spot[0], spot[1]) >= SwampyForestLogic.HUT_WEIGHT);
		int middleX = 2 * SwampyForestLogic.CELL + SwampyForestLogic.CELL / 2;
		int middleZ = -3 * SwampyForestLogic.CELL + SwampyForestLogic.CELL / 2;
		int reach = SwampyForestLogic.JITTER + (int) (SwampyForestLogic.RADIUS * SwampyForestLogic.SHAPE_WARP) + 1;
		assertTrue(Math.abs(spot[0] - middleX) <= reach && Math.abs(spot[1] - middleZ) <= reach);
	}

	@Test
	void noHutWithoutSwamp() {
		assertEquals(null, SwampyForestLogic.hutSpot(new FlatClimate(0.9F, 0.0F, 0.3F), 0, 0));
	}
}
