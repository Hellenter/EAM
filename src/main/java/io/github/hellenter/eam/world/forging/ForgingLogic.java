package io.github.hellenter.eam.world.forging;

/** Pure rules for hot iron: when it cools and what the anvil makes of it. */
public final class ForgingLogic {
	/** Hot iron turns back into an ingot this long after it leaves the furnace: one minute. */
	public static final int COOL_TICKS = 60 * 20;
	/** Marks hot iron still in the furnace output: its cooling has not started. */
	public static final long NOT_STARTED = Long.MAX_VALUE;
	/** Hot iron hammered into one iron plate. */
	public static final int HOT_IRON_PER_PLATE = 2;
	/** Ticks the anvil takes for one plate: five seconds. */
	public static final int FORGE_TICKS = 5 * 20;

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
	 * Whether the anvil can forge a plate from the hot iron in its left slot: enough of it, still hot, and nothing in
	 * the right slot.
	 */
	public static boolean canForge(int hotIron, boolean additionEmpty, boolean cooled) {
		return additionEmpty && !cooled && hotIron >= HOT_IRON_PER_PLATE;
	}

	/** A plate is done once it has been hammered for {@code total} ticks; a plate with no time is done at once. */
	public static boolean forged(long now, long start, int total) {
		return now - start >= total;
	}

	/** Ticks hammered so far, never above {@code total}. */
	public static int progress(long now, long start, int total) {
		return (int) Math.clamp(now - start, 0, total);
	}
}
