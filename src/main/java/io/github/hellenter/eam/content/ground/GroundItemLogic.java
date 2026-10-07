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

	/** A ground item's box at one position, in model pixels. */
	public record Box(double minX, double minZ, double maxX, double maxZ, double height) {
	}

	/** How a ground item lies at each of the 16 positions. */
	@FunctionalInterface
	public interface Shape {
		Box at(int position);
	}

	/** A small item {@code sizeX} wide and {@code sizeZ} deep, in the middle of its cell. */
	public static Shape small(double sizeX, double height, double sizeZ) {
		return position -> {
			double x = minX(position, sizeX);
			double z = minZ(position, sizeZ);
			return new Box(x, z, x + sizeX, z + sizeZ, height);
		};
	}

	/**
	 * A thin item as long as the whole block. The row picks how far from the edge it lies; even columns
	 * lie along x and odd ones along z, so half of the positions are turned.
	 */
	public static Shape fullLength(double thickness, double height) {
		return position -> {
			double offset = (position / GRID) * CELL + (CELL - thickness) / 2;
			return position % 2 == 0
					? new Box(0, offset, 16, offset + thickness, height)
					: new Box(offset, 0, offset + thickness, 16, height);
		};
	}

	private static int column(double fraction) {
		return Math.clamp((int) Math.floor(fraction * GRID), 0, GRID - 1);
	}

	private static int row(double fraction) {
		return column(fraction);
	}
}
