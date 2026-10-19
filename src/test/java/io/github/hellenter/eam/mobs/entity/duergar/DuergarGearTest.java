package io.github.hellenter.eam.mobs.entity.duergar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.hellenter.eam.mobs.entity.duergar.DuergarGear.Slot;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

class DuergarGearTest {
	private static final double[] PIECES = ZombieDuergarSettings.DEFAULT.pieceWeightArray();

	@Test
	void packsEverySlotApart() {
		int gear = 0;
		for (Slot slot : Slot.values()) {
			gear = DuergarGear.with(gear, slot, slot.variants());
		}
		for (Slot slot : Slot.values()) {
			assertEquals(slot.variants(), DuergarGear.piece(gear, slot));
		}
		gear = DuergarGear.with(gear, Slot.CHEST, 0);
		assertEquals(0, DuergarGear.piece(gear, Slot.CHEST));
		assertEquals(Slot.HEAD.variants(), DuergarGear.piece(gear, Slot.HEAD));
		assertEquals(Slot.SHOULDERS.variants(), DuergarGear.piece(gear, Slot.SHOULDERS));
	}

	@Test
	void zombiesAlwaysHaveABeardAndAtMostFourArmorPieces() {
		Random random = new Random(7);
		Set<Integer> outfits = new HashSet<>();
		int bald = 0;
		for (int i = 0; i < 2000; i++) {
			int gear = DuergarGear.scavenged(random::nextDouble, PIECES, 0.5, 0.4);
			outfits.add(gear);
			int beard = DuergarGear.piece(gear, Slot.BEARD);
			assertTrue(beard >= 1 && beard <= Slot.BEARD.variants());
			assertTrue(DuergarGear.armorPieces(gear) <= 4);
			for (Slot slot : Slot.values()) {
				assertTrue(DuergarGear.piece(gear, slot) <= slot.variants());
			}
			if (DuergarGear.piece(gear, Slot.HAIR) == 0) {
				bald++;
			}
		}
		// Hundreds of different looks, about four in ten of them bald.
		assertTrue(outfits.size() > 500, "outfits: " + outfits.size());
		assertTrue(bald > 650 && bald < 950, "bald: " + bald);
	}

	@Test
	void armorAddsUpFromWornPieces() {
		int gear = DuergarGear.with(DuergarGear.with(0, Slot.HEAD, 3), Slot.CHEST, 1);
		assertEquals(5, DuergarGear.armor(gear));
		// Belt, beard and hair are no armor.
		gear = DuergarGear.with(DuergarGear.with(DuergarGear.with(gear, Slot.BELT, 2), Slot.BEARD, 4), Slot.HAIR, 1);
		assertEquals(5, DuergarGear.armor(gear));
		assertEquals(2, DuergarGear.armorPieces(gear));
	}

	@Test
	void ridersTravelLighterThanRenegadesAndTheLeaderWearsTheHornedHelm() {
		Random random = new Random(11);
		int light = 0;
		for (int i = 0; i < 500; i++) {
			int rider = DuergarGear.rider(random::nextDouble, false, 0.5);
			if (DuergarGear.armorPieces(rider) < DuergarGear.armorPieces(DuergarGear.armored(random::nextDouble, false, 0.5))) {
				light++;
			}
			assertTrue(DuergarGear.piece(rider, Slot.HEAD) != 6, "only the leader wears the horned helm");
			assertTrue(DuergarGear.piece(rider, Slot.BEARD) >= 1);
		}
		// More than half ride without shoulder pieces or boots; a renegade never goes without.
		assertTrue(light > 200, "light: " + light);
		int leader = DuergarGear.rider(random::nextDouble, true, 0.5);
		assertEquals(6, DuergarGear.piece(leader, Slot.HEAD));
		assertEquals(5, DuergarGear.armorPieces(leader));
	}
}
