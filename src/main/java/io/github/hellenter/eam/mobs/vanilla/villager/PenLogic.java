package io.github.hellenter.eam.mobs.vanilla.villager;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/** Pure pen search, unit tested: whether an animal stands in a fenced pen with no door or gate in its fence. */
public final class PenLogic {
	/** {@link Terrain#step} result: a fence, wall or block too high to step onto. */
	public static final int WALL = Integer.MIN_VALUE;
	/** {@link Terrain#step} result: a door or fence gate. */
	public static final int DOOR = Integer.MIN_VALUE + 1;

	private PenLogic() {
	}

	/** The ground around the animal. */
	@FunctionalInterface
	public interface Terrain {
		/** The height an animal standing at {@code y} gets to by walking onto column {@code x, z}, or {@link #WALL} or {@link #DOOR}. */
		int step(int x, int y, int z);
	}

	/**
	 * Whether the animal at {@code x, y, z} cannot walk out: the ground it reaches is closed by walls and has no door or
	 * gate. Ground larger than {@code limit} cells counts as open.
	 */
	public static boolean closedPen(Terrain terrain, int x, int y, int z, int limit) {
		Set<Long> seen = new HashSet<>();
		ArrayDeque<int[]> open = new ArrayDeque<>();
		seen.add(key(x, y, z));
		open.add(new int[] {x, y, z});
		int[][] sides = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
		while (!open.isEmpty()) {
			int[] cell = open.poll();
			for (int[] side : sides) {
				int nx = cell[0] + side[0];
				int nz = cell[2] + side[1];
				int ny = terrain.step(nx, cell[1], nz);
				if (ny == DOOR) return false;
				if (ny == WALL || !seen.add(key(nx, ny, nz))) continue;
				if (seen.size() > limit) return false;
				open.add(new int[] {nx, ny, nz});
			}
		}
		return true;
	}

	private static long key(int x, int y, int z) {
		return ((long) x & 0x3FFFFFF) << 38 | ((long) z & 0x3FFFFFF) << 12 | (long) y & 0xFFF;
	}
}
