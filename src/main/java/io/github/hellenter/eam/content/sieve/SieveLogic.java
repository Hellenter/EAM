package io.github.hellenter.eam.content.sieve;

/** Pure rules of the sieve: how worn it looks, how much gravel is left on it, when it breaks. */
public final class SieveLogic {
	/** Gravel sifted before the sieve breaks; also the sieve item's durability. */
	public static final int USES = 32;
	/** Wear stages: whole, worn, torn. The mesh tears like an anvil cracks. */
	public static final int MAX_WEAR = 2;
	/** Gravel heap heights on the mesh while sifting; 0 when the sieve is empty. */
	public static final int MAX_GRAVEL = 4;

	private SieveLogic() {
	}

	/** How torn the mesh looks after {@code damage} sifts: 0-10 whole, 11-21 worn, 22-31 torn. */
	public static int wear(int damage) {
		return Math.clamp((long) damage * (MAX_WEAR + 1) / USES, 0, MAX_WEAR);
	}

	/** True once the sieve has sifted {@code damage} times and has no use left. */
	public static boolean wornOut(int damage) {
		return damage >= USES;
	}

	/** Height of the gravel heap {@code progress} ticks into a sift: full at the start, shrinking, never 0 before the end. */
	public static int gravelLeft(int progress, int siftTicks) {
		int left = MAX_GRAVEL - (int) ((long) progress * MAX_GRAVEL / siftTicks);
		return Math.clamp(left, 1, MAX_GRAVEL);
	}

	public static boolean done(int progress, int siftTicks) {
		return progress >= siftTicks;
	}
}
