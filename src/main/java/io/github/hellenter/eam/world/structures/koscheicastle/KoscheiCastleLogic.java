package io.github.hellenter.eam.world.structures.koscheicastle;

import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;

/**
 * Where Koschei's castle goes, apart from the world so it can be tested: in the middle of an ice caves patch, read
 * off a grid of biome samples. Each patch is owned by the one cell its middle lies in, so a patch spread over several
 * cells still gets one castle at most.
 */
public final class KoscheiCastleLogic {
	/** Blocks between two biome samples. */
	public static final int SAMPLE = 32;
	/** Samples along a cell's side: a cell is 512 blocks, 32 chunks. */
	public static final int CELL = 16;
	/** A cell looks for patches at every other sample; a patch narrower than that is too small for the castle anyway. */
	private static final int SEED_STEP = 2;
	/** A patch bigger than this many samples is not followed to its end, and gets no castle. */
	private static final int MAX_PATCH = 4096;
	/**
	 * Samples from the middle to the patch's edge, at least, counting the edge sample as 1: two keep the castle's
	 * footprint, 28 blocks either way, inside the ice caves.
	 */
	public static final int MIN_DEPTH = 2;

	/** Whether the sample at {@code x}, {@code z} (sample grid coordinates) lies in the biome. */
	@FunctionalInterface
	public interface Biome {
		boolean in(int x, int z);
	}

	/** A patch of the biome: its middle in sample grid coordinates, how deep inside the patch that is, and its size. */
	public record Patch(int middleX, int middleZ, int depth, int size) {
		/** The middle of the patch in blocks, {x, z}. */
		public int[] block() {
			return new int[] {middleX * SAMPLE + SAMPLE / 2, middleZ * SAMPLE + SAMPLE / 2};
		}
	}

	private KoscheiCastleLogic() {
	}

	/** The biggest patch deep enough for the castle whose middle lies in the cell, or null if there is none. */
	public static @Nullable Patch patch(Biome biome, int cellX, int cellZ) {
		Biome cached = cache(biome);
		Set<Long> seen = new HashSet<>();
		Patch best = null;
		for (int i = 0; i < CELL; i += SEED_STEP) {
			for (int j = 0; j < CELL; j += SEED_STEP) {
				int x = cellX * CELL + i;
				int z = cellZ * CELL + j;
				if (seen.contains(key(x, z)) || !cached.in(x, z)) continue;
				Set<Long> samples = flood(cached, x, z);
				seen.addAll(samples);
				if (samples.size() > MAX_PATCH) continue;
				Patch patch = middle(samples);
				if (Math.floorDiv(patch.middleX(), CELL) != cellX || Math.floorDiv(patch.middleZ(), CELL) != cellZ) continue;
				if (patch.depth() < MIN_DEPTH) continue;
				if (best == null || patch.size() > best.size()) best = patch;
			}
		}
		return best;
	}

	/** Whether the patch with its middle here gets a castle: true for about {@code chance} of patches, the same each time. */
	public static boolean chosen(long seed, int middleX, int middleZ, float chance) {
		SplittableRandom random = new SplittableRandom(seed ^ (middleX * 341873128712L + middleZ * 132897987541L) ^ 0x4B6F73636865694CL);
		return random.nextDouble() < chance;
	}

	/** The samples of the patch holding {@code x}, {@code z}; one more than {@link #MAX_PATCH} when it is bigger. */
	private static Set<Long> flood(Biome biome, int x, int z) {
		Set<Long> samples = new HashSet<>();
		ArrayDeque<long[]> open = new ArrayDeque<>();
		samples.add(key(x, z));
		open.add(new long[] {x, z});
		while (!open.isEmpty() && samples.size() <= MAX_PATCH) {
			long[] at = open.poll();
			for (int[] step : STEPS) {
				int nx = (int) at[0] + step[0];
				int nz = (int) at[1] + step[1];
				if (!samples.contains(key(nx, nz)) && biome.in(nx, nz)) {
					samples.add(key(nx, nz));
					open.add(new long[] {nx, nz});
				}
			}
		}
		return samples;
	}

	/**
	 * The middle of a patch: the sample farthest from its edge, counted in steps through the patch; of several as far,
	 * the nearest to the patch's centre of mass, then the westmost and northmost.
	 */
	static Patch middle(Set<Long> samples) {
		Map<Long, Integer> depth = new HashMap<>();
		ArrayDeque<Long> open = new ArrayDeque<>();
		double sumX = 0.0;
		double sumZ = 0.0;
		for (long sample : samples) {
			int x = x(sample);
			int z = z(sample);
			sumX += x;
			sumZ += z;
			for (int[] step : STEPS) {
				if (!samples.contains(key(x + step[0], z + step[1]))) {
					depth.put(sample, 1);
					open.add(sample);
					break;
				}
			}
		}
		while (!open.isEmpty()) {
			long sample = open.poll();
			int next = depth.get(sample) + 1;
			for (int[] step : STEPS) {
				long near = key(x(sample) + step[0], z(sample) + step[1]);
				if (samples.contains(near) && !depth.containsKey(near)) {
					depth.put(near, next);
					open.add(near);
				}
			}
		}
		double centreX = sumX / samples.size();
		double centreZ = sumZ / samples.size();
		List<Long> sorted = new ArrayList<>(samples);
		sorted.sort((a, b) -> Long.compare(a, b));
		long best = sorted.getFirst();
		for (long sample : sorted) {
			int compare = Integer.compare(depth.get(sample), depth.get(best));
			if (compare == 0) {
				compare = Double.compare(distance(best, centreX, centreZ), distance(sample, centreX, centreZ));
			}
			if (compare == 0) {
				compare = x(sample) != x(best) ? Integer.compare(x(best), x(sample)) : Integer.compare(z(best), z(sample));
			}
			if (compare > 0) best = sample;
		}
		return new Patch(x(best), z(best), depth.get(best), samples.size());
	}

	private static final int[][] STEPS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

	private static double distance(long sample, double x, double z) {
		double dx = x(sample) - x;
		double dz = z(sample) - z;
		return dx * dx + dz * dz;
	}

	/** Remembers each sample read, since following a patch and finding its middle ask for the same samples again. */
	private static Biome cache(Biome biome) {
		Map<Long, Boolean> known = new HashMap<>();
		return (x, z) -> known.computeIfAbsent(key(x, z), k -> biome.in(x, z));
	}

	static long key(int x, int z) {
		return (long) x << 32 | (z & 0xFFFFFFFFL);
	}

	private static int x(long key) {
		return (int) (key >> 32);
	}

	private static int z(long key) {
		return (int) key;
	}
}
