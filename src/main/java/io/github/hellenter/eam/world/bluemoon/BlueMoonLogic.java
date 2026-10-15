package io.github.hellenter.eam.world.bluemoon;

import io.github.hellenter.eam.world.bloodmoon.BloodMoonLogic;

import java.util.function.IntUnaryOperator;

/** Pure blue moon rules: the calendar, which never shares a night with the blood moon, and the harvest bonus. */
public final class BlueMoonLogic {
	private BlueMoonLogic() {
	}

	/** The day of the first blue moon of a world. */
	public static int firstDay(BlueMoonSettings settings, IntUnaryOperator random) {
		return BloodMoonLogic.between(settings.firstMinDay(), settings.firstMaxDay(), random);
	}

	/** The next blue moon after the one scheduled for {@code scheduled}, never on {@code today} or earlier. */
	public static int nextDay(int scheduled, int today, BlueMoonSettings settings, IntUnaryOperator random) {
		int next = scheduled + BloodMoonLogic.between(settings.intervalMinDays(), settings.intervalMaxDays(), random);
		return next > today ? next : today + BloodMoonLogic.between(settings.intervalMinDays(), settings.intervalMaxDays(), random);
	}

	/** A blue moon that would fall on the blood moon's day moves to the next day. */
	public static int avoid(int blueDay, int bloodDay) {
		return blueDay != 0 && blueDay == bloodDay ? blueDay + 1 : blueDay;
	}

	/**
	 * How many items a stack of {@code count} becomes: {@code count * multiplier}, the fraction kept with its odds,
	 * so one wheat under a 1.5 multiplier is two wheat half the time.
	 *
	 * @param roll a number in {@code [0, 1)}
	 */
	public static int harvestCount(int count, double multiplier, double roll) {
		double scaled = count * multiplier;
		int whole = (int) Math.floor(scaled);
		return whole + (roll < scaled - whole ? 1 : 0);
	}
}
