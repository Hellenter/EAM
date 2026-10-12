package io.github.hellenter.eam.mobs.entity.draugr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
	void climbsOnlyTowardATargetAboveHim() {
		// A player five blocks up on a ledge close by: up he goes.
		assertTrue(DraugrLogic.climbs(5.0, 3.0, false, 16.0, 12.0));
		// The player on the same floor: he walks there and climbs no spire on the way.
		assertFalse(DraugrLogic.climbs(0.0, 3.0, false, 16.0, 12.0));
		assertFalse(DraugrLogic.climbs(-4.0, 3.0, false, 16.0, 12.0));
		// Too high up or too far across.
		assertFalse(DraugrLogic.climbs(20.0, 3.0, false, 16.0, 12.0));
		assertFalse(DraugrLogic.climbs(5.0, 15.0, false, 16.0, 12.0));
		// Halfway up he keeps on until he is over the lip, but never climbs past the player.
		assertTrue(DraugrLogic.climbs(0.2, 1.0, true, 16.0, 12.0));
		assertFalse(DraugrLogic.climbs(0.2, 1.0, false, 16.0, 12.0));
		assertFalse(DraugrLogic.climbs(-1.5, 1.0, true, 16.0, 12.0));
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
