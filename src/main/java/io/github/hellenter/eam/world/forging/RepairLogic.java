package io.github.hellenter.eam.world.forging;

/**
 * Pure rules for repair on the anvil. Metal gear is mended with nuggets: as many nuggets as the item's recipe takes ingots
 * (and plates) restore {@link #METAL_SHARE} of its durability, and an ingot counts as {@link #NUGGETS_PER_INGOT}
 * nuggets. Other gear keeps the vanilla share per repair item. Repair costs no experience.
 */
public final class RepairLogic {
	/** Share of durability restored by as many nuggets as the recipe takes ingots: a chestplate, 8 nuggets for 75%. */
	public static final float METAL_SHARE = 0.75F;
	public static final int NUGGETS_PER_INGOT = 9;
	/** Share one repair item restores on gear that is not metal, as in vanilla. */
	public static final float OTHER_SHARE = 0.25F;

	private RepairLogic() {
	}

	/** Durability one nugget restores on metal gear whose recipe takes {@code ingots} ingots. */
	public static float perNugget(int maxDamage, int ingots) {
		return maxDamage * METAL_SHARE / Math.max(1, ingots);
	}

	/**
	 * How many of {@code available} repair items are used up: just enough to mend {@code damage}, each restoring
	 * {@code perItem}.
	 */
	public static int itemsUsed(int damage, float perItem, int available) {
		if (damage <= 0 || perItem <= 0 || available <= 0) {
			return 0;
		}
		return (int) Math.min(available, (long) Math.ceil(damage / perItem - 1.0E-4));
	}

	/** Durability mended by {@code used} items restoring {@code perItem} each, never more than the damage. */
	public static int repaired(int damage, float perItem, int used) {
		return Math.min(damage, Math.round(used * perItem));
	}
}
