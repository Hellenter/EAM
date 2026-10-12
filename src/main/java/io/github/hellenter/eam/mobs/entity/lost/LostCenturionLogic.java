package io.github.hellenter.eam.mobs.entity.lost;

/** The centurion's horn calls, free of game state so they can be unit tested. */
public final class LostCenturionLogic {
	/** Whom a horn call brings. */
	public enum Call {
		/** A pair of heavy cavalrymen on their horses. */
		CAVALRY,
		/** A squad of the Lost on foot. */
		SQUAD
	}

	private LostCenturionLogic() {
	}

	/** Whom the call brings after {@code callsBefore} earlier calls; {@code roll} is in [0, 1). */
	public static Call call(int callsBefore, double roll, LostCenturionSettings.Horn horn) {
		if (callsBefore <= 0) {
			return Call.CAVALRY;
		}
		double cavalry = callsBefore == 1 ? horn.secondCavalryChance() : horn.laterCavalryChance();
		return roll < cavalry ? Call.CAVALRY : Call.SQUAD;
	}

	/** Ticks until the next call, after {@code callsDone} calls in all (at least one). */
	public static int cooldownAfter(int callsDone, LostCenturionSettings.Horn horn) {
		return callsDone <= 1 ? horn.secondCooldown() : horn.laterCooldown();
	}
}
