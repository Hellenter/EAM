package io.github.hellenter.eam.mobs.entity.duergar;

/** Pure rules for the duergar, unit tested. */
public final class DuergarLogic {
	private DuergarLogic() {
	}

	/** Renegades spawn only where no light at all falls: no torch, lava or glowing mushroom near, no sky. */
	public static boolean darkEnough(int blockLight, int skyLight) {
		return blockLight == 0 && skyLight == 0;
	}

	/** How many followers a renegade leader brings, from {@code min} to {@code max}, for a roll in [0, 1). */
	public static int followerCount(int min, int max, double roll) {
		int low = Math.min(min, max);
		int high = Math.max(min, max);
		return Math.min(high, low + (int) (roll * (high - low + 1)));
	}
}
