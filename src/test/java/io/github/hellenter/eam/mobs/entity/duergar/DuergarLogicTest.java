package io.github.hellenter.eam.mobs.entity.duergar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.hellenter.eam.mobs.entity.duergar.DuergarGear.Slot;

import org.junit.jupiter.api.Test;

import java.util.Random;

class DuergarLogicTest {
	@Test
	void renegadesSpawnOnlyInFullDarkness() {
		assertTrue(DuergarLogic.darkEnough(0, 0));
		assertFalse(DuergarLogic.darkEnough(1, 0));
		assertFalse(DuergarLogic.darkEnough(0, 4));
		assertFalse(DuergarLogic.darkEnough(14, 0));
	}

	@Test
	void squadOfFourOrFive() {
		DuergarRenegadeSettings settings = DuergarRenegadeSettings.DEFAULT;
		assertEquals(3, DuergarLogic.followerCount(settings.minFollowers(), settings.maxFollowers(), 0.0));
		assertEquals(3, DuergarLogic.followerCount(settings.minFollowers(), settings.maxFollowers(), 0.49));
		assertEquals(4, DuergarLogic.followerCount(settings.minFollowers(), settings.maxFollowers(), 0.5));
		assertEquals(4, DuergarLogic.followerCount(settings.minFollowers(), settings.maxFollowers(), 0.999));
		assertEquals(2, DuergarLogic.followerCount(2, 2, 0.7));
	}

	@Test
	void renegadesWearArmorInEverySlot() {
		Random random = new Random(3);
		for (int i = 0; i < 500; i++) {
			int gear = DuergarGear.armored(random::nextDouble, false, 0.5);
			assertEquals(5, DuergarGear.armorPieces(gear));
			assertTrue(DuergarGear.piece(gear, Slot.BELT) != 0);
			assertTrue(DuergarGear.piece(gear, Slot.BEARD) != 0);
			// No torn mail, holed breastplate or miner's leather: those are the dead miners'.
			int chest = DuergarGear.piece(gear, Slot.CHEST);
			assertTrue(chest == 4 || chest == 5, "chest " + chest);
		}
	}

	@Test
	void theLeaderAlwaysWearsHisOwnSet() {
		Random random = new Random(5);
		int first = DuergarGear.armored(random::nextDouble, true, 0.0);
		for (int i = 0; i < 50; i++) {
			int gear = DuergarGear.armored(random::nextDouble, true, 0.0);
			for (Slot slot : DuergarGear.ARMOR_SLOTS) {
				assertEquals(DuergarGear.piece(first, slot), DuergarGear.piece(gear, slot));
			}
		}
		assertEquals(6, DuergarGear.piece(first, Slot.HEAD));
	}

	@Test
	void levelsUpAtVillagerThresholds() {
		assertFalse(DuergarLogic.levelsUp(1, 5, 9));
		assertTrue(DuergarLogic.levelsUp(1, 5, 10));
		assertTrue(DuergarLogic.levelsUp(4, 5, 250));
		assertFalse(DuergarLogic.levelsUp(5, 5, 10_000));
		assertFalse(DuergarLogic.levelsUp(1, 1, 10_000), "the elder never levels");
	}

	@Test
	void templateRolesTakeOneOfTheirProfessions() {
		for (DuergarVillager.Role role : DuergarVillager.Role.values()) {
			for (double roll = 0.0; roll < 1.0; roll += 0.01) {
				assertEquals(role, DuergarProfession.pick(role, roll).role());
			}
		}
		assertEquals(DuergarProfession.ELDER, DuergarProfession.pick(DuergarVillager.Role.ELDER, 0.99));
		assertEquals(DuergarProfession.MINER, DuergarProfession.pick(DuergarVillager.Role.MINER, 0.0));
		assertEquals(DuergarProfession.RUNECARVER, DuergarProfession.pick(DuergarVillager.Role.MINER, 0.999));
		assertEquals(DuergarProfession.TOOLSMITH, DuergarProfession.pick(DuergarVillager.Role.SMITH, 0.999));
	}
}
