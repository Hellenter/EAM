package io.github.hellenter.eam.world.structures.village;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntUnaryOperator;

/**
 * Where a duergar village and each of its pieces stand: on the floor of the cave, read from the bare noise column
 * (before carvers and features), so the answer is the same in every chunk.
 */
public final class DuergarVillageLogic {
	/** Kinds of block in a column. */
	public static final int SOLID = 0;
	public static final int AIR = 1;
	public static final int WATER = 2;
	/** Lava and anything else nobody builds on. */
	public static final int OTHER = 3;
	/** No floor found. */
	public static final int NONE = Integer.MIN_VALUE;

	private DuergarVillageLogic() {
	}

	/**
	 * Whether {@code y} is a floor: solid, with room to stand above it. Shallow water (one block) counts, the lake
	 * villages stand in it on stilts.
	 */
	public static boolean isFloor(IntUnaryOperator column, int y) {
		int above = column.applyAsInt(y + 1);
		return column.applyAsInt(y) == SOLID && (above == AIR || above == WATER)
				&& column.applyAsInt(y + 2) == AIR && column.applyAsInt(y + 3) == AIR;
	}

	/** Every floor from {@code minY} to {@code maxY}, highest first. */
	public static List<Integer> floors(IntUnaryOperator column, int minY, int maxY) {
		List<Integer> floors = new ArrayList<>();
		for (int y = maxY; y >= minY; y--) {
			if (isFloor(column, y)) {
				floors.add(y);
			}
		}
		return floors;
	}

	/** The floor nearest to {@code y}, at most {@code range} above or below, or {@link #NONE}. */
	public static int floorNear(IntUnaryOperator column, int y, int range) {
		for (int d = 0; d <= range; d++) {
			if (isFloor(column, y - d)) {
				return y - d;
			}
			if (d > 0 && isFloor(column, y + d)) {
				return y + d;
			}
		}
		return NONE;
	}

	/** Whether something standing on {@code y} hangs over open air: the two blocks below it are air. */
	public static boolean hangs(IntUnaryOperator column, int y) {
		return column.applyAsInt(y - 1) == AIR && column.applyAsInt(y - 2) == AIR;
	}

	/**
	 * Whether something standing on {@code y} is dug into the ground: the two blocks above it are rock, so the cave floor
	 * around it stands at least two blocks higher and walls it in.
	 */
	public static boolean buried(IntUnaryOperator column, int y) {
		return column.applyAsInt(y + 1) == SOLID && column.applyAsInt(y + 2) == SOLID;
	}

	/** The middle of the values (the lower middle for an even count). */
	public static int median(int[] values) {
		int[] sorted = values.clone();
		Arrays.sort(sorted);
		return sorted[(sorted.length - 1) / 2];
	}

	/**
	 * How far a piece moves: towards the ground under it, but no more than {@code maxStep} above or below the piece it
	 * hangs from, so streets climb and fall with the cave floor a step at a time.
	 */
	public static int step(int wanted, int parent, int maxStep) {
		return Math.max(parent - maxStep, Math.min(parent + maxStep, wanted));
	}
}
