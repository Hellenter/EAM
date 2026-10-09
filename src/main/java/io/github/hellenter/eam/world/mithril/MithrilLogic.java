package io.github.hellenter.eam.world.mithril;

/** Pure rules for obsidian and the obsidian blast furnace; {@link MithrilModule} feeds them the block and the furnace. */
public final class MithrilLogic {
	/** Obsidian's hardness, down from vanilla's 50: an iron pickaxe takes about 15 s with the slower breaking. */
	public static final float OBSIDIAN_HARDNESS = 15.0F;

	private MithrilLogic() {
	}

	/** Hardness of a block: obsidian gets {@link #OBSIDIAN_HARDNESS}, everything else keeps its own. */
	public static float destroySpeed(boolean obsidian, float original) {
		return obsidian ? OBSIDIAN_HARDNESS : original;
	}

	/**
	 * Whether a furnace may cook its input.
	 *
	 * @param obsidianOnly     the input is smelted only in the obsidian blast furnace (mithril)
	 * @param obsidianFurnace  the furnace is the obsidian blast furnace
	 */
	public static boolean mayCook(boolean obsidianOnly, boolean obsidianFurnace) {
		return !obsidianOnly || obsidianFurnace;
	}
}
