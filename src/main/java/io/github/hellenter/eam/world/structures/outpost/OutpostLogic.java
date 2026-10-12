package io.github.hellenter.eam.world.structures.outpost;

/** Geometry of the outpost's clearing, apart from the world so it can be tested. */
public final class OutpostLogic {
	private OutpostLogic() {
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
