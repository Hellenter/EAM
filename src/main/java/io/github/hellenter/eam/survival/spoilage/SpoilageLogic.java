package io.github.hellenter.eam.survival.spoilage;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure spoilage rules. A perishable stack keeps one spoil time per item, oldest first, so food picked on different days
 * can share a stack and still rot one item at a time. Times are overworld clock ticks: sleeping through the night ages
 * food like the day it skips.
 */
public final class SpoilageLogic {
	/** Ticks in a Minecraft day. */
	public static final long DAY = 24000L;
	/** Ticks in an in-game hour. */
	public static final long HOUR = DAY / 24;
	/**
	 * The time of an item that joined a stack before the server stamped it: fresh food just dropped, crafted or cooked.
	 * The server turns it into a real time on its next look; until then it sorts last and never spoils.
	 */
	public static final long UNSTAMPED = Long.MAX_VALUE;

	private SpoilageLogic() {
	}

	/** Ticks a food lasts for a datapack's {@code days}. */
	public static long lifetime(double days) {
		return Math.max(1L, Math.round(days * DAY));
	}

	/**
	 * Spoil times matched to a stack of {@code count}: extra times drop the oldest, missing times copy the oldest one (a
	 * merge nothing tracked never makes food fresher), or are {@code fresh} when there are none at all.
	 */
	public static List<Long> fit(List<Long> times, int count, long fresh) {
		int size = times.size();
		if (size == count) return times;
		if (size > count) return List.copyOf(times.subList(size - count, size));
		List<Long> fitted = new ArrayList<>(times);
		long pad = times.isEmpty() ? fresh : times.getFirst();
		while (fitted.size() < count) fitted.add(pad);
		fitted.sort(null);
		return List.copyOf(fitted);
	}

	/** The {@code n} oldest times. */
	public static List<Long> oldest(List<Long> times, int n) {
		return List.copyOf(times.subList(0, Math.min(n, times.size())));
	}

	/**
	 * The times of {@code n} items moved out of a stack: its oldest, and {@link #UNSTAMPED} for items it has no time for
	 * yet, as a stack the server has not stamped is fresh.
	 */
	public static List<Long> take(List<Long> times, int n) {
		List<Long> taken = new ArrayList<>(oldest(times, n));
		while (taken.size() < n) taken.add(UNSTAMPED);
		return List.copyOf(taken);
	}

	/** Unstamped items get {@code fresh}, the time of food stamped now. */
	public static List<Long> stamp(List<Long> times, long fresh) {
		if (times.isEmpty() || times.getLast() != UNSTAMPED) return times;
		List<Long> stamped = new ArrayList<>(times.size());
		for (long time : times) stamped.add(time == UNSTAMPED ? fresh : time);
		stamped.sort(null);
		return List.copyOf(stamped);
	}

	/** The times left once the {@code n} oldest are gone: eaten, moved away or spoiled. */
	public static List<Long> withoutOldest(List<Long> times, int n) {
		return List.copyOf(times.subList(Math.min(n, times.size()), times.size()));
	}

	/** Both stacks' times in one, oldest first. */
	public static List<Long> merge(List<Long> a, List<Long> b) {
		List<Long> merged = new ArrayList<>(a.size() + b.size());
		merged.addAll(a);
		merged.addAll(b);
		merged.sort(null);
		return List.copyOf(merged);
	}

	/** How many items have spoiled by {@code now}. */
	public static int spoiled(List<Long> times, long now) {
		int count = 0;
		for (long time : times) {
			if (time > now) break;
			count++;
		}
		return count;
	}

	/** Every time put off by {@code ticks}: the food spent them in the cold. */
	public static List<Long> delay(List<Long> times, long ticks) {
		if (ticks <= 0) return times;
		List<Long> delayed = new ArrayList<>(times.size());
		for (long time : times) delayed.add(time == UNSTAMPED ? time : time + ticks);
		return List.copyOf(delayed);
	}

	/**
	 * Spoil times of food taken off a dryer: the time each item had left grows {@code multiplier} times, but never past
	 * what freshly dried food keeps ({@code lifetime} times {@code multiplier}). Unstamped items count as fresh.
	 */
	public static List<Long> dried(List<Long> times, long now, long lifetime, double multiplier) {
		long longest = Math.round(lifetime * multiplier);
		List<Long> dried = new ArrayList<>(times.size());
		for (long time : times) {
			long left = time == UNSTAMPED ? lifetime : Math.max(0L, time - now);
			dried.add(now + Math.min(longest, Math.round(left * multiplier)));
		}
		dried.sort(null);
		return List.copyOf(dried);
	}

	/** A group of items in one stack that spoil within the same tenth of a day. */
	public record Batch(int count, long ticksLeft) {
	}

	/** The stack's items grouped by time left, soonest first, for the tooltip. */
	public static List<Batch> batches(List<Long> times, long now) {
		List<Batch> batches = new ArrayList<>();
		for (long time : times) {
			if (time == UNSTAMPED) continue;
			long left = Math.max(0L, time - now);
			if (!batches.isEmpty() && tenths(batches.getLast().ticksLeft()) == tenths(left)) {
				Batch last = batches.removeLast();
				batches.add(new Batch(last.count() + 1, last.ticksLeft()));
			} else {
				batches.add(new Batch(1, left));
			}
		}
		return batches;
	}

	/** Tenths of a day, rounded up: what the tooltip shows. */
	public static long tenths(long ticks) {
		return (ticks + DAY / 10 - 1) / (DAY / 10);
	}

	/**
	 * How long a fridge stays cold over {@code elapsed} ticks. It first uses up the cold left from its last ice, then melts
	 * one ice item at a time, each worth {@code ticksPerIce}, for as long as it has ice.
	 *
	 * @param charge cold left from the ice melting now
	 * @param ice    ice items in its ice slots
	 */
	public static Cooling cool(long elapsed, long charge, int ice, long ticksPerIce) {
		long cooled = 0;
		int melted = 0;
		long left = Math.max(0L, elapsed);
		while (left > 0) {
			if (charge <= 0) {
				if (melted >= ice) break;
				melted++;
				charge = ticksPerIce;
			}
			long used = Math.min(charge, left);
			charge -= used;
			left -= used;
			cooled += used;
		}
		return new Cooling(cooled, Math.max(0L, charge), melted);
	}

	/**
	 * @param cooled ticks the fridge was cold; its food did not age in them
	 * @param charge cold left from the ice melting now
	 * @param melted ice items used up
	 */
	public record Cooling(long cooled, long charge, int melted) {
	}
}
