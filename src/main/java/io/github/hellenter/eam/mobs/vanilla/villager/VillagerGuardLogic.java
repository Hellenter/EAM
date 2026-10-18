package io.github.hellenter.eam.mobs.vanilla.villager;

/** Pure guard rules, unit tested. */
public final class VillagerGuardLogic {
	private VillagerGuardLogic() {
	}

	/**
	 * A grown villager with a weapon in his main hand fights as a guard: one of the guard profession with the weapon
	 * alone, any other with at least one piece of armor on too.
	 */
	public static boolean isGuard(boolean adult, boolean weaponInHand, int armorPieces, boolean guardProfession) {
		return adult && weaponInHand && (armorPieces > 0 || guardProfession);
	}

	/**
	 * A jobless villager armed as a guard takes the guard profession instead of a job, unless a player dismissed him
	 * from it and he has not had a job since.
	 */
	public static boolean becomesGuard(boolean jobless, boolean armedAsGuard, boolean dismissed) {
		return jobless && armedAsGuard && !dismissed;
	}

	/** Whether a guard drawing his bow for {@code drawnTicks} lets the arrow go. */
	public static boolean releases(int drawnTicks, int drawTicks) {
		return drawnTicks >= drawTicks;
	}
}
