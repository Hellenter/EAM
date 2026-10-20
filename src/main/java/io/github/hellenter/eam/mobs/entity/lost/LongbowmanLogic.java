package io.github.hellenter.eam.mobs.entity.lost;

import org.jspecify.annotations.Nullable;

import java.util.List;

/** Pure rules of the Lost longbowman: when he looses a marking arrow, whom he shoots at and his power shot. */
public final class LongbowmanLogic {
	private LongbowmanLogic() {
	}

	/**
	 * Whether this shot is a marking arrow: only at a player not marked yet, and at most one shot in {@code every}.
	 * {@code shotsSinceMark} counts his shots since the last marking arrow; it starts full, so his first shot at a
	 * player marks him.
	 */
	public static boolean marks(int shotsSinceMark, int every, boolean unmarkedPlayer) {
		return unmarkedPlayer && shotsSinceMark >= every - 1;
	}

	/** The damage a marked one takes for a hit of {@code amount}: {@code bonus} more, 0.2 for a fifth. */
	public static float markedDamage(float amount, float bonus) {
		return amount * (1.0F + bonus);
	}

	/**
	 * The red power arrow's damage: his full-draw arrow's hit ({@code base}, a squad archer's base damage, flying at
	 * {@code archerSpeed} and hurting {@code damageMultiplier} times more), times {@code powerMultiplier}.
	 */
	public static float redDamage(double base, float archerSpeed, float damageMultiplier, float powerMultiplier) {
		return (float) (base * archerSpeed * damageMultiplier * powerMultiplier);
	}

	/** Ticks to the next power shot: a number from {@code min} to {@code max}, picked by {@code roll} in [0, 1). */
	public static int powerCooldown(int min, int max, float roll) {
		return max <= min ? min : min + (int) (roll * (max - min + 1));
	}

	/** One player he could shoot at. */
	public record Candidate<T>(T player, boolean marked, float health, double distanceSqr) {
	}

	/** Whom to shoot at: a marked player first, then the one with the least health, then the nearest. */
	public static <T> @Nullable T pick(List<Candidate<T>> candidates) {
		Candidate<T> best = null;
		for (Candidate<T> candidate : candidates) {
			if (best == null || better(candidate, best)) {
				best = candidate;
			}
		}
		return best == null ? null : best.player();
	}

	private static boolean better(Candidate<?> a, Candidate<?> b) {
		if (a.marked() != b.marked()) {
			return a.marked();
		}
		if (a.health() != b.health()) {
			return a.health() < b.health();
		}
		return a.distanceSqr() < b.distanceSqr();
	}
}
