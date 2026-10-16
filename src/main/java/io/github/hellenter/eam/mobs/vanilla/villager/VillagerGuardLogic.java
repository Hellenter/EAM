package io.github.hellenter.eam.mobs.vanilla.villager;

/** Pure guard rules, unit tested. */
public final class VillagerGuardLogic {
	private VillagerGuardLogic() {
	}

	/** A grown villager with a weapon in his main hand and at least one piece of armor on is a guard. */
	public static boolean isGuard(boolean adult, boolean weaponInHand, int armorPieces) {
		return adult && weaponInHand && armorPieces > 0;
	}

	/** Whether a guard drawing his bow for {@code drawnTicks} lets the arrow go. */
	public static boolean releases(int drawnTicks, int drawTicks) {
		return drawnTicks >= drawTicks;
	}
}
