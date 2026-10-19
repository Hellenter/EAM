package io.github.hellenter.eam.world.structures.mine;

import java.util.List;

/**
 * Where an underworld mine opens into the cave: a straight adit carried on from one of the mine's dead ends until it
 * comes out on the open floor of a cave, read from the bare noise columns (before structures, carvers and features), so
 * every chunk computes the same adit.
 *
 * <p>The mine's pieces are wrapped in rock up to {@link #CLEARANCE} blocks around them (terrain adaptation
 * {@code encapsulate}), so the adit only counts a cave floor that far from every piece, where the cave is left as it was.
 */
public final class UnderworldMineLogic {
	/** Kinds of block in the bare terrain. */
	public static final int SOLID = 0;
	public static final int AIR = 1;
	public static final int WATER = 2;
	/** Lava and anything else nobody digs into. */
	public static final int OTHER = 3;

	/** How far around a piece the terrain closes in: {@code encapsulate} reaches 6 blocks at half scale. */
	public static final int CLEARANCE = 12;
	/** Headroom of the adit above its floor, like the mine's own tunnels. */
	public static final int HEIGHT = 3;
	/** How far above the cave floor an adit has to come out of the wall to end in a tower instead of a staircase. */
	public static final int TOWER_DROP = 6;
	/** The tallest tower: an adit that comes out higher than this over the floor finds another way. */
	public static final int TOWER_MAX = 48;
	/** Cells of the tower along the adit (and across it): three, the ladder in the middle one. */
	public static final int TOWER_SIZE = 3;
	/** {@link Adit#towerBottom()} of an adit that ends on the cave floor. */
	public static final int NO_TOWER = Integer.MIN_VALUE;

	private UnderworldMineLogic() {
	}

	/** The bare terrain: the kind of block at a position. */
	@FunctionalInterface
	public interface Terrain {
		int kind(int x, int y, int z);
	}

	/** A box, both corners inclusive. */
	public record Box(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		boolean contains(int x, int y, int z) {
			return minX <= x && x <= maxX && minY <= y && y <= maxY && minZ <= z && z <= maxZ;
		}

		int distanceSqr(int x, int y, int z) {
			int dx = Math.max(0, Math.max(minX - x, x - maxX));
			int dy = Math.max(0, Math.max(minY - y, y - maxY));
			int dz = Math.max(0, Math.max(minZ - z, z - maxZ));
			return dx * dx + dy * dy + dz * dz;
		}
	}

	/**
	 * An adit: cells {@code 0..length} from {@code start} along ({@code stepX}, {@code stepZ}), three wide. Its floor
	 * starts at {@code startFloor} and climbs or drops one block per cell to {@code endFloor}; cell {@code length} is the
	 * cave floor it comes out on. An adit that comes out of the wall high above the cave floor stays level and ends in a
	 * tower instead: the {@link #TOWER_SIZE} cells after {@code length}, standing on the floor down to
	 * {@code towerBottom}, its top level with the adit.
	 */
	public record Adit(int startX, int startFloor, int startZ, int stepX, int stepZ, int endFloor, int length, int towerBottom) {
		public Adit(int startX, int startFloor, int startZ, int stepX, int stepZ, int endFloor, int length) {
			this(startX, startFloor, startZ, stepX, stepZ, endFloor, length, NO_TOWER);
		}

		public boolean hasTower() {
			return towerBottom != NO_TOWER;
		}

		/** The floor of cell {@code i}. */
		public int floor(int i) {
			return UnderworldMineLogic.floor(startFloor, endFloor, i);
		}

		public int x(int i, int side) {
			return startX + stepX * i - stepZ * side;
		}

		public int z(int i, int side) {
			return startZ + stepZ * i + stepX * side;
		}
	}

	/**
	 * The floor of cell {@code i} of an adit from {@code start} to {@code end}: the first two cells level with the mine,
	 * then one block up or down per cell until the end height.
	 */
	public static int floor(int start, int end, int i) {
		int steps = Math.max(0, i - 1);
		return start + Integer.signum(end - start) * Math.min(Math.abs(end - start), steps);
	}

	/**
	 * Which way a stair in cell {@code i} climbs: {@code 1} along the adit, {@code -1} back towards the mine, {@code 0}
	 * when the cell's floor is a plain block. A stair stands in the higher of two cells whose floors differ.
	 */
	public static int stair(Adit adit, int i) {
		int here = adit.floor(i);
		if (i > 0 && adit.floor(i - 1) < here) {
			return 1;
		}
		if (i < adit.length() && adit.floor(i + 1) < here) {
			return -1;
		}
		return 0;
	}

	/** Whether cell {@code i} gets a timber frame like the mine's corridors: every fifth level cell under rock. */
	public static boolean frame(Adit adit, int i) {
		return i % 5 == 3 && i < adit.length() - 1 && stair(adit, i) == 0
				&& adit.floor(i - 1) == adit.floor(i) && adit.floor(i + 1) == adit.floor(i);
	}

	/**
	 * The adit from a dead end at ({@code x}, {@code floor}, {@code z}) heading ({@code stepX}, {@code stepZ}), or
	 * {@code null} when no open cave floor is within {@code maxLength} cells, or the way there runs into water, lava or
	 * another piece of the mine. {@code pieces} are the mine's pieces without the dead end being replaced.
	 */
	public static Adit find(Terrain terrain, List<Box> pieces, int x, int floor, int z, int stepX, int stepZ,
			int maxLength, int minFloor, int maxFloor) {
		for (int length = 2; length <= maxLength; length++) {
			int bottom = tower(terrain, pieces, x + stepX * length, floor, z + stepZ * length, stepX, stepZ, minFloor);
			if (bottom != NO_TOWER) {
				Adit adit = new Adit(x, floor, z, stepX, stepZ, floor, length - 1, bottom);
				return clear(terrain, pieces, adit) ? adit : null;
			}
			int end = caveFloor(terrain, pieces, x + stepX * length, z + stepZ * length, stepX, stepZ, floor, length - 1,
					minFloor, maxFloor);
			if (end != Integer.MIN_VALUE) {
				Adit adit = new Adit(x, floor, z, stepX, stepZ, end, length);
				return clear(terrain, pieces, adit) ? adit : null;
			}
		}
		return null;
	}

	/**
	 * The floor nearest to {@code floor} (at most {@code reach} above or below) where the cave is open three wide and
	 * far enough from the mine to keep its shape, or {@link Integer#MIN_VALUE}.
	 */
	private static int caveFloor(Terrain terrain, List<Box> pieces, int x, int z, int stepX, int stepZ, int floor,
			int reach, int minFloor, int maxFloor) {
		for (int d = 0; d <= reach; d++) {
			for (int y : d == 0 ? new int[] {floor} : new int[] {floor - d, floor + d}) {
				if (y >= minFloor && y <= maxFloor && openFloor(terrain, pieces, x, y, z, stepX, stepZ)) {
					return y;
				}
			}
		}
		return Integer.MIN_VALUE;
	}

	private static boolean openFloor(Terrain terrain, List<Box> pieces, int x, int y, int z, int stepX, int stepZ) {
		if (terrain.kind(x, y, z) != SOLID) {
			return false;
		}
		for (int side = -1; side <= 1; side++) {
			int sx = x - stepZ * side;
			int sz = z + stepX * side;
			for (int h = 1; h <= HEIGHT; h++) {
				if (terrain.kind(sx, y + h, sz) != AIR) {
					return false;
				}
				for (Box box : pieces) {
					if (box.distanceSqr(sx, y + h, sz) < CLEARANCE * CLEARANCE) {
						return false;
					}
				}
			}
		}
		return true;
	}

	/**
	 * The lowest block of a tower whose near row starts at ({@code x}, {@code z}) heading ({@code stepX},
	 * {@code stepZ}), its top level with {@code floor}, or {@link #NO_TOWER}. A tower stands where the adit comes out of
	 * the wall into open cave (every cell of the tower open from the cave floor up to the adit's headroom, clear of the
	 * mine) at least {@link #TOWER_DROP} and at most {@link #TOWER_MAX} blocks above the floor under the ladder.
	 */
	static int tower(Terrain terrain, List<Box> pieces, int x, int floor, int z, int stepX, int stepZ, int minFloor) {
		int bottom = floor;
		for (int k = 0; k < TOWER_SIZE; k++) {
			for (int side = -1; side <= 1; side++) {
				int cx = x + stepX * k - stepZ * side;
				int cz = z + stepZ * k + stepX * side;
				int y = floor + HEIGHT;
				while (y > floor - TOWER_MAX && y > minFloor && terrain.kind(cx, y, cz) == AIR) {
					for (Box box : pieces) {
						if (box.distanceSqr(cx, y, cz) < CLEARANCE * CLEARANCE) {
							return NO_TOWER;
						}
					}
					y--;
				}
				// The column has to reach below the adit's floor and stand on rock.
				if (y >= floor || terrain.kind(cx, y, cz) != SOLID) {
					return NO_TOWER;
				}
				if (k == 1 && side == 0 && floor - y < TOWER_DROP) {
					return NO_TOWER;
				}
				bottom = Math.min(bottom, y + 1);
			}
		}
		return bottom;
	}

	/**
	 * Whether the adit's floor and headroom stay out of water, lava and the mine's other pieces, and none of its stairs
	 * hangs high over the cave floor (that is what towers are for).
	 */
	private static boolean clear(Terrain terrain, List<Box> pieces, Adit adit) {
		for (int i = 0; i <= adit.length(); i++) {
			int f = adit.floor(i);
			boolean stair = stair(adit, i) != 0;
			for (int side = -1; side <= 1; side++) {
				int x = adit.x(i, side);
				int z = adit.z(i, side);
				if (stair && drop(terrain, x, f, z) >= TOWER_DROP) {
					return false;
				}
				for (int y = f; y <= f + HEIGHT; y++) {
					int kind = terrain.kind(x, y, z);
					if (kind == WATER || kind == OTHER) {
						return false;
					}
					for (Box box : pieces) {
						if (box.contains(x, y, z)) {
							return false;
						}
					}
				}
			}
		}
		return true;
	}

	/** How many blocks of open air there are under a floor block at ({@code x}, {@code y}, {@code z}). */
	private static int drop(Terrain terrain, int x, int y, int z) {
		int drop = 0;
		while (drop < TOWER_DROP && terrain.kind(x, y - drop, z) == AIR) {
			drop++;
		}
		return drop;
	}
}
