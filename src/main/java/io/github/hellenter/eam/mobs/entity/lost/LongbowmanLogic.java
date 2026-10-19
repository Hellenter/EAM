package io.github.hellenter.eam.mobs.entity.lost;

import org.jspecify.annotations.Nullable;

import java.util.List;

/** Pure rules of the Lost longbowman: when he looses a marking arrow and whom he shoots at. */
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
