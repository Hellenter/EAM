package io.github.hellenter.eam.mobs.entity.lurker;

/** The swamp lurker's choices and its leap ballistics, free of game state so they can be unit tested. */
public final class SwampLurkerLogic {
	/** Gravity of a living entity, blocks per tick squared. */
	static final double GRAVITY = 0.08;
	/** Vertical speed kept each tick in the air. */
	static final double VERTICAL_DRAG = 0.98;
	/** Horizontal speed kept each tick in the air. */
	static final double AIR_DRAG = 0.91;
	/** The lowest arc of a leap, in blocks above the higher of its two ends. */
	private static final double MIN_ARC = 0.8;
	/** Longest flight simulated, in ticks. */
	private static final int MAX_TICKS = 80;
	/** How far over its highest leap it may rise with nothing under it before it is made to fall. */
	private static final double RISE_SLACK = 1.0;

	public enum Move {
		/** Three swipes at a target in reach. */
		COMBO,
		/** A leap at the target. */
		LEAP_IN,
		/** A leap away from the target. */
		LEAP_BACK,
		/** Run at the target on all fours. */
		APPROACH,
		/** Keep its distance, circling the target, until it may attack again. */
		PROWL
	}

	/** Launch speed of a leap: {@code horizontal} along the ground toward the landing point, {@code vertical} up. */
	public record Launch(double horizontal, double vertical) {
	}

	private SwampLurkerLogic() {
	}

	/**
	 * What to do next about a target.
	 *
	 * @param distance       flat distance to the target
	 * @param height         how far the target stands above it (negative below)
	 * @param attackCooldown ticks before it may attack again
	 * @param leapCooldown   ticks before it may leap again
	 * @param canLeap        whether it stands on the ground or swims, so it can push off
	 */
	public static Move choose(double distance, double height, int attackCooldown, int leapCooldown, boolean canLeap,
			SwampLurkerSettings settings) {
		SwampLurkerSettings.Leap leap = settings.leap();
		boolean leapReady = canLeap && leapCooldown <= 0;
		if (attackCooldown > 0) {
			return leapReady && distance < leap.retreatDistance() - 2.0 ? Move.LEAP_BACK : Move.PROWL;
		}
		if (distance <= settings.reach() && Math.abs(height) < 2.0) {
			return Move.COMBO;
		}
		if (leapReady && distance >= leap.minDistance() && distance <= leap.maxDistance() + settings.reach()
				&& height <= leap.maxHeight() - 0.5) {
			return Move.LEAP_IN;
		}
		return Move.APPROACH;
	}

	/**
	 * The launch that lands a leap {@code distance} away along the ground and {@code height} above its start, peaking
	 * {@code arc} blocks above the higher end (at most {@code maxHeight} above the start). Distance is capped at
	 * {@code maxDistance}; a landing point too high is not reached.
	 */
	public static Launch leap(double distance, double height, double arc, double maxHeight, double maxDistance) {
		double flat = Math.clamp(distance, 0.0, maxDistance);
		double apex = Math.clamp(Math.max(height, 0.0) + Math.max(arc, MIN_ARC), MIN_ARC, maxHeight);
		double vertical = launchSpeedFor(apex);
		int ticks = flightTicks(vertical, Math.min(height, apex - 0.1));
		// The flat run of n ticks with drag: v * (1 + d + ... + d^(n-1)).
		double horizontal = flat * (1.0 - AIR_DRAG) / (1.0 - Math.pow(AIR_DRAG, ticks));
		return new Launch(horizontal, vertical);
	}

	/** The arc for a leap in: higher for a longer leap. */
	public static double arcFor(double distance) {
		return 1.0 + distance * 0.2;
	}

	/** The highest point reached from {@code vertical} launch speed. */
	static double apex(double vertical) {
		double y = 0.0;
		double v = vertical;
		while (v > 0.0) {
			y += v;
			v = (v - GRAVITY) * VERTICAL_DRAG;
		}
		return y;
	}

	/** The launch speed that peaks at {@code height}. */
	static double launchSpeedFor(double height) {
		double low = 0.0;
		double high = 3.0;
		for (int i = 0; i < 40; i++) {
			double mid = (low + high) / 2.0;
			if (apex(mid) < height) {
				low = mid;
			} else {
				high = mid;
			}
		}
		return high;
	}

	/** Ticks from launch until it comes back down to {@code height}. */
	static int flightTicks(double vertical, double height) {
		double y = 0.0;
		double v = vertical;
		for (int tick = 1; tick <= MAX_TICKS; tick++) {
			y += v;
			v = (v - GRAVITY) * VERTICAL_DRAG;
			if (v < 0.0 && y <= height) {
				return tick;
			}
		}
		return MAX_TICKS;
	}

	/** The flat distance travelled in {@code ticks} from {@code horizontal} launch speed. */
	static double flatRun(double horizontal, int ticks) {
		return horizontal * (1.0 - Math.pow(AIR_DRAG, ticks)) / (1.0 - AIR_DRAG);
	}

	/**
	 * Whether it is still going up with nothing carrying it, already higher above the last place it stood than its
	 * highest leap: whatever pushed it there, it must fall back.
	 *
	 * @param rise          how far it is above the last place it stood, swam or climbed
	 * @param verticalSpeed its speed upward
	 * @param maxHeight     its highest leap
	 */
	public static boolean risesTooHigh(double rise, double verticalSpeed, double maxHeight) {
		return verticalSpeed > 0.0 && rise > maxHeight + RISE_SLACK;
	}

	/** Whether a hidden lurker bursts out at a player this far away. */
	public static boolean emerges(double distance, boolean sneaking, SwampLurkerSettings settings) {
		return distance <= (sneaking ? settings.sneakEmergeRange() : settings.emergeRange());
	}
}
