package io.github.hellenter.eam.world.structures.mine;

import java.util.ArrayList;
import java.util.List;

/**
 * Wings of an underworld mine: a piece and every piece the mine grew from it, away from the hall. One wing may have
 * caved in (rubble on the floors, a heap that closes its entrance, broken supports) and another may have flooded (water up
 * to the floor of the tunnel it branches off). The pieces form a tree: {@code parent[i]} is the piece that piece {@code i}
 * was attached to, {@code -1} for the hall and for pieces not joined to it.
 */
public final class MineWingLogic {
	/** The smallest wing worth closing: a lone dead end is not one. */
	public static final int MIN_PIECES = 3;
	/** How deep under the tunnel it branches off a flooded wing has to reach, so its water closes over one's head. */
	public static final int MIN_FLOOD_DEPTH = 3;

	private MineWingLogic() {
	}

	/** Whether {@code piece} lies in the wing that starts at {@code root} (the root itself included). */
	public static boolean inWing(int[] parent, int root, int piece) {
		for (int i = piece; i >= 0; i = parent[i]) {
			if (i == root) {
				return true;
			}
		}
		return false;
	}

	/** How many pieces the wing that starts at {@code root} has. */
	public static int size(int[] parent, int root) {
		int size = 0;
		for (int i = 0; i < parent.length; i++) {
			if (inWing(parent, root, i)) {
				size++;
			}
		}
		return size;
	}

	/**
	 * The pieces a wing can start from: joined to the hall, not the hall itself, at least {@code minPieces} and at most
	 * {@code maxPieces} pieces, and not on the way in from {@code keep} (the dead end the adit replaces), so the mine is
	 * still entered dry and open.
	 */
	public static List<Integer> candidates(int[] parent, int keep, int minPieces, int maxPieces) {
		List<Integer> roots = new ArrayList<>();
		for (int root = 0; root < parent.length; root++) {
			if (parent[root] < 0 || inWing(parent, root, keep)) {
				continue;
			}
			int size = size(parent, root);
			if (size >= minPieces && size <= maxPieces) {
				roots.add(root);
			}
		}
		return roots;
	}

	/** Whether the wings that start at {@code a} and {@code b} share no piece. */
	public static boolean disjoint(int[] parent, int a, int b) {
		return !inWing(parent, a, b) && !inWing(parent, b, a);
	}

	/**
	 * Whether a wing whose lowest piece starts at {@code lowest} floods when the water stands level with
	 * {@code waterLevel}, the floor of the tunnel it branches off.
	 */
	public static boolean floods(int lowest, int waterLevel) {
		return lowest <= waterLevel - MIN_FLOOD_DEPTH;
	}

	/**
	 * How many blocks of rubble lie on the floor of a caved-in wing at ({@code x}, {@code z}): mostly a heap to step
	 * over, now and then a pile to dig through, and here and there the tunnel closed to the roof.
	 */
	public static int rubble(long seed, int x, int z) {
		double heap = smooth(seed, x, z, 4);
		double v = 0.65 * heap + 0.35 * random(seed, x, 0, z, 1);
		if (v < 0.3) {
			return 0;
		}
		if (v < 0.68) {
			return 1;
		}
		return v < 0.82 ? 2 : 3;
	}

	/** A number in [0, 1) for a block and a {@code salt}, the same in every chunk. */
	public static double random(long seed, int x, int y, int z, int salt) {
		long h = seed ^ 0x9E3779B97F4A7C15L * salt;
		h ^= x * 0x632BE59BD9B4E019L;
		h ^= y * 0x8CB92BA72F3D8DD7L;
		h ^= z * 0xC2B2AE3D27D4EB4FL;
		h = (h ^ h >>> 33) * 0xFF51AFD7ED558CCDL;
		h = (h ^ h >>> 33) * 0xC4CEB9FE1A85EC53L;
		h ^= h >>> 33;
		return (h >>> 11) * 0x1.0p-53;
	}

	/** Value noise over cells of {@code cell} blocks, interpolated between the cell corners. */
	private static double smooth(long seed, int x, int z, int cell) {
		int cx = Math.floorDiv(x, cell);
		int cz = Math.floorDiv(z, cell);
		double fx = (double) Math.floorMod(x, cell) / cell;
		double fz = (double) Math.floorMod(z, cell) / cell;
		double a = random(seed, cx, 0, cz, 2);
		double b = random(seed, cx + 1, 0, cz, 2);
		double c = random(seed, cx, 0, cz + 1, 2);
		double d = random(seed, cx + 1, 0, cz + 1, 2);
		double top = a + (b - a) * fx;
		double bottom = c + (d - c) * fx;
		return top + (bottom - top) * fz;
	}
}
