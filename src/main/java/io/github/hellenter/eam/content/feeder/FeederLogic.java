package io.github.hellenter.eam.content.feeder;

import org.jspecify.annotations.Nullable;

/** Pure feeding math, unit tested. */
public final class FeederLogic {
	/** Ticks in a day. */
	public static final long DAY = 24000L;
	/** Most feeder blocks joined into one trough. */
	public static final int MAX_LENGTH = 3;
	/** Steps of the food heap model in a feeder; 0 is empty. */
	public static final int MAX_FILL = 3;

	private FeederLogic() {
	}

	public static long day(long clockTime) {
		return Math.floorDiv(clockTime, DAY);
	}

	/** Whether an animal may eat today: one meal a day. A wild animal (no state yet) always may. */
	public static boolean canEat(@Nullable LivestockState state, long today) {
		return state == null || today > state.lastMealDay();
	}

	/** Days the animal has gone without a meal since its last one, today not counted: it can still eat today. */
	public static int missedDays(LivestockState state, long today) {
		return (int) Math.clamp(today - state.lastMealDay() - 1, 0, Integer.MAX_VALUE);
	}

	/** Whether the animal is thin today, also counting the days missed since its last meal. */
	public static boolean isThin(@Nullable LivestockState state, long today, int thinAfterDays) {
		if (state == null) return false;
		return state.thin() || state.hunger() + missedDays(state, today) > thinAfterDays;
	}

	/** The state after a meal today: missed days add up, the meal takes one off, and an animal fattens up at 0. */
	public static LivestockState eat(@Nullable LivestockState state, long today, int thinAfterDays) {
		if (state == null) return new LivestockState(today, 0, false);
		int hunger = state.hunger() + missedDays(state, today);
		boolean thin = state.thin() || hunger > thinAfterDays;
		hunger = Math.max(0, hunger - 1);
		return new LivestockState(today, hunger, thin && hunger > 0);
	}

	/** Food heap step for the model: 0 when empty, otherwise 1 to {@link #MAX_FILL}. */
	public static int fill(int food, int capacity) {
		if (food <= 0) return 0;
		return (int) Math.clamp((long) Math.ceil(food * (double) MAX_FILL / capacity), 1, MAX_FILL);
	}

	/**
	 * Which sides a feeder placed between runs of {@code before} and {@code after} feeders joins, so a trough stays at
	 * most {@link #MAX_LENGTH} long. Joins the side before first. Returns {@code {joinBefore, joinAfter}}.
	 */
	public static boolean[] join(int before, int after) {
		boolean joinBefore = before > 0 && before + 1 <= MAX_LENGTH;
		boolean joinAfter = after > 0 && (joinBefore ? before : 0) + 1 + after <= MAX_LENGTH;
		return new boolean[] {joinBefore, joinAfter};
	}

	/** How many of {@code count} crops worth {@code points} each a farmer gives while keeping {@code keep} of his {@code total} points. */
	public static int farmerGives(int count, int points, int total, int keep) {
		if (points <= 0) return count;
		int surplus = total - keep;
		if (surplus <= 0) return 0;
		return Math.min(count, surplus / points);
	}

	/** Whether the animals around a trough of {@code length} feeders leave room for one more pair's young. */
	public static boolean roomToBreed(int animals, int length, int headsPerFeeder) {
		return animals < length * headsPerFeeder;
	}
}
