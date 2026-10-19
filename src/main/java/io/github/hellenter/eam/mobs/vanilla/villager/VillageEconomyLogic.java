package io.github.hellenter.eam.mobs.vanilla.villager;

import java.util.Map;

/** Pure village economy rules, unit tested. */
public final class VillageEconomyLogic {
	private VillageEconomyLogic() {
	}

	/**
	 * What one villager brings in half a day, an average of his work rather than counted piece by piece: his trade's
	 * base amount, plus a share of the farmland around his job site for a farmer, times his tool's tier, his level and the
	 * components he used up.
	 *
	 * @param farmland     farmland blocks this villager tends (his share, when farmers share the fields)
	 * @param toolRate     the multiplier of the tool in his hand, 1 with none
	 * @param level        his trade level, 1 to 5
	 * @param components   components he used up this time
	 */
	public static double yield(double base, double perFarmland, double farmland, double toolRate, int level, double levelBonus,
			int components, double componentBonus) {
		double work = base + perFarmland * farmland;
		return work * toolRate * (1.0 + levelBonus * (Math.max(1, level) - 1)) * (1.0 + componentBonus * components);
	}

	/** Guards a village of {@code adults} grown villagers needs: one for each full {@code perGuard}. */
	public static int guardsNeeded(int adults, int perGuard) {
		return perGuard <= 0 ? 0 : adults / perGuard;
	}

	/** Iron golems a village of {@code villagers} may have: one, and one more for each full {@code perGolem} past the first. */
	public static int golemsAllowed(int villagers, int perGolem) {
		return perGolem <= 0 ? 1 : Math.max(1, villagers / perGolem);
	}

	/** Whether {@code stock} covers {@code cost}. */
	public static boolean affords(Map<VillageResource, Double> stock, Map<VillageResource, Double> cost) {
		for (Map.Entry<VillageResource, Double> entry : cost.entrySet()) {
			if (stock.getOrDefault(entry.getKey(), 0.0) + 1.0E-6 < entry.getValue()) return false;
		}
		return true;
	}

	/** Whether villagers may have children: a free bed for the child, and food in store for him. */
	public static boolean canBreed(int villagers, int beds, double provisions, double breedCost) {
		return villagers < beds && provisions >= breedCost;
	}

	/**
	 * How readily a villager is taken as a new guard, lower first: nitwits, then the jobless, then one of the trade with
	 * the most villagers, the least experienced of them. {@link Long#MAX_VALUE} when he is not taken at all: an elder,
	 * a guard, one a player dismissed, or the last of his trade.
	 *
	 * @param sameTrade villagers of his trade in the village, him included
	 */
	public static long guardRank(boolean nitwit, boolean jobless, boolean excluded, int sameTrade, int xp) {
		if (excluded) return Long.MAX_VALUE;
		if (nitwit) return 0;
		if (jobless) return 1;
		if (sameTrade < 2) return Long.MAX_VALUE;
		// More of a trade first, then less experience.
		return 2 + (long) (1000 - Math.min(999, sameTrade)) * 1_000_000L + Math.min(999_999, xp);
	}

	/**
	 * The inventory slots a player gives from, in order: the main inventory first, so what is at hand on the hotbar goes
	 * last.
	 *
	 * @param size    slots of the inventory, the hotbar included
	 * @param hotbar  hotbar slots, the first of the inventory
	 */
	public static int[] donationSlots(int size, int hotbar) {
		int[] slots = new int[size];
		for (int i = 0; i < size; i++) slots[i] = (i + hotbar) % size;
		return slots;
	}
}
