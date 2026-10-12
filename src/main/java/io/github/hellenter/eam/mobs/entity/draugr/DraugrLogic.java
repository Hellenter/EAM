package io.github.hellenter.eam.mobs.entity.draugr;

import java.util.List;

/** The draugr's arithmetic, apart from the game so it can be unit tested. */
public final class DraugrLogic {
	private DraugrLogic() {
	}

	/** Damage after his weakness: fire hurts him {@code fireMultiplier} times as much. */
	public static float damageTaken(float damage, boolean fire, float fireMultiplier) {
		return fire ? damage * fireMultiplier : damage;
	}

	/**
	 * The waves of a call that are due {@code elapsed} ticks after it went out, from wave {@code next} on: returns the
	 * index after the last one due (equal to {@code next} if none is).
	 */
	public static int wavesDue(List<DraugrSettings.Wave> waves, int next, long elapsed) {
		int index = next;
		while (index < waves.size() && waves.get(index).delay() <= elapsed) {
			index++;
		}
		return index;
	}
}
