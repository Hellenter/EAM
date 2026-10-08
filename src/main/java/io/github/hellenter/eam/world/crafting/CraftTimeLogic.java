package io.github.hellenter.eam.world.crafting;

/** Pure arithmetic for craft times. */
public final class CraftTimeLogic {
	public static final int TICKS_PER_SECOND = 20;
	/** Menu data is synced as a short, so no craft may take longer than this. */
	public static final int MAX_TICKS = Short.MAX_VALUE;

	private CraftTimeLogic() {
	}

	/**
	 * Ticks one craft takes.
	 *
	 * @param seconds       seconds from the matching rule (per ingredient when {@code perIngredient})
	 * @param perIngredient whether the time is paid for every item in the grid, as for armor
	 * @param ingredients   items in the crafting grid
	 * @param multiplier    1 in the inventory grid, the crafting table multiplier on a table
	 */
	public static int ticks(float seconds, boolean perIngredient, int ingredients, float multiplier) {
		float total = perIngredient ? seconds * ingredients : seconds;
		return Math.clamp(Math.round(total * multiplier * TICKS_PER_SECOND), 0, MAX_TICKS);
	}

	/** Ticks crafted so far, never above {@code total}. */
	public static int progress(long now, long start, int total) {
		return (int) Math.clamp(now - start, 0, total);
	}

	/** The result can be taken once the craft has run its full time; a craft with no time is always ready. */
	public static boolean ready(int progress, int total) {
		return progress >= total;
	}
}
