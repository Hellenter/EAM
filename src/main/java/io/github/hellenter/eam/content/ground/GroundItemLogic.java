package io.github.hellenter.eam.content.ground;

import java.util.ArrayList;
import java.util.List;

/**
 * Where a loose ground item (pebble, flint shard, stick) lies on top of a block. The top face is split into a
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

	/**
	 * A ground item's box at one position, in model pixels, turned by {@code angle} degrees about the
	 * vertical axis through the middle of the block (the same rotation as a block model element's).
	 */
	public record Box(double minX, double minZ, double maxX, double maxZ, double height, double angle) {
		public Box(double minX, double minZ, double maxX, double maxZ, double height) {
			this(minX, minZ, maxX, maxZ, height, 0);
		}

		/**
		 * @return axis-aligned boxes covering this one, for the outline and hit box: the box itself when it
		 *         is not turned, otherwise one small box around each of {@link #OUTLINE_SEGMENTS} pieces along x.
		 */
		public List<Box> outline() {
			if (angle == 0) {
				return List.of(this);
			}
			double radians = Math.toRadians(angle);
			double cos = Math.cos(radians);
			double sin = Math.sin(radians);
			double step = (maxX - minX) / OUTLINE_SEGMENTS;
			double depth = maxZ - minZ;
			// Half extents of one turned piece, step long and depth wide.
			double halfX = (Math.abs(cos) * step + Math.abs(sin) * depth) / 2;
			double halfZ = (Math.abs(sin) * step + Math.abs(cos) * depth) / 2;
			List<Box> boxes = new ArrayList<>(OUTLINE_SEGMENTS);
			for (int i = 0; i < OUTLINE_SEGMENTS; i++) {
				double x = minX + step * (i + 0.5) - 8;
				double z = (minZ + maxZ) / 2 - 8;
				// Rotation about +y: x' = x cos + z sin, z' = -x sin + z cos.
				double centerX = 8 + x * cos + z * sin;
				double centerZ = 8 - x * sin + z * cos;
				boxes.add(new Box(centerX - halfX, centerZ - halfZ, centerX + halfX, centerZ + halfZ, height));
			}
			return boxes;
		}
	}

	/** Pieces a turned box's outline is made of. */
	public static final int OUTLINE_SEGMENTS = 6;
	/** Length of a stick lying diagonally, short enough to stay inside the block at every offset. */
	public static final double DIAGONAL_LENGTH = 12;
	/** Distance in model pixels between the parallel lines diagonal sticks lie on. */
	public static final double DIAGONAL_SPACING = 2.5;

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
	 * A thin stick. The column picks how it lies: along x, along z (both as long as the whole block), or
	 * diagonally one way or the other; the row picks how far from the middle of the block.
	 */
	public static Shape stick(double thickness, double height) {
		return position -> {
			int row = position / GRID;
			if (position % 4 < 2) {
				double offset = row * CELL + (CELL - thickness) / 2;
				return position % 2 == 0
						? new Box(0, offset, 16, offset + thickness, height)
						: new Box(offset, 0, offset + thickness, 16, height);
			}
			double z = 8 + (row - (GRID - 1) / 2.0) * DIAGONAL_SPACING - thickness / 2;
			double x = 8 - DIAGONAL_LENGTH / 2;
			double angle = position % 2 == 0 ? 45 : -45;
			return new Box(x, z, x + DIAGONAL_LENGTH, z + thickness, height, angle);
		};
	}

	private static int column(double fraction) {
		return Math.clamp((int) Math.floor(fraction * GRID), 0, GRID - 1);
	}

	private static int row(double fraction) {
		return column(fraction);
	}
}
