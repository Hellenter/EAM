package io.github.hellenter.eam.world.farming;

/** Pure growth math, unit tested. */
public final class FarmingLogic {
	private FarmingLogic() {
	}

	/**
	 * Bound of the vanilla {@code random.nextInt(bound) == 0} growth roll, scaled so a crop grows {@code slowdown} times
	 * slower, or {@code boost} times faster than that once bone meal was used on it.
	 */
	public static int growthBound(int vanillaBound, double slowdown, double boost) {
		return Math.max(1, (int) Math.round(vanillaBound * slowdown / boost));
	}

	/** Whether an animal ages on this tick: one tick in {@code slowdown}, so growing up and breeding cooldowns stretch. */
	public static boolean agesThisTick(int tickCount, int slowdown) {
		return slowdown <= 1 || tickCount % slowdown == 0;
	}
}
