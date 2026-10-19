package io.github.hellenter.eam.content.tools;

/** Pure maths for the knife's bonus to animal drops ({@link KnifeModule}). */
public final class KnifeLootLogic {
	private KnifeLootLogic() {
	}

	/**
	 * The size of a dropped stack after the knife bonus: {@code count * multiplier}, its fractional part rounded up with
	 * that chance, capped at {@code maxCount}.
	 *
	 * @param roll a uniform random number in [0, 1)
	 */
	public static int bonusCount(int count, double multiplier, int maxCount, double roll) {
		if (count <= 0 || multiplier <= 1.0) return count;
		double scaled = count * multiplier;
		int whole = (int) Math.floor(scaled);
		if (roll < scaled - whole) whole++;
		return Math.max(count, Math.min(whole, maxCount));
	}
}
