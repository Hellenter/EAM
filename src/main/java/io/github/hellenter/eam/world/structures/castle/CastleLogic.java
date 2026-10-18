package io.github.hellenter.eam.world.structures.castle;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.function.Predicate;

/** Geometry of the castle's site and its markers, apart from the world so it can be tested. */
public final class CastleLogic {
	/** Blocks between two ground samples. */
	public static final int STEP = 8;
	/** How far, in samples, the middle may move off the chunk middle to find flatter ground. */
	public static final int SHIFT = 3;

	private CastleLogic() {
	}

	/** Where to put the castle: its middle {@code dx}, {@code dz} samples off the chunk middle, on {@code ground}. */
	public record Site(int dx, int dz, int ground) {
	}

	/** Samples from the middle of a footprint {@code blocks} wide to past its edge. */
	public static int halfSamples(int blocks) {
		return Math.ceilDiv(blocks / 2, STEP);
	}

	/** Samples from the chunk middle to the edge of the sampled square for a footprint reaching {@code half} samples. */
	public static int reach(int half) {
		return half + SHIFT;
	}

	/**
	 * Picks the flattest dry site among the middles at most {@link #SHIFT} samples off the chunk middle, preferring the
	 * middle on a tie, or none if every one is wet or rises more than {@code maxSlope}. The castle stands on the highest
	 * ground of its footprint, so a slope never buries it.
	 *
	 * @param ground surface height at each sample, {@code [x][z]}, square, the chunk middle in the centre
	 * @param wet    whether each sample is under water
	 * @param halfX  samples from the footprint's middle to past its east and west edges
	 * @param halfZ  samples from the footprint's middle to past its north and south edges
	 */
	public static Site site(int[][] ground, boolean[][] wet, int halfX, int halfZ, int maxSlope) {
		int middle = ground.length / 2;
		Site best = null;
		int bestSpread = Integer.MAX_VALUE;
		int bestShift = Integer.MAX_VALUE;
		for (int dx = -SHIFT; dx <= SHIFT; dx++) {
			for (int dz = -SHIFT; dz <= SHIFT; dz++) {
				int lowest = Integer.MAX_VALUE;
				int highest = Integer.MIN_VALUE;
				boolean dry = true;
				for (int x = dx - halfX; x <= dx + halfX; x++) {
					for (int z = dz - halfZ; z <= dz + halfZ; z++) {
						dry &= !wet[middle + x][middle + z];
						lowest = Math.min(lowest, ground[middle + x][middle + z]);
						highest = Math.max(highest, ground[middle + x][middle + z]);
					}
				}
				int spread = highest - lowest;
				int shift = Math.abs(dx) + Math.abs(dz);
				if (!dry || spread > maxSlope || spread > bestSpread || spread == bestSpread && shift >= bestShift) continue;
				best = new Site(dx, dz, highest);
				bestSpread = spread;
				bestShift = shift;
			}
		}
		return best;
	}

	/**
	 * Whether wool at {@code pos} stands in the open, so it marks a mob: a marker stands on a floor with air or carpet
	 * beside it. Wool laid flush into a floor, like the red pattern in a castle floor, is only wool.
	 */
	public static boolean marksMob(BlockPos pos, Predicate<BlockPos> open) {
		for (Direction side : Direction.Plane.HORIZONTAL) {
			if (open.test(pos.relative(side))) return true;
		}
		return false;
	}
}
