package io.github.hellenter.eam.world.structures.lostcastle;

import io.github.hellenter.eam.mobs.entity.lost.LostRole;
import io.github.hellenter.eam.world.structures.village.DuergarVillageLogic;

import java.util.function.IntUnaryOperator;

/**
 * Where the Lost castle stands and who holds it, apart from the world so it can be tested. The site is read from the
 * bare noise columns (before carvers and features), so every chunk gets the same answer.
 */
public final class LostCastleLogic {
	/** Blocks between two floor samples across the footprint. */
	public static final int STEP = 6;
	/** How far above or below the middle's floor a sample may find its own and still count as the same floor. */
	public static final int MAX_STEP = 4;
	/** Open air a sample needs over its floor: the castle's walls stand in the cave rather than in the rock. */
	public static final int HEADROOM = 10;
	/** Share of the samples that must be open floor. */
	public static final double MIN_OPEN = 0.85;
	/** Chance a plain Lost of the garrison bears a bow, and else a spear rather than a sword. */
	private static final double ARCHER = 0.3;
	private static final double SPEARMAN = 0.3;

	private LostCastleLogic() {
	}

	/** Reads the kind of block ({@link DuergarVillageLogic#SOLID}, {@code AIR}...) in the column at {@code x}, {@code z}. */
	@FunctionalInterface
	public interface Columns {
		IntUnaryOperator at(int x, int z);
	}

	/** A site: the castle's middle and the floor its bottom layer stands on, and how many samples were open floor. */
	public record Site(int x, int z, int ground, int open) {
	}

	/**
	 * How the footprint {@code half} blocks to each side of {@code x}, {@code z} sits on the floor at {@code floor}:
	 * the samples that are open floor with headroom, and the highest of their floors, which the castle stands on so
	 * none of it is buried. {@code null} if too few samples are open.
	 */
	public static Site site(Columns columns, int x, int z, int floor, int half) {
		int samples = 0;
		int open = 0;
		int ground = floor;
		for (int dx = -half; dx <= half; dx += STEP) {
			for (int dz = -half; dz <= half; dz += STEP) {
				samples++;
				IntUnaryOperator column = columns.at(x + dx, z + dz);
				int y = DuergarVillageLogic.floorNear(column, floor, MAX_STEP);
				if (y == DuergarVillageLogic.NONE || !openAbove(column, y, HEADROOM)) continue;
				open++;
				ground = Math.max(ground, y);
			}
		}
		return open >= Math.ceil(samples * MIN_OPEN) ? new Site(x, z, ground, open) : null;
	}

	/** Whether the {@code height} blocks over {@code y} are air. */
	static boolean openAbove(IntUnaryOperator column, int y, int height) {
		for (int dy = 1; dy <= height; dy++) {
			if (column.applyAsInt(y + dy) != DuergarVillageLogic.AIR) return false;
		}
		return true;
	}

	/** Whether {@code site} beats {@code best}: more open floor, then nearer {@code x}, {@code z}. */
	public static boolean better(Site site, Site best, int x, int z) {
		if (best == null) return true;
		if (site.open() != best.open()) return site.open() > best.open();
		return distance(site, x, z) < distance(best, x, z);
	}

	private static long distance(Site site, int x, int z) {
		long dx = site.x() - x;
		long dz = site.z() - z;
		return dx * dx + dz * dz;
	}

	/** The role of a plain Lost of the garrison for a roll in [0, 1): mostly shield bearers, some archers. */
	public static LostRole garrisonRole(double roll) {
		if (roll < ARCHER) return LostRole.ARCHER;
		if (roll < ARCHER + SPEARMAN) return LostRole.SPEARMAN;
		return LostRole.SWORDSMAN;
	}
}
