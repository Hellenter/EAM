package io.github.hellenter.eam.world.forging;

/** Pure rules for hot iron: when it cools and what the anvil makes of it. */
public final class ForgingLogic {
	/** Hot iron turns back into an ingot this long after it leaves the furnace: one minute. */
	public static final int COOL_TICKS = 60 * 20;
	/** Marks hot iron still in the furnace output: its cooling has not started. */
	public static final long NOT_STARTED = Long.MAX_VALUE;
	/** Hot iron hammered into one iron plate. */
	public static final int HOT_IRON_PER_PLATE = 2;
	/** Ticks the anvil takes for one plate without a hammer: five seconds. */
	public static final int FORGE_TICKS = 5 * 20;
	/** Plates are forged only on an anvil of this tier: iron. */
	public static final int PLATE_ANVIL_TIER = 3;
	/** Speed a hammer adds to forging, by hammer tier (first entry is tier 1): copper and silver +25%, iron +50%. */
	public static final float[] HAMMER_SPEED_BONUS = {0.15F, 0.25F, 0.5F};

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

	/** An anvil works on items of its own tier and below: a copper anvil repairs flint and copper gear, not iron. */
	public static boolean canWorkOn(int anvilTier, int itemTier) {
		return itemTier <= anvilTier;
	}

	/** Ticks one plate takes with a hammer of {@code hammerTier} in the anvil; 0 is no hammer. */
	public static int forgeTicks(int hammerTier) {
		float bonus = hammerTier > 0 && hammerTier <= HAMMER_SPEED_BONUS.length ? HAMMER_SPEED_BONUS[hammerTier - 1] : 0.0F;
		return Math.round(FORGE_TICKS / (1.0F + bonus));
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
