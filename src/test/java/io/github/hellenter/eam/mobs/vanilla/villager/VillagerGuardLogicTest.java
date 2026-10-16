package io.github.hellenter.eam.mobs.vanilla.villager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VillagerGuardLogicTest {
	@Test
	void weaponAndOnePieceOfArmorMakeAGuard() {
		assertTrue(VillagerGuardLogic.isGuard(true, true, 1));
	}

	@Test
	void noArmorOrNoWeaponIsNoGuard() {
		assertFalse(VillagerGuardLogic.isGuard(true, true, 0));
		assertFalse(VillagerGuardLogic.isGuard(true, false, 4));
	}

	@Test
	void childrenAreNeverGuards() {
		assertFalse(VillagerGuardLogic.isGuard(false, true, 4));
	}

	@Test
	void bowLetsGoOnceFullyDrawn() {
		assertFalse(VillagerGuardLogic.releases(19, 20));
		assertTrue(VillagerGuardLogic.releases(20, 20));
	}
}
