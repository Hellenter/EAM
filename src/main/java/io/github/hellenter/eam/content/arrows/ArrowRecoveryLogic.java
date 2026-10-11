package io.github.hellenter.eam.content.arrows;

/** Chance for an arrow stuck in a mob to drop when the mob dies. */
public final class ArrowRecoveryLogic {
	private ArrowRecoveryLogic() {
	}

	/** Base chance plus a share per Arrow Recovery level, at most 1. */
	public static float chance(float baseChance, float chancePerLevel, int recoveryLevel) {
		return Math.clamp(baseChance + chancePerLevel * Math.max(0, recoveryLevel), 0.0F, 1.0F);
	}
}
