package io.github.hellenter.eam.content.brewing;

import java.util.ArrayList;
import java.util.List;

/**
 * Lays out a recipe tree for the silver stand's recipe book: leaves take one column each, left to right, and a parent
 * sits over the middle of its children. Pure, unit tested; the client turns columns and depths into pixels.
 */
public final class RecipeTreeLayout {
	private RecipeTreeLayout() {
	}

	/** A laid out node: its column (a half for a parent between two columns) and its depth from the root. */
	public record Placement(double column, int depth) {
	}

	/**
	 * @param parents for each node, the index of its parent or -1 for a root; a parent comes before its children
	 * @return a placement per node, in the same order; roots sit side by side
	 */
	public static List<Placement> layout(int[] parents) {
		int count = parents.length;
		List<List<Integer>> children = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			children.add(new ArrayList<>());
		}
		for (int i = 0; i < count; i++) {
			if (parents[i] >= 0) {
				children.get(parents[i]).add(i);
			}
		}
		double[] columns = new double[count];
		int[] depths = new int[count];
		int[] nextLeaf = {0};
		for (int i = 0; i < count; i++) {
			if (parents[i] < 0) {
				place(i, 0, children, columns, depths, nextLeaf);
			}
		}
		List<Placement> result = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			result.add(new Placement(columns[i], depths[i]));
		}
		return result;
	}

	/** Columns the tree needs: one per leaf. */
	public static int width(int[] parents) {
		boolean[] hasChild = new boolean[parents.length];
		for (int parent : parents) {
			if (parent >= 0) {
				hasChild[parent] = true;
			}
		}
		int leaves = 0;
		for (boolean parent : hasChild) {
			if (!parent) {
				leaves++;
			}
		}
		return leaves;
	}

	private static void place(int node, int depth, List<List<Integer>> children, double[] columns, int[] depths, int[] nextLeaf) {
		depths[node] = depth;
		List<Integer> kids = children.get(node);
		if (kids.isEmpty()) {
			columns[node] = nextLeaf[0]++;
			return;
		}
		for (int child : kids) {
			place(child, depth + 1, children, columns, depths, nextLeaf);
		}
		columns[node] = (columns[kids.getFirst()] + columns[kids.getLast()]) / 2;
	}
}
