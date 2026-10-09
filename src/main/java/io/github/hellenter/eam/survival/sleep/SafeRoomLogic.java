package io.github.hellenter.eam.survival.sleep;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * Decides whether a bed stands in a safe room: a closed room, walled with blocks mobs cannot break, lit, and with a
 * door if it is small. Pure logic over a {@link RoomView}, no Minecraft state.
 *
 * <p>Two bounded flood fills start from the bed. The first walks only open cells (air, torches); if it runs past the
 * volume or radius cap, the room is not closed. The second also walks through weak blocks (dirt, furniture); if that
 * one escapes, the room is closed only by weak blocks. Weak blocks inside a strong shell, like a chest, do no harm.
 */
public final class SafeRoomLogic {
	/** What a block counts as for the room check. */
	public enum Cell {
		/** No collision: air, torches, plants. The fill walks through it. */
		OPEN,
		/** A block mobs can break: dirt, wool, furniture. */
		WEAK,
		/** A block mobs cannot break, or planks and the like a datapack lists as safe walls. */
		STRONG,
		/** A door. Counts as a strong wall. */
		DOOR
	}

	public enum Result {
		SAFE,
		NOT_ENCLOSED,
		WEAK_BLOCKS,
		NEEDS_DOOR,
		NEEDS_LIGHT
	}

	/** The world as the check sees it. */
	public interface RoomView {
		Cell cell(int x, int y, int z);

		/** Block light (torches, lanterns), 0..15. Sky light does not count. */
		int blockLight(int x, int y, int z);
	}

	private static final int[][] NEIGHBORS = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

	private SafeRoomLogic() {
	}

	/**
	 * @param starts the bed's cells as {x, y, z}; the fill starts there whatever they are
	 */
	public static Result check(RoomView view, int[][] starts, SafeRoomSettings settings) {
		Fill room = fill(view, starts, settings, false);
		if (room == null) {
			return Result.NOT_ENCLOSED;
		}
		if (fill(view, starts, settings, true) == null) {
			return Result.WEAK_BLOCKS;
		}
		int width = room.maxX - room.minX + 1;
		int depth = room.maxZ - room.minZ + 1;
		if (!room.door && (width < settings.doorBelowSize() || depth < settings.doorBelowSize())) {
			return Result.NEEDS_DOOR;
		}
		if (room.maxLight < settings.minLight()) {
			return Result.NEEDS_LIGHT;
		}
		return Result.SAFE;
	}

	/** @return the filled room, or null if it escaped the caps */
	private static Fill fill(RoomView view, int[][] starts, SafeRoomSettings settings, boolean throughWeak) {
		int ox = starts[0][0];
		int oy = starts[0][1];
		int oz = starts[0][2];
		int radius = settings.maxRadius();
		Fill result = new Fill();
		Set<Long> seen = new HashSet<>();
		ArrayDeque<int[]> queue = new ArrayDeque<>();
		for (int[] start : starts) {
			if (seen.add(key(start[0] - ox, start[1] - oy, start[2] - oz))) {
				queue.add(start);
			}
		}
		int visited = 0;
		while (!queue.isEmpty()) {
			int[] pos = queue.poll();
			if (++visited > settings.maxVolume()) {
				return null;
			}
			result.add(pos[0], pos[1], pos[2], view.blockLight(pos[0], pos[1], pos[2]));
			for (int[] step : NEIGHBORS) {
				int x = pos[0] + step[0];
				int y = pos[1] + step[1];
				int z = pos[2] + step[2];
				if (!seen.add(key(x - ox, y - oy, z - oz))) {
					continue;
				}
				Cell cell = view.cell(x, y, z);
				if (cell == Cell.DOOR) {
					result.door = true;
				} else if (cell == Cell.OPEN || throughWeak && cell == Cell.WEAK) {
					if (Math.abs(x - ox) > radius || Math.abs(y - oy) > radius || Math.abs(z - oz) > radius) {
						return null;
					}
					queue.add(new int[] {x, y, z});
				}
			}
		}
		return result;
	}

	/** Packs an offset of up to ±1023 per axis. */
	private static long key(int dx, int dy, int dz) {
		return ((long) (dx + 1024) << 22) | ((long) (dy + 1024) << 11) | (dz + 1024);
	}

	private static final class Fill {
		int minX = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int minZ = Integer.MAX_VALUE;
		int maxZ = Integer.MIN_VALUE;
		int maxLight;
		boolean door;

		void add(int x, int y, int z, int light) {
			minX = Math.min(minX, x);
			maxX = Math.max(maxX, x);
			minZ = Math.min(minZ, z);
			maxZ = Math.max(maxZ, z);
			maxLight = Math.max(maxLight, light);
		}
	}
}
