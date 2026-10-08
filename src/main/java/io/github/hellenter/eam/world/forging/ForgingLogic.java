package io.github.hellenter.eam.world.forging;

/** Pure rules for hot iron: when it cools and what the anvil makes of it. */
public final class ForgingLogic {
	/** Hot iron turns back into an ingot this long after it leaves the furnace: one minute. */
	public static final int COOL_TICKS = 60 * 20;
	/** Marks hot iron still in the furnace output: its cooling has not started. */
	public static final long NOT_STARTED = Long.MAX_VALUE;

	private ForgingLogic() {
	}

	/** Game time at which hot iron taken out at {@code now} turns back into an ingot. */
	public static long coolsAt(long now) {
		return now + COOL_TICKS;
	}

	/** Hot iron whose timer has not started is still hot. */
	public static boolean isCooled(long coolsAt, long now) {
		return coolsAt != NOT_STARTED && now >= coolsAt;
	}

	/** Whole seconds left before it cools, rounded up; 0 once cooled. */
	public static int secondsLeft(long coolsAt, long now) {
		if (coolsAt == NOT_STARTED) {
			return COOL_TICKS / 20;
		}
		return (int) Math.max(0, (coolsAt - now + 19) / 20);
	}

	/**
	 * Iron plates the anvil forges: one per hot ingot in the left slot, nothing when the right slot holds anything or
	 * the iron has cooled.
	 */
	public static int plates(int hotIron, boolean additionEmpty, boolean cooled) {
		return additionEmpty && !cooled ? hotIron : 0;
	}
}
