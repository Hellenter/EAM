package io.github.hellenter.eam.world.mining;

/** Decides whether a block can be broken without a tool. Pure logic, no Minecraft state. */
public final class HandBreakingLogic {
	private HandBreakingLogic() {
	}

	/**
	 * @param original      vanilla break progress per tick
	 * @param holdingTool   the main hand item is a tool (has the {@code tool} component)
	 * @param handBreakable the block is in {@code #eam:hand_breakable}
	 * @param hardness      the block's destroy time; 0 means it breaks instantly (torches, flowers)
	 * @return the break progress per tick: unchanged, or 0 if the block cannot be broken by hand
	 */
	public static float destroyProgress(float original, boolean holdingTool, boolean handBreakable, float hardness) {
		if (holdingTool || handBreakable || hardness <= 0.0F) {
			return original;
		}
		return 0.0F;
	}
}
