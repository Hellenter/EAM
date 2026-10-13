package io.github.hellenter.eam.mobs.entity.yaga.grave;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;

/** Where Baba Yaga's grave goes, free of the game so it can be unit tested. */
public final class YagaGraveLogic {
	/** The grave lies this far from where she died, when that spot is in her swamp. */
	public static final int NEAR_MIN = 40;
	public static final int NEAR_MAX = 90;
	/** Killed outside a swamp, her map looks for the nearest swamp this far out. */
	public static final int FAR_MAX = 256;
	/** Rings of candidates are this far apart. */
	public static final int RING_STEP = 8;

	private YagaGraveLogic() {
	}

	/**
	 * Offsets from the spot she died at, in the order to try them: ring by ring outwards from {@code min} to {@code max}
	 * blocks, each ring a whole turn of points about {@link #RING_STEP} apart starting at a random angle.
	 */
	public static List<int[]> candidates(int min, int max, DoubleSupplier random) {
		List<int[]> offsets = new ArrayList<>();
		for (int radius = Math.max(0, min); radius <= max; radius += RING_STEP) {
			if (radius == 0) {
				offsets.add(new int[] {0, 0});
				continue;
			}
			int points = Math.max(6, (int) Math.round(2.0 * Math.PI * radius / RING_STEP));
			double start = random.getAsDouble() * Math.PI * 2.0;
			for (int i = 0; i < points; i++) {
				double angle = start + Math.PI * 2.0 * i / points;
				offsets.add(new int[] {(int) Math.round(Math.cos(angle) * radius), (int) Math.round(Math.sin(angle) * radius)});
			}
		}
		return offsets;
	}
}
