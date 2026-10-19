package io.github.hellenter.eam.content.dryer;

/** Pure food dryer rules. */
public final class DryerLogic {
	private DryerLogic() {
	}

	/** Drying progress after one tick: rain falling on the rack stops it, but what it dried so far stays. */
	public static int advance(int progress, boolean wet) {
		return wet ? progress : progress + 1;
	}

	/** Whether food that has dried for {@code progress} ticks is done. */
	public static boolean done(int progress, DryerSettings settings) {
		return progress >= settings.dryTicks();
	}
}
