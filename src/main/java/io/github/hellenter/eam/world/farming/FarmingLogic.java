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

	/**
	 * How many steps an animal ages on this tick when it ages {@code speed} steps in {@code slowdown} ticks: 0 or 1 while
	 * slower than vanilla, more when faster.
	 */
	public static int ageSteps(int tickCount, int slowdown, double speed) {
		double rate = speed / Math.max(1, slowdown);
		return (int) (Math.floor((tickCount + 1L) * rate) - Math.floor(tickCount * rate));
	}

	/** {@code age} moved {@code steps} towards 0, where a baby is grown up and a parent can breed again. */
	public static int stepAge(int age, int steps) {
		return age < 0 ? Math.min(0, age + steps) : Math.max(0, age - steps);
	}
}
