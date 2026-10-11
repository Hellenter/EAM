package io.github.hellenter.eam.survival.bleeding;

/** Pure bleeding rules, kept apart from the game so they can be unit tested. */
public final class BleedingLogic {
	private BleedingLogic() {
	}

	/**
	 * Whether bleeding hurts on the tick with {@code duration} ticks left. The first hit comes one interval after the
	 * wound, the last on the final tick, so a 60-tick bleed with a 15-tick interval hits four times.
	 */
	public static boolean hurtsThisTick(int duration, int interval) {
		return duration > 0 && (duration - 1) % interval == 0;
	}

	/** Exhaustion to add on top of the normal drain so it runs {@code multiplier} times as fast. */
	public static float extraDrain(float normal, float multiplier) {
		return normal * Math.max(multiplier - 1.0F, 0.0F);
	}

	/** Food exhaustion from activity while bleeding: everything tires {@code multiplier} times as much. */
	public static float activity(float amount, boolean bleeding, float multiplier) {
		return bleeding ? amount * multiplier : amount;
	}
}
