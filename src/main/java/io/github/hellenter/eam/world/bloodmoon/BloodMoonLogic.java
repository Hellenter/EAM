package io.github.hellenter.eam.world.bloodmoon;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.IntUnaryOperator;

/** Pure blood moon rules: the calendar, which armor pieces a mob wears, weighted picks. */
public final class BloodMoonLogic {
	public static final int DAY_LENGTH = 24_000;

	/** Armor pieces in the order they are handed out: a chestplate first, then a helmet, then the rest. */
	public enum Piece {
		CHESTPLATE, HELMET, LEGGINGS, BOOTS
	}

	private BloodMoonLogic() {
	}

	/** Day 1 is the first day of the world. */
	public static int dayNumber(long dayTime) {
		return (int) Math.floorDiv(dayTime, DAY_LENGTH) + 1;
	}

	/** The event night of a day starts at {@code startTime} ticks into it and lasts until the next day begins. */
	public static boolean isEventNight(long dayTime, int startTime) {
		return Math.floorMod(dayTime, DAY_LENGTH) >= startTime;
	}

	/**
	 * @param random returns a number in {@code [0, bound)} for the bound passed in
	 * @return a number in {@code [min, max]}
	 */
	public static int between(int min, int max, IntUnaryOperator random) {
		return max <= min ? min : min + random.applyAsInt(max - min + 1);
	}

	/** The day of the first blood moon of a world. */
	public static int firstDay(BloodMoonSettings settings, IntUnaryOperator random) {
		return between(settings.firstMinDay(), settings.firstMaxDay(), random);
	}

	/** The next blood moon after the one scheduled for {@code scheduled}, never on {@code today} or earlier. */
	public static int nextDay(int scheduled, int today, BloodMoonSettings settings, IntUnaryOperator random) {
		int next = scheduled + between(settings.intervalMinDays(), settings.intervalMaxDays(), random);
		return next > today ? next : today + between(settings.intervalMinDays(), settings.intervalMaxDays(), random);
	}

	public static boolean active(int scheduledDay, int forcedDay, long dayTime, int startTime) {
		int today = dayNumber(dayTime);
		return forcedDay == today || scheduledDay == today && isEventNight(dayTime, startTime);
	}

	/**
	 * 1 piece is always a chestplate, 2 add a helmet, 3 add leggings or boots, 4 is the full set.
	 *
	 * @param random returns a number in {@code [0, bound)} for the bound passed in
	 */
	public static List<Piece> armorPieces(int count, IntUnaryOperator random) {
		List<Piece> pieces = new ArrayList<>(List.of(Piece.values()).subList(0, Math.clamp(count, 0, 4)));
		if (count == 3) {
			pieces.set(2, random.applyAsInt(2) == 0 ? Piece.LEGGINGS : Piece.BOOTS);
		}
		return pieces;
	}

	/**
	 * @param random returns a number in {@code [0, bound)} for the bound passed in
	 * @return a key picked with the odds of its weight, or null if no weight is positive
	 */
	public static <K> K pick(Map<K, Integer> weights, IntUnaryOperator random) {
		int total = 0;
		for (int weight : weights.values()) {
			total += Math.max(weight, 0);
		}
		if (total <= 0) {
			return null;
		}
		int roll = random.applyAsInt(total);
		for (Map.Entry<K, Integer> entry : weights.entrySet()) {
			roll -= Math.max(entry.getValue(), 0);
			if (roll < 0) {
				return entry.getKey();
			}
		}
		return null;
	}

	/** Damage a hostile mob deals during the event. */
	public static float damage(float amount, double multiplier) {
		return (float) (amount * multiplier);
	}
}
