package io.github.hellenter.eam.mobs.vanilla.breaking;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Finds the cheapest way for a mob to reach its target when walls are in the way: a bounded A* over block cells
 * where breaking a block costs the ticks it takes to break and walking a block costs a few ticks. Pure logic over a
 * {@link Terrain}, so it is unit tested without a world.
 *
 * <p>A node is the cell the mob's feet are in; the mob is {@code height} cells tall. Moves go to the four horizontal
 * neighbours: flat (breaking the body cells), a step up (also breaking the head cell above the mob), a step down
 * (breaking the neighbour's floor and body cells) and a drop of up to three blocks. The mob never breaks the block it
 * stands on and never digs straight up or down.
 */
public final class BreachPlanner {
	/** Nothing to collide with: the mob can walk through, it cannot stand on it. */
	public static final int AIR = 0;
	/** A solid block mobs cannot break. */
	public static final int SOLID = -1;
	/** A cell the mob must never enter: lava, fire, unloaded chunks. */
	public static final int BLOCKED = -2;

	/** Cells around the mob as the planner sees them. */
	@FunctionalInterface
	public interface Terrain {
		/** {@link #AIR}, {@link #SOLID}, {@link #BLOCKED}, or a positive number: ticks left to break this solid block. */
		int cell(int x, int y, int z);
	}

	/** One move of the route: where the mob's feet end up and which blocks it breaks first (top to bottom). */
	public record Step(long feet, long[] toBreak) {
	}

	/**
	 * @param steps    the route from the mob's position, first move first
	 * @param reaches  the route ends next to the target, not at the closest cell the search found
	 * @param expanded nodes the search expanded, to measure its cost
	 */
	public record Plan(List<Step> steps, boolean reaches, int expanded) {
		/** Index of the first step that breaks something, or -1 when the route is open. */
		public int firstBreakingStep() {
			for (int i = 0; i < steps.size(); i++) {
				if (steps.get(i).toBreak().length > 0) {
					return i;
				}
			}
			return -1;
		}
	}

	private static final int[][] DIRECTIONS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
	private static final int MAX_DROP = 3;
	private static final long[] NO_BREAKS = new long[0];

	private final int maxNodes;
	private final int radius;
	private final int walkTicks;
	private final int distanceTicks;
	/** Reused by every expansion; the planner runs on the server thread only. */
	private final LongArrayList breaks = new LongArrayList(8);

	/**
	 * @param maxNodes        most nodes one search expands
	 * @param radius          the search stays within this many blocks of the mob on each axis
	 * @param walkTicks       cost of walking one block, in ticks
	 * @param distanceTicks   what one block of distance to the target is estimated to cost, in ticks. Far above
	 *                        {@code walkTicks} on purpose: the search heads for the target instead of trying every
	 *                        detour that is cheaper than breaking a wall, which keeps it small
	 */
	public BreachPlanner(int maxNodes, int radius, int walkTicks, int distanceTicks) {
		this.maxNodes = maxNodes;
		this.radius = radius;
		this.walkTicks = walkTicks;
		this.distanceTicks = distanceTicks;
	}

	private static final class Node implements Comparable<Node> {
		final int x;
		final int y;
		final int z;
		final int g;
		final int f;
		final Node parent;
		final long[] toBreak;

		Node(int x, int y, int z, int g, int h, Node parent, long[] toBreak) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.g = g;
			this.f = g + h;
			this.parent = parent;
			this.toBreak = toBreak;
		}

		@Override
		public int compareTo(Node other) {
			return Integer.compare(f, other.f);
		}
	}

	/** Plans a route for a mob {@code height} cells tall from its feet cell to the target's feet cell. */
	public Plan plan(Terrain world, int sx, int sy, int sz, int tx, int ty, int tz, int height) {
		Terrain terrain = cached(world);
		Long2IntOpenHashMap bestG = new Long2IntOpenHashMap();
		bestG.defaultReturnValue(Integer.MAX_VALUE);
		PriorityQueue<Node> open = new PriorityQueue<>();

		Node start = new Node(sx, sy, sz, 0, heuristic(sx, sy, sz, tx, ty, tz), null, NO_BREAKS);
		open.add(start);
		bestG.put(BlockPos.asLong(sx, sy, sz), 0);
		Node closest = start;
		int closestDistance = distance(sx, sy, sz, tx, ty, tz);
		int expanded = 0;

		while (!open.isEmpty() && expanded < maxNodes) {
			Node node = open.poll();
			if (node.g > bestG.get(BlockPos.asLong(node.x, node.y, node.z))) {
				continue; // A cheaper way here was already expanded.
			}
			expanded++;
			if (reaches(terrain, node.x, node.y, node.z, tx, ty, tz)) {
				return new Plan(route(node), true, expanded);
			}
			int d = distance(node.x, node.y, node.z, tx, ty, tz);
			if (d < closestDistance || d == closestDistance && node.g < closest.g) {
				closest = node;
				closestDistance = d;
			}
			for (int[] dir : DIRECTIONS) {
				expand(terrain, node, node.x + dir[0], node.z + dir[1], height, sx, sy, sz, tx, ty, tz, bestG, open);
			}
		}
		return new Plan(route(closest), false, expanded);
	}

	private void expand(Terrain t, Node from, int nx, int nz, int height, int sx, int sy, int sz,
			int tx, int ty, int tz, Long2IntOpenHashMap bestG, PriorityQueue<Node> open) {
		if (Math.abs(nx - sx) > radius || Math.abs(nz - sz) > radius) {
			return;
		}
		int y = from.y;
		LongArrayList breaks = this.breaks;
		breaks.clear();

		// Flat move, or a drop when there is no floor.
		if (clear(t, nx, y, nz, height, breaks)) {
			int floor = t.cell(nx, y - 1, nz);
			if (isFloor(floor)) {
				add(t, from, nx, y, nz, walkTicks, breaks, tx, ty, tz, bestG, open);
			} else if (floor == AIR) {
				int landing = y - 1;
				while (landing > y - 1 - MAX_DROP && t.cell(nx, landing - 1, nz) == AIR) {
					landing--;
				}
				if (landing > y - 1 - MAX_DROP && isFloor(t.cell(nx, landing - 1, nz))) {
					add(t, from, nx, landing, nz, walkTicks * (1 + y - landing), breaks, tx, ty, tz, bestG, open);
				}
			}
		}

		// Step up onto the neighbour block, breaking the cell above the mob's head first.
		breaks.clear();
		if (Math.abs(y + 1 - sy) <= radius && isFloor(t.cell(nx, y, nz))
				&& clear(t, from.x, y + height, from.z, 1, breaks) && clear(t, nx, y + 1, nz, height, breaks)) {
			add(t, from, nx, y + 1, nz, walkTicks * 2, breaks, tx, ty, tz, bestG, open);
		}

		// Step down by breaking the neighbour's floor too.
		breaks.clear();
		if (Math.abs(y - 1 - sy) <= radius && t.cell(nx, y - 1, nz) != AIR
				&& clear(t, nx, y - 1, nz, height + 1, breaks) && isFloor(t.cell(nx, y - 2, nz))) {
			add(t, from, nx, y - 1, nz, walkTicks * 2, breaks, tx, ty, tz, bestG, open);
		}
	}

	private void add(Terrain t, Node from, int x, int y, int z, int moveCost, LongArrayList breaks,
			int tx, int ty, int tz, Long2IntOpenHashMap bestG, PriorityQueue<Node> open) {
		int g = from.g + moveCost;
		long[] toBreak = breaks.isEmpty() ? NO_BREAKS : new long[breaks.size()];
		for (int i = 0; i < toBreak.length; i++) {
			long cell = breaks.getLong(toBreak.length - 1 - i); // Top first, so the mob does not fall into the hole.
			toBreak[i] = cell;
			g += breakCost(t, from, cell);
		}
		long key = BlockPos.asLong(x, y, z);
		if (g < bestG.get(key)) {
			bestG.put(key, g);
			open.add(new Node(x, y, z, g, heuristic(x, y, z, tx, ty, tz), from, toBreak));
		}
	}

	/** Break cost of a cell; a cell the route already broke on an earlier step is free. */
	private static int breakCost(Terrain t, Node from, long cell) {
		for (Node n = from; n != null; n = n.parent) {
			for (long broken : n.toBreak) {
				if (broken == cell) {
					return 0;
				}
			}
		}
		return t.cell(BlockPos.getX(cell), BlockPos.getY(cell), BlockPos.getZ(cell));
	}

	/** Collects the cells to break so {@code count} cells upward from (x, y, z) are passable; false if one cannot be. */
	private static boolean clear(Terrain t, int x, int y, int z, int count, LongArrayList breaks) {
		for (int i = 0; i < count; i++) {
			int cell = t.cell(x, y + i, z);
			if (cell < 0) {
				return false;
			}
			if (cell > 0) {
				breaks.add(BlockPos.asLong(x, y + i, z));
			}
		}
		return true;
	}

	private static boolean isFloor(int cell) {
		return cell == SOLID || cell > 0;
	}

	/**
	 * The mob can hit the target from here: next to it on the same level, or one block higher or lower when the cell
	 * its line of sight crosses is open. A mob on a ledge above a player in a one-block hole has the roof over the
	 * player's head between their eyes, and one in a hole below has the block above its own head in the way.
	 */
	private static boolean reaches(Terrain t, int x, int y, int z, int tx, int ty, int tz) {
		if (Math.abs(x - tx) + Math.abs(z - tz) > 1) {
			return false;
		}
		return switch (y - ty) {
			case 0 -> true;
			case 1 -> t.cell(tx, ty + 2, tz) == AIR;
			case -1 -> t.cell(x, y + 2, z) == AIR;
			default -> false;
		};
	}

	private static int distance(int x, int y, int z, int tx, int ty, int tz) {
		return Math.abs(x - tx) + Math.abs(y - ty) + Math.abs(z - tz);
	}

	private int heuristic(int x, int y, int z, int tx, int ty, int tz) {
		return distance(x, y, z, tx, ty, tz) * distanceTicks;
	}

	private static List<Step> route(Node end) {
		List<Step> steps = new ArrayList<>();
		for (Node n = end; n.parent != null; n = n.parent) {
			steps.add(new Step(BlockPos.asLong(n.x, n.y, n.z), n.toBreak));
		}
		return steps.reversed();
	}

	/** Each cell is read from the world once per plan. */
	private static Terrain cached(Terrain world) {
		Long2IntOpenHashMap cache = new Long2IntOpenHashMap();
		cache.defaultReturnValue(Integer.MIN_VALUE);
		return (x, y, z) -> {
			long key = BlockPos.asLong(x, y, z);
			int value = cache.get(key);
			if (value == Integer.MIN_VALUE) {
				value = world.cell(x, y, z);
				cache.put(key, value);
			}
			return value;
		};
	}
}
