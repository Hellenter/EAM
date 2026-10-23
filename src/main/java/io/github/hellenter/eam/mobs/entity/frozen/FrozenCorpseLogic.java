package io.github.hellenter.eam.mobs.entity.frozen;

/** Pure rules of the frozen corpse's chilling touch. */
public final class FrozenCorpseLogic {
	private FrozenCorpseLogic() {
	}

	/**
	 * The slowness level a hit leaves: each hit while the victim is still slowed from the last one deepens it by a
	 * level, up to the cap.
	 *
	 * @param current  amplifier of the slowness the victim has now, or -1 for none
	 * @param maxLevel the deepest it goes, 0 being Slowness I
	 */
	public static int nextLevel(int current, int maxLevel) {
		return Math.max(0, Math.min(maxLevel, current + 1));
	}
}
