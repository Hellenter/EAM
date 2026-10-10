package io.github.hellenter.eam.mobs.vanilla.crypt;

import java.util.function.IntPredicate;

/** Where in height a crypt spawner puts its mob. */
public final class CryptSpawnerLogic {
	private CryptSpawnerLogic() {
	}

	/**
	 * Finds a floor for a mob near {@code startY}, which is up to {@code range} blocks above or below the spawner: the
	 * first height going down from {@code startY} where the mob can stand, then going up. Searches only within
	 * {@code range} of the spawner, and gives {@code startY} back when nothing fits.
	 *
	 * @param canStand whether a mob can stand at a height: its feet in a free block, a solid block under them
	 */
	public static int standingY(int spawnerY, int startY, int range, IntPredicate canStand) {
		int low = spawnerY - range;
		int high = spawnerY + range;
		for (int y = startY; y >= low; y--) {
			if (canStand.test(y)) return y;
		}
		for (int y = startY + 1; y <= high; y++) {
			if (canStand.test(y)) return y;
		}
		return startY;
	}
}
