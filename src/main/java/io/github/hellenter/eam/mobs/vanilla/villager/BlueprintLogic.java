package io.github.hellenter.eam.mobs.vanilla.villager;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Pure blueprint rules, unit tested: how a template turns, where it stands and the outline of its ghost. */
public final class BlueprintLogic {
	private BlueprintLogic() {
	}

	/**
	 * Where a template block at {@code x}, {@code z} of a template {@code sizeX} by {@code sizeZ} lands once the
	 * template is turned clockwise {@code turns} quarter turns, its corner kept at the origin: {@code {x, z}}.
	 */
	public static int[] turn(int x, int z, int sizeX, int sizeZ, int turns) {
		return switch (Math.floorMod(turns, 4)) {
			case 1 -> new int[] {sizeZ - 1 - z, x};
			case 2 -> new int[] {sizeX - 1 - x, sizeZ - 1 - z};
			case 3 -> new int[] {z, sizeX - 1 - x};
			default -> new int[] {x, z};
		};
	}

	/** The template's size along x and z once turned: {@code {x, z}}. */
	public static int[] turnedSize(int sizeX, int sizeZ, int turns) {
		return Math.floorMod(turns, 2) == 1 ? new int[] {sizeZ, sizeX} : new int[] {sizeX, sizeZ};
	}

	/**
	 * Where a turned template stands in front of a player: its centre straight ahead of him, as far as keeps its near
	 * side {@code distance} blocks away, and lifted off the ground as he looks up, {@code tan(up) * } its distance, up to
	 * {@code maxLift} blocks. {@code {centreX, centreZ, lift}}, the centre in block coordinates.
	 *
	 * @param yaw   the player's yaw in degrees, 0 south, 90 west
	 * @param pitch the player's pitch in degrees, negative up
	 */
	public static int[] ahead(double x, double z, float yaw, float pitch, int turnedX, int turnedZ, double distance, int maxLift) {
		double rad = Math.toRadians(yaw);
		double dx = -Math.sin(rad);
		double dz = Math.cos(rad);
		double halfX = turnedX / 2.0;
		double halfZ = turnedZ / 2.0;
		double extent = Math.min(Math.abs(dx) < 1.0E-6 ? Double.MAX_VALUE : halfX / Math.abs(dx),
				Math.abs(dz) < 1.0E-6 ? Double.MAX_VALUE : halfZ / Math.abs(dz));
		double reach = distance + extent;
		double up = Math.min(-pitch, 80.0F);
		int lift = up <= 0.0 ? 0 : (int) Math.min(maxLift, Math.round(Math.tan(Math.toRadians(up)) * reach));
		return new int[] {(int) Math.floor(x + dx * reach), (int) Math.floor(z + dz * reach), lift};
	}

	/**
	 * The corner a turned template is placed from so that it stands centred on the block it is set on, its bottom
	 * layer level with that block: the bottom layer is the template's foundation and takes the place of the ground.
	 * {@code {x, y, z}}.
	 */
	public static int[] origin(int lookX, int lookY, int lookZ, int turnedX, int turnedZ) {
		return new int[] {lookX - turnedX / 2, lookY, lookZ - turnedZ / 2};
	}

	/**
	 * A straight edge of a ghost's outline, from {@code x, y, z} along {@code axis} (0 x, 1 y, 2 z) for {@code length}
	 * blocks, in block corner coordinates.
	 */
	public record Edge(int axis, int x, int y, int z, int length) {
	}

	/**
	 * The edges that outline a shape of blocks the way its silhouette and creases show: an edge where the four blocks
	 * around it are not a flat face, so a wall of blocks shows its outline and not every block in it. Edges in a line
	 * are joined into one.
	 */
	public static List<Edge> outline(Set<BlockPos> cells) {
		Set<Edge> units = new HashSet<>();
		int[] cell = new int[3];
		for (BlockPos pos : cells) {
			cell[0] = pos.getX();
			cell[1] = pos.getY();
			cell[2] = pos.getZ();
			for (int axis = 0; axis < 3; axis++) {
				int u = (axis + 1) % 3;
				int v = (axis + 2) % 3;
				for (int du = 0; du <= 1; du++) {
					for (int dv = 0; dv <= 1; dv++) {
						boolean[][] around = new boolean[2][2];
						int count = 0;
						for (int i = 0; i <= 1; i++) {
							for (int j = 0; j <= 1; j++) {
								int[] other = cell.clone();
								other[u] += du - 1 + i;
								other[v] += dv - 1 + j;
								around[i][j] = cells.contains(new BlockPos(other[0], other[1], other[2]));
								if (around[i][j]) count++;
							}
						}
						boolean diagonal = count == 2 && around[0][0] == around[1][1];
						if (count != 1 && count != 3 && !diagonal) continue;
						int[] corner = cell.clone();
						corner[u] += du;
						corner[v] += dv;
						units.add(new Edge(axis, corner[0], corner[1], corner[2], 1));
					}
				}
			}
		}
		return join(units);
	}

	/** Joins unit edges that follow on in a line. */
	private static List<Edge> join(Set<Edge> units) {
		List<Edge> sorted = new ArrayList<>(units);
		sorted.sort(Comparator.comparingInt(Edge::axis).thenComparingInt(edge -> along(edge, (edge.axis() + 1) % 3))
				.thenComparingInt(edge -> along(edge, (edge.axis() + 2) % 3)).thenComparingInt(edge -> along(edge, edge.axis())));
		List<Edge> joined = new ArrayList<>();
		Edge run = null;
		for (Edge edge : sorted) {
			if (run != null && run.axis() == edge.axis() && same(run, edge, (edge.axis() + 1) % 3) && same(run, edge, (edge.axis() + 2) % 3)
					&& along(run, run.axis()) + run.length() == along(edge, edge.axis())) {
				run = new Edge(run.axis(), run.x(), run.y(), run.z(), run.length() + 1);
				continue;
			}
			if (run != null) joined.add(run);
			run = edge;
		}
		if (run != null) joined.add(run);
		return joined;
	}

	private static int along(Edge edge, int axis) {
		return axis == 0 ? edge.x() : axis == 1 ? edge.y() : edge.z();
	}

	private static boolean same(Edge a, Edge b, int axis) {
		return along(a, axis) == along(b, axis);
	}
}
