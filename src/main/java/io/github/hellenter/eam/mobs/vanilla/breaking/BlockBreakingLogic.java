package io.github.hellenter.eam.mobs.vanilla.breaking;

/** Break times and crack stages for blocks mobs break. Pure logic, no Minecraft state. */
public final class BlockBreakingLogic {
	private BlockBreakingLogic() {
	}

	/**
	 * @param hardness         the block's destroy time
	 * @param handBreakable    a player could break it bare-handed ({@code #eam:hand_breakable})
	 * @param ticksPerHardness ticks a mob needs per point of hardness
	 * @return ticks one mob needs to break the block, or -1 if mobs cannot break it
	 */
	public static int breakTicks(float hardness, boolean handBreakable, int ticksPerHardness) {
		if (hardness < 0.0F || !handBreakable && hardness > 0.0F) {
			return -1;
		}
		return Math.max(1, Math.round(hardness * ticksPerHardness));
	}

	/** Crack overlay stage 0..9 for {@code done} of {@code total} break ticks. */
	public static int stage(float done, int total) {
		return Math.clamp((int) (done / total * 10.0F), 0, 9);
	}

	/** Break ticks left after a block was left alone for {@code idleTicks}; damage heals after a delay. */
	public static float decay(float done, long idleTicks, int delayTicks, float perTick, int elapsed) {
		if (idleTicks <= delayTicks) {
			return done;
		}
		return Math.max(0.0F, done - perTick * elapsed);
	}
}
