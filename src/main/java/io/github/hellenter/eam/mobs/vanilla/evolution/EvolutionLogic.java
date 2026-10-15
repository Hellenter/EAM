package io.github.hellenter.eam.mobs.vanilla.evolution;

import java.util.List;

/** Pure rules of monster evolution: which phase the world is in and what a roll of the dice hands out. */
public final class EvolutionLogic {
	/** Ticks in a Minecraft day. */
	public static final long DAY = 24000L;

	private EvolutionLogic() {
	}

	/** Whole days passed in a world whose clock reads {@code clockTime}: 0 on the first day. */
	public static long daysPassed(long clockTime) {
		return Math.floorDiv(clockTime, DAY);
	}

	/**
	 * The phase in force after {@code days} days: the last of {@code startDays} (sorted ascending) that has begun, or -1
	 * before the first.
	 */
	public static int phase(List<Integer> startDays, long days) {
		int phase = -1;
		for (int i = 0; i < startDays.size(); i++) {
			if (days >= startDays.get(i)) {
				phase = i;
			}
		}
		return phase;
	}

	/**
	 * One roll over outcomes that exclude each other, each with its own chance: the index of the outcome {@code roll}
	 * (0 to 1) lands in, or -1 for nothing. Chances add up, so 0.2 and 0.125 give 20% the first, 12.5% the second.
	 */
	public static int pick(List<Float> chances, float roll) {
		float upTo = 0.0F;
		for (int i = 0; i < chances.size(); i++) {
			upTo += chances.get(i);
			if (roll < upTo) {
				return i;
			}
		}
		return -1;
	}
}
