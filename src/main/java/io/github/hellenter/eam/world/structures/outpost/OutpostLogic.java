package io.github.hellenter.eam.world.structures.outpost;

/** Geometry of the outpost's site and clearing, apart from the world so it can be tested. */
public final class OutpostLogic {
	/** Blocks between two ground samples. */
	public static final int STEP = 8;
	/** Samples from the chunk middle to the edge of the sampled square, which is {@code 2 * REACH + 1} samples wide. */
	public static final int REACH = 5;
	/** Samples from the outpost's middle to the edge of its footprint: the template is 33 blocks, 4 steps, wide. */
	private static final int HALF = 2;
	/** How far, in samples, the middle may move off the chunk middle to find flatter ground. */
	private static final int SHIFT = 1;

	private OutpostLogic() {
	}

	/**
	 * Where to put the outpost: its middle {@code dx}, {@code dz} samples off the chunk middle, the side its gate faces
	 * (0 north, 1 east, 2 south, 3 west, the order of {@code Rotation}'s turns from the template's north gate), and the
	 * ground level of its grass layer.
	 */
	public record Site(int dx, int dz, int gate, int ground) {
	}

	/**
	 * Picks the flattest dry site among the middles at most one sample off the chunk middle, preferring the middle on
	 * a tie, or none if every one is wet or rises more than {@code maxSlope}. The outpost stands on the highest ground
	 * of its footprint, so a slope never buries it, and its gate faces the side whose land falls lowest.
	 *
	 * @param ground surface height at each sample, {@code [x][z]}, {@code 2 * REACH + 1} square, the chunk middle in the
	 *               centre
	 * @param wet    whether each sample is under water
	 * @param turn   the side to start from when two sides fall equally low, so ties do not always face one way
	 */
	public static Site site(int[][] ground, boolean[][] wet, int maxSlope, int turn) {
		Site best = null;
		int bestSpread = Integer.MAX_VALUE;
		int bestShift = Integer.MAX_VALUE;
		for (int dx = -SHIFT; dx <= SHIFT; dx++) {
			for (int dz = -SHIFT; dz <= SHIFT; dz++) {
				int lowest = Integer.MAX_VALUE;
				int highest = Integer.MIN_VALUE;
				boolean dry = true;
				for (int x = dx - HALF; x <= dx + HALF; x++) {
					for (int z = dz - HALF; z <= dz + HALF; z++) {
						dry &= !wet[REACH + x][REACH + z];
						lowest = Math.min(lowest, ground[REACH + x][REACH + z]);
						highest = Math.max(highest, ground[REACH + x][REACH + z]);
					}
				}
				int spread = highest - lowest;
				int shift = Math.abs(dx) + Math.abs(dz);
				if (!dry || spread > maxSlope || spread > bestSpread || spread == bestSpread && shift >= bestShift) continue;
				best = new Site(dx, dz, gate(ground, dx, dz, turn), highest);
				bestSpread = spread;
				bestShift = shift;
			}
		}
		return best;
	}

	/** The side of the footprint at {@code dx}, {@code dz} whose two outer rows of samples lie lowest on average. */
	static int gate(int[][] ground, int dx, int dz, int turn) {
		int gate = 0;
		long lowest = Long.MAX_VALUE;
		for (int i = 0; i < 4; i++) {
			int side = (turn + i) & 3;
			long sum = 0;
			for (int out = HALF + 1; out <= HALF + 2; out++) {
				for (int along = -HALF; along <= HALF; along++) {
					sum += switch (side) {
						case 0 -> ground[REACH + dx + along][REACH + dz - out];
						case 1 -> ground[REACH + dx + out][REACH + dz + along];
						case 2 -> ground[REACH + dx + along][REACH + dz + out];
						default -> ground[REACH + dx - out][REACH + dz + along];
					};
				}
			}
			if (sum < lowest) {
				lowest = sum;
				gate = side;
			}
		}
		return gate;
	}

	/**
	 * Distance, rounded up, from the column at {@code x}, {@code z} to the footprint from {@code minX}, {@code minZ} to
	 * {@code maxX}, {@code maxZ} inclusive; 0 inside it.
	 */
	public static int distance(int minX, int minZ, int maxX, int maxZ, int x, int z) {
		long dx = Math.max(0, Math.max(minX - x, x - maxX));
		long dz = Math.max(0, Math.max(minZ - z, z - maxZ));
		return (int) Math.ceil(Math.sqrt(dx * dx + dz * dz));
	}

	/**
	 * The top of the bank of earth around an outpost standing above the ground, {@code distance} blocks out from its
	 * footprint: level with its grass right beside it, one block lower for each block further out.
	 */
	public static int bankTop(int grass, int distance) {
		return grass - distance + 1;
	}

	/**
	 * Whether the column at {@code x}, {@code z} lies within {@code radius} blocks of the outpost's footprint, which runs
	 * from {@code minX}, {@code minZ} to {@code maxX}, {@code maxZ} inclusive. The clearing has rounded corners.
	 */
	public static boolean inClearing(int minX, int minZ, int maxX, int maxZ, int radius, int x, int z) {
		long dx = Math.max(0, Math.max(minX - x, x - maxX));
		long dz = Math.max(0, Math.max(minZ - z, z - maxZ));
		return dx * dx + dz * dz <= (long) radius * radius;
	}

	/**
	 * Whether a block at height {@code y} in a cleared column may be removed: anywhere outside the footprint, and only
	 * above the template's top inside it, where the template has no blocks of its own.
	 */
	public static boolean mayClear(boolean inFootprint, int y, int templateTop) {
		return !inFootprint || y > templateTop;
	}
}
