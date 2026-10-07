package io.github.hellenter.eam.survival.progression;

/** Pure progression formulas, free of Minecraft state so they can be unit tested. */
public final class ProgressionLogic {
	private ProgressionLogic() {
	}

	/** Size of the health, food and water bars at an XP level: grows by a step every few levels, as in MITE. */
	public static int capacity(int xpLevel, ProgressionRules rules) {
		int steps = Math.max(xpLevel, 0) / rules.levelsPerStep();
		long size = rules.baseCapacity() + (long) steps * rules.capacityStep();
		return (int) Math.min(size, rules.maxCapacity());
	}

	/** Whether hunger and thirst are paused, given the ticks lived since the first spawn or the last death. */
	public static boolean inGracePeriod(int ticksSinceDeath, ProgressionRules rules) {
		return ticksSinceDeath < rules.gracePeriodTicks();
	}
}
