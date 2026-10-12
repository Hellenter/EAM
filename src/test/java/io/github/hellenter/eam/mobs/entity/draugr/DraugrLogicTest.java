package io.github.hellenter.eam.mobs.entity.draugr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.hellenter.eam.mobs.entity.draugr.DraugrGear.Faction;
import io.github.hellenter.eam.mobs.entity.draugr.DraugrGear.Slot;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

class DraugrLogicTest {
	private static final List<DraugrSettings.Wave> WAVES = DraugrSettings.DEFAULT.waves();

	@Test
	void threeWavesFifteenThirtyFifty() {
		assertEquals(1, DraugrLogic.wavesDue(WAVES, 0, 0));
		assertEquals(1, DraugrLogic.wavesDue(WAVES, 1, 99));
		assertEquals(2, DraugrLogic.wavesDue(WAVES, 1, 100));
		assertEquals(3, DraugrLogic.wavesDue(WAVES, 2, 200));
		assertEquals(3, DraugrLogic.wavesDue(WAVES, 0, 5000));
		assertEquals(50.0, WAVES.get(2).radius());
	}

	@Test
	void fireHurtsDouble() {
		assertEquals(8.0F, DraugrLogic.damageTaken(4.0F, true, 2.0F));
		assertEquals(4.0F, DraugrLogic.damageTaken(4.0F, false, 2.0F));
	}

	@Test
	void oneToFourArmorPiecesAndVariety() {
		Random random = new Random(42);
		Set<Integer> outfits = new HashSet<>();
		int[] counts = new int[6];
		for (int n = 0; n < 2000; n++) {
			int gear = DraugrGear.choose(random::nextDouble, new double[] {2, 3, 3, 2}, 0.75, 0.4);
			int pieces = DraugrGear.armorPieces(gear);
			assertTrue(pieces >= 1 && pieces <= 4, "pieces " + pieces);
			counts[pieces]++;
			for (Slot slot : Slot.values()) {
				assertTrue(DraugrGear.piece(gear, slot) <= slot.variants());
			}
			outfits.add(gear);
		}
		assertTrue(counts[1] > 0 && counts[4] > 0);
		assertTrue(outfits.size() > 500, "outfits " + outfits.size());
	}

	@Test
	void factionPiecesExist() {
		for (Faction faction : Faction.values()) {
			for (Slot slot : Slot.values()) {
				assertTrue(faction.piece(slot) <= slot.variants());
			}
		}
	}

	@Test
	void ownPieceAlwaysWithFullChance() {
		// Faction roll 0 -> NORSE; one piece; head slot; own roll; no belt.
		double[] rolls = {0.0, 0.0, 0.0, 0.0, 0.99};
		int[] next = {0};
		int gear = DraugrGear.choose(() -> rolls[next[0]++], new double[] {1, 0, 0, 0}, 1.0, 0.0);
		assertEquals(Faction.NORSE.piece(Slot.HEAD), DraugrGear.piece(gear, Slot.HEAD));
		assertEquals(10 - 8, DraugrGear.armor(gear));
	}
}
