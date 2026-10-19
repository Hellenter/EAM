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
	 * Ticks an axe that would disable a player's shield for {@code seconds} keeps his shield down: {@code multiplier}
	 * times as long. 0 for anything that does not disable shields.
	 */
	public static int shieldKnockedTicks(float seconds, float multiplier) {
		return Math.max(0, Math.round(seconds * multiplier * 20.0F));
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

	/**
	 * Whether a draugr pressed against a wall claws his way up it, like a spider: only toward a target above him, at most
	 * {@code maxRise} blocks up and {@code reach} blocks across, and never higher than it. A target on the same floor he
	 * walks to; he does not climb the spires on the way. Once climbing he keeps on until his feet are a block above the
	 * target's, enough to get over the lip of the ledge it stands on.
	 *
	 * @param rise       how far his target's feet are above his
	 * @param horizontal how far his target is across
	 * @param climbing   whether he was climbing last tick
	 */
	public static boolean climbs(double rise, double horizontal, boolean climbing, double maxRise, double reach) {
		if (horizontal > reach || rise > maxRise) {
			return false;
		}
		return climbing ? rise > -1.0 : rise >= 1.0;
	}
}
