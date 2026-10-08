package io.github.hellenter.eam.world.mining;

/**
 * Players break blocks {@link #TIME_MULTIPLIER} times slower, and nothing breaks faster than
 * {@link #MIN_BREAK_TICKS}. These are constants, not data: the client predicts break progress
 * with the same numbers, and a mismatch with the server makes broken blocks reappear.
 */
public final class BreakTimeLogic {
	public static final float TIME_MULTIPLIER = 4.0F;
	/** 0.5 s: blocks that used to break instantly (flowers, torches, ground items) take this long. */
	public static final int MIN_BREAK_TICKS = 10;

	private BreakTimeLogic() {
	}

	/** @param progress break progress per tick (1 = broken); infinite for blocks that break instantly */
	public static float destroyProgress(float progress) {
		return Math.min(progress / TIME_MULTIPLIER, 1.0F / MIN_BREAK_TICKS);
	}
}
