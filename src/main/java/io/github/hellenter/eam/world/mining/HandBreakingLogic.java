package io.github.hellenter.eam.world.mining;

/** Decides whether a block can be broken without the right tool. Pure logic, no Minecraft state. */
public final class HandBreakingLogic {
	private HandBreakingLogic() {
	}

	/**
	 * @param original      vanilla break progress per tick
	 * @param toolFits      the main hand item is a tool made for this block (a pickaxe on stone, an axe on logs)
	 * @param handBreakable the block is in {@code #eam:hand_breakable}
	 * @param hardness      the block's destroy time; 0 means it breaks instantly (torches, flowers)
	 * @param axeOnly       the block is in {@code #eam:axe_only} (logs)
	 * @param holdsAxe      the main hand item is in {@code #eam:chops_logs}
	 * @return the break progress per tick: unchanged, or 0 if the block cannot be broken with what is in hand
	 */
	public static float destroyProgress(float original, boolean toolFits, boolean handBreakable, float hardness, boolean axeOnly,
			boolean holdsAxe) {
		if (axeOnly) {
			return holdsAxe ? original : 0.0F;
		}
		if (toolFits || handBreakable || hardness <= 0.0F) {
			return original;
		}
		return 0.0F;
	}
}
