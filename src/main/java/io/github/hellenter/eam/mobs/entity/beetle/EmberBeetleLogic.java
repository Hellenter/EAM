package io.github.hellenter.eam.mobs.entity.beetle;

/** Pure ember beetle rules: when it notices a player, how hard a crushing blow lands, and its belly of eaten coal. */
public final class EmberBeetleLogic {
	private EmberBeetleLogic() {
	}

	/** True when a player {@code distanceSqr} away (squared) is close enough that the beetle goes for him unprovoked. */
	public static boolean notices(double distanceSqr, double noticeRange) {
		return distanceSqr < noticeRange * noticeRange;
	}

	/** Damage a blow does to the shell: crushing weapons crack it, the rest land as they are. */
	public static float shellDamage(float damage, boolean crushing, float crushMultiplier) {
		return crushing ? damage * crushMultiplier : damage;
	}

	/** True when a beetle that has {@code eaten} coals in its belly still wants more. */
	public static boolean hungry(int eaten, int maxEaten) {
		return eaten < maxEaten;
	}

	/** Coal a beetle with {@code eaten} in its belly carries after one more, never past the limit. */
	public static int afterEating(int eaten, int maxEaten) {
		return Math.min(maxEaten, eaten + 1);
	}
}
