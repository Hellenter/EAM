package io.github.hellenter.eam.mobs.vanilla.villager;

/** Pure food bank rules, unit tested. */
public final class VillagerFoodLogic {
	private VillagerFoodLogic() {
	}

	/** How many of {@code added} feed items a player gave a farmer go to the livestock, rounded down. */
	public static int livestockShare(int added, double share) {
		return added <= 0 ? 0 : (int) Math.floor(added * share);
	}

	/** What is left of a farmer's {@code bank} when he holds {@code points} of food: never more than he has. */
	public static int bankLeft(int bank, int points) {
		return Math.max(0, Math.min(bank, points));
	}
}
