package io.github.hellenter.eam.mobs.vanilla.raid;

import java.util.List;
import java.util.function.IntUnaryOperator;

/** Pure village raid rules: the random raid calendar, reinforcements per wave, when raiders start to glow. */
public final class VillageRaidsLogic {
	public static final int DAY_LENGTH = 24_000;

	private VillageRaidsLogic() {
	}

	/** Day 1 is the first day of the world. */
	public static int dayNumber(long dayTime) {
		return (int) Math.floorDiv(dayTime, DAY_LENGTH) + 1;
	}

	/**
	 * The day the next random raid is due, {@code min} to {@code max} days after {@code lastDay}.
	 *
	 * @param random returns a number in {@code [0, bound)} for the bound passed in
	 */
	public static int nextDay(int lastDay, int min, int max, IntUnaryOperator random) {
		return lastDay + (max <= min ? min : min + random.applyAsInt(max - min + 1));
	}

	/** A random raid is due from its day on, until one starts. */
	public static boolean due(int today, int nextDay) {
		return nextDay > 0 && today >= nextDay;
	}

	/** How many of a kind join wave {@code wave} (1 is the first): the list's entry, or its last one for later waves. */
	public static int perWave(List<Integer> counts, int wave) {
		if (counts.isEmpty() || wave < 1) {
			return 0;
		}
		return counts.get(Math.min(wave, counts.size()) - 1);
	}

	/** Raiders glow once their wave has gone on for {@code glowTicks}. */
	public static boolean glows(long waveStart, long now, int glowTicks) {
		return now - waveStart >= glowTicks;
	}
}
