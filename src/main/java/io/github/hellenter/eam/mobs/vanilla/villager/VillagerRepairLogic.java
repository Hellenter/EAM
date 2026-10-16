package io.github.hellenter.eam.mobs.vanilla.villager;

/** Pure repair rules, unit tested. */
public final class VillagerRepairLogic {
	private VillagerRepairLogic() {
	}

	/**
	 * Whether a villager is served: villagers with worn gear take turns by a fixed order, and the masters of the village
	 * serve {@code clientsPerMaster} each.
	 *
	 * @param rank 0-based place of this villager among those in the village needing the same kind of repair
	 */
	public static boolean served(int rank, int masters, int clientsPerMaster) {
		return rank < (long) masters * clientsPerMaster;
	}

	/** Damage left on an item with {@code damage} out of {@code maxDamage} after one mending of {@code share}. */
	public static int mended(int damage, int maxDamage, double share) {
		int amount = Math.max(1, (int) Math.round(maxDamage * share));
		return Math.max(0, damage - amount);
	}

	/** Whether a villager checks his gear this tick: each one every {@code interval} ticks, spread by his id. */
	public static boolean checksNow(long gameTime, int id, int interval) {
		return Math.floorMod(gameTime + id, interval) == 0;
	}
}
