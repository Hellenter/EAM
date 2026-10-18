package io.github.hellenter.eam.survival.death;

/** Pure rules of dying. */
public final class DeathLogic {
	private DeathLogic() {
	}

	/** The level a player respawns with: {@code lost} levels fewer, never below zero. */
	public static int levelsAfterDeath(int level, int lost) {
		return Math.max(0, level - Math.max(0, lost));
	}
}
