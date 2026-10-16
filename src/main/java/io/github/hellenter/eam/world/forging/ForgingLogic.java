package io.github.hellenter.eam.world.forging;

/** Pure rules for hot iron, mithril and adamantium: when they cool and what the anvil makes of them. */
public final class ForgingLogic {
	/** Hot iron turns back into an ingot this long after it leaves the furnace: one minute. */
	public static final int COOL_TICKS = 60 * 20;
	/** Marks hot iron still in the furnace output: its cooling has not started. */
	public static final long NOT_STARTED = Long.MAX_VALUE;
	/** Hot iron (or hot mithril) taken from each anvil input slot for one plate: one left, one right. */
	public static final int HOT_IRON_PER_SLOT = 1;
	/** Ticks the anvil takes for one plate without a hammer: five seconds. */
	public static final int FORGE_TICKS = 5 * 20;
	/** Iron plates are forged only on an anvil of this tier or better: iron. */
	public static final int PLATE_ANVIL_TIER = 3;
	/** Mithril plates are forged only on the mithril anvil. */
	public static final int MITHRIL_PLATE_ANVIL_TIER = 4;
	/** Adamantium plates are forged only on the adamantium anvil. */
	public static final int ADAMANTIUM_PLATE_ANVIL_TIER = 5;
	/** Speed a hammer adds to forging, by hammer tier (first entry is tier 1): copper and silver +25%, iron +50%, mithril +75%,
	 * adamantium +100%. */
	public static final float[] HAMMER_SPEED_BONUS = {0.15F, 0.25F, 0.5F, 0.75F, 1.0F};

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
	 * Whether the anvil can forge a plate from the same hot metal in both input slots: at least one ingot in each and
	 * neither cooled.
	 */
	public static boolean canForge(int left, int right, boolean cooled) {
		return !cooled && left >= HOT_IRON_PER_SLOT && right >= HOT_IRON_PER_SLOT;
	}

	/** Whether an anvil of {@code anvilTier} can forge plates that need {@code plateTier}. */
	public static boolean canForgeOn(int anvilTier, int plateTier) {
		return anvilTier >= plateTier;
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
