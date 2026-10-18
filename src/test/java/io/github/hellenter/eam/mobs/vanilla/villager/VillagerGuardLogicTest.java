package io.github.hellenter.eam.mobs.vanilla.villager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VillagerGuardLogicTest {
	@Test
	void weaponAndOnePieceOfArmorMakeAGuard() {
		assertTrue(VillagerGuardLogic.isGuard(true, true, 1, false));
	}

	@Test
	void guardProfessionNeedsOnlyAWeapon() {
		assertTrue(VillagerGuardLogic.isGuard(true, true, 0, true));
		assertFalse(VillagerGuardLogic.isGuard(true, false, 4, true));
	}

	@Test
	void onlyJoblessArmedVillagersBecomeGuards() {
		assertTrue(VillagerGuardLogic.becomesGuard(true, true, false));
		assertFalse(VillagerGuardLogic.becomesGuard(false, true, false));
		assertFalse(VillagerGuardLogic.becomesGuard(true, false, false));
	}

	@Test
	void dismissedGuardsStayJobless() {
		assertFalse(VillagerGuardLogic.becomesGuard(true, true, true));
	}

	@Test
	void noArmorOrNoWeaponIsNoGuard() {
		assertFalse(VillagerGuardLogic.isGuard(true, true, 0, false));
		assertFalse(VillagerGuardLogic.isGuard(true, false, 4, false));
	}

	@Test
	void childrenAreNeverGuards() {
		assertFalse(VillagerGuardLogic.isGuard(false, true, 4, true));
	}

	@Test
	void bowLetsGoOnceFullyDrawn() {
		assertFalse(VillagerGuardLogic.releases(19, 20));
		assertTrue(VillagerGuardLogic.releases(20, 20));
	}
}
