package io.github.hellenter.eam.survival.sleep;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Decides whether a bed stands in a safe room: a closed room, walled with blocks mobs cannot break, lit, and with a
 * door if it is small. Pure logic over a {@link RoomView}, no Minecraft state.
 *
 * <p>Two bounded flood fills start from the bed. The first walks only open cells (air, torches); if it runs past the
 * volume or radius cap, the room is not closed. The second also walks through weak blocks (dirt, furniture); if that
 * one escapes, the room is closed only by weak blocks. Weak blocks inside a strong shell, like a chest, do no harm.
 *
 * <p>When a fill escapes, the shortest way out it found points at the weak spot: the first weak block on it, or for an
 * open room the first gap on it (a cell walled on both sides across the way, like a window or a doorway).
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

	/**
	 * @param spot   the weak spot of the room as {x, y, z}, or null when there is none to show (a closed hall too big, a
	 *               missing door or light)
	 * @param seenFrom the open cell next to {@code spot} on the room side, or null; a solid weak block is best marked on
	 *               this face
	 */
	public record Report(Result result, int[] spot, int[] seenFrom) {
		static Report at(Result result, List<int[]> path, int index) {
			return index < 0 ? new Report(result, null, null) : new Report(result, path.get(index), path.get(index - 1));
		}
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
		return inspect(view, starts, settings).result();
	}

	/** Like {@link #check}, and also finds where the room fails. */
	public static Report inspect(RoomView view, int[][] starts, SafeRoomSettings settings) {
		Fill room = fill(view, starts, settings, false, false);
		if (room.escape != null) {
			return Report.at(Result.NOT_ENCLOSED, room.escape, gap(view, room.escape, settings));
		}
		Fill shell = fill(view, starts, settings, true, true);
		if (shell.escape != null) {
			// A weak block right under the bed is hidden by it: point at one seen from the room if there is one.
			Fill seen = fill(view, starts, settings, true, false);
			List<int[]> path = seen.escape != null ? seen.escape : shell.escape;
			return Report.at(Result.WEAK_BLOCKS, path, firstWeak(view, path));
		}
		return new Report(result(room, settings), null, null);
	}

	private static Result result(Fill room, SafeRoomSettings settings) {
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

	/** @return the index of the first weak block on the way out past the bed, or -1 */
	private static int firstWeak(RoomView view, List<int[]> path) {
		for (int i = 1; i < path.size(); i++) {
			int[] pos = path.get(i);
			if (view.cell(pos[0], pos[1], pos[2]) == Cell.WEAK) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * The first cell on the way out walled on both sides across the way (a window, a doorway, a hole in the roof); else
	 * the first one with open sky above it (a missing wall or roof).
	 *
	 * @return the index of that cell on the path, or -1
	 */
	private static int gap(RoomView view, List<int[]> path, SafeRoomSettings settings) {
		for (int i = 1; i < path.size(); i++) {
			int[] from = path.get(i - 1);
			int[] pos = path.get(i);
			for (int axis = 0; axis < 3; axis++) {
				if (from[axis] != pos[axis]) {
					continue;
				}
				int[] a = pos.clone();
				int[] b = pos.clone();
				a[axis]--;
				b[axis]++;
				if (view.cell(a[0], a[1], a[2]) != Cell.OPEN && view.cell(b[0], b[1], b[2]) != Cell.OPEN) {
					return i;
				}
			}
		}
		for (int i = 1; i < path.size(); i++) {
			if (openSky(view, path.get(i), settings.maxRadius())) {
				return i;
			}
		}
		return -1;
	}

	private static boolean openSky(RoomView view, int[] pos, int height) {
		for (int dy = 1; dy <= height; dy++) {
			if (view.cell(pos[0], pos[1] + dy, pos[2]) != Cell.OPEN) {
				return false;
			}
		}
		return true;
	}

	/**
	 * @param weakFromBed whether the fill may step from the bed straight into a weak block
	 * @return the filled room; its {@code escape} is set if the fill ran past the caps
	 */
	private static Fill fill(RoomView view, int[][] starts, SafeRoomSettings settings, boolean throughWeak, boolean weakFromBed) {
		int ox = starts[0][0];
		int oy = starts[0][1];
		int oz = starts[0][2];
		int radius = settings.maxRadius();
		Fill result = new Fill();
		// Each reached cell to the cell it was reached from; the starts map to null.
		Map<Long, int[]> from = new HashMap<>();
		ArrayDeque<int[]> queue = new ArrayDeque<>();
		for (int[] start : starts) {
			long key = key(start[0] - ox, start[1] - oy, start[2] - oz);
			if (!from.containsKey(key)) {
				from.put(key, null);
				queue.add(start);
			}
		}
		int visited = 0;
		while (!queue.isEmpty()) {
			int[] pos = queue.poll();
			if (++visited > settings.maxVolume()) {
				// Polled last, so as far from the bed as the fill got: the way there leads out, if anywhere.
				result.escape = path(from, pos, ox, oy, oz);
				return result;
			}
			result.add(pos[0], pos[1], pos[2], view.blockLight(pos[0], pos[1], pos[2]));
			boolean bed = from.get(key(pos[0] - ox, pos[1] - oy, pos[2] - oz)) == null;
			for (int[] step : NEIGHBORS) {
				int x = pos[0] + step[0];
				int y = pos[1] + step[1];
				int z = pos[2] + step[2];
				long key = key(x - ox, y - oy, z - oz);
				if (from.containsKey(key)) {
					continue;
				}
				Cell cell = view.cell(x, y, z);
				if (cell == Cell.WEAK && bed && !weakFromBed) {
					continue;
				}
				from.put(key, pos);
				if (cell == Cell.DOOR) {
					result.door = true;
				} else if (cell == Cell.OPEN || throughWeak && cell == Cell.WEAK) {
					if (Math.abs(x - ox) > radius || Math.abs(y - oy) > radius || Math.abs(z - oz) > radius) {
						result.escape = path(from, new int[] {x, y, z}, ox, oy, oz);
						return result;
					}
					queue.add(new int[] {x, y, z});
				}
			}
		}
		return result;
	}

	/** The way from a start to {@code end}, start first. */
	private static List<int[]> path(Map<Long, int[]> from, int[] end, int ox, int oy, int oz) {
		List<int[]> path = new ArrayList<>();
		for (int[] pos = end; pos != null; pos = from.get(key(pos[0] - ox, pos[1] - oy, pos[2] - oz))) {
			path.add(pos);
		}
		Collections.reverse(path);
		return path;
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
		/** The way out, start first, if the fill escaped or grew too big. */
		List<int[]> escape;

		void add(int x, int y, int z, int light) {
			minX = Math.min(minX, x);
			maxX = Math.max(maxX, x);
			minZ = Math.min(minZ, z);
			maxZ = Math.max(maxZ, z);
			maxLight = Math.max(maxLight, light);
		}
	}
}
