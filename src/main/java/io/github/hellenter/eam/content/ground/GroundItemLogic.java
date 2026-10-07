package io.github.hellenter.eam.content.ground;

/**
 * Where a loose ground item (pebble, flint shard) lies on top of a block. The top face is split into a
 * 4x4 grid of cells, so there are 16 positions; the item sits in the middle of its cell.
 */
public final class GroundItemLogic {
	/** Cells per side of the grid. */
	public static final int GRID = 4;
	/** Number of positions on a block. */
	public static final int POSITIONS = GRID * GRID;
	/** Size of one cell in model pixels (a block is 16). */
	public static final double CELL = 16.0 / GRID;

	private GroundItemLogic() {
	}

	/** @return the position index for a point on the top face, given as fractions 0..1 of the block. */
	public static int positionAt(double fractionX, double fractionZ) {
		return row(fractionZ) * GRID + column(fractionX);
	}

	/** @return the lowest x in model pixels of an item {@code sizeX} wide at {@code position}. */
	public static double minX(int position, double sizeX) {
		return (position % GRID) * CELL + (CELL - sizeX) / 2;
	}

	/** @return the lowest z in model pixels of an item {@code sizeZ} deep at {@code position}. */
	public static double minZ(int position, double sizeZ) {
		return (position / GRID) * CELL + (CELL - sizeZ) / 2;
	}

	private static int column(double fraction) {
		return Math.clamp((int) Math.floor(fraction * GRID), 0, GRID - 1);
	}

	private static int row(double fraction) {
		return column(fraction);
	}
}
