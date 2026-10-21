package io.github.hellenter.eam.world.structures.koscheicastle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class KoscheiCastleLogicTest {
	/** A round patch of samples within {@code radius} of {@code x}, {@code z}. */
	private static KoscheiCastleLogic.Biome disc(int x, int z, double radius) {
		return (sx, sz) -> (sx - x) * (sx - x) + (sz - z) * (sz - z) <= radius * radius;
	}

	@Test
	void aRoundPatchGetsItsMiddle() {
		KoscheiCastleLogic.Patch patch = KoscheiCastleLogic.patch(disc(8, 8, 5.0), 0, 0);
		assertNotNull(patch);
		assertEquals(8, patch.middleX());
		assertEquals(8, patch.middleZ());
		assertTrue(patch.depth() >= KoscheiCastleLogic.MIN_DEPTH);
		assertEquals(8 * KoscheiCastleLogic.SAMPLE + KoscheiCastleLogic.SAMPLE / 2, patch.block()[0]);
	}

	@Test
	void aPatchOverSeveralCellsBelongsOnlyToTheCellOfItsMiddle() {
		// Middle at sample (16, 16): the first sample of cell (1, 1); the patch reaches into all four cells round it.
		KoscheiCastleLogic.Biome biome = disc(16, 16, 6.0);
		assertNotNull(KoscheiCastleLogic.patch(biome, 1, 1));
		assertNull(KoscheiCastleLogic.patch(biome, 0, 0));
		assertNull(KoscheiCastleLogic.patch(biome, 0, 1));
		assertNull(KoscheiCastleLogic.patch(biome, 1, 0));
	}

	@Test
	void aThinPatchIsTooSmall() {
		KoscheiCastleLogic.Biome strip = (x, z) -> z == 8 && x >= 2 && x <= 14;
		assertNull(KoscheiCastleLogic.patch(strip, 0, 0));
		assertNull(KoscheiCastleLogic.patch((x, z) -> false, 0, 0));
	}

	@Test
	void theMiddleOfALongPatchIsInItsMiddle() {
		KoscheiCastleLogic.Biome band = (x, z) -> x >= 2 && x <= 12 && z >= 6 && z <= 10;
		KoscheiCastleLogic.Patch patch = KoscheiCastleLogic.patch(band, 0, 0);
		assertNotNull(patch);
		assertEquals(7, patch.middleX());
		assertEquals(8, patch.middleZ());
		assertEquals(3, patch.depth());
	}

	@Test
	void aboutHalfThePatchesAreChosen() {
		int chosen = 0;
		for (int i = 0; i < 2000; i++) {
			if (KoscheiCastleLogic.chosen(12345L, i * 37, i * 11 - 500, 0.5F)) chosen++;
		}
		assertTrue(chosen > 850 && chosen < 1150, "chosen " + chosen);
		assertEquals(KoscheiCastleLogic.chosen(7L, 3, 4, 0.5F), KoscheiCastleLogic.chosen(7L, 3, 4, 0.5F));
	}
}
