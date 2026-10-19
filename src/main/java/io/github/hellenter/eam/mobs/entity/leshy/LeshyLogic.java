package io.github.hellenter.eam.mobs.entity.leshy;

/** The leshy's decisions, free of the game so they can be unit tested. Angles are Minecraft yaws in degrees. */
public final class LeshyLogic {
	/** A point on the ground. */
	public record Point(double x, double z) {
	}

	/** What it does next in a fight. */
	public enum Move {
		/** Close in and claw. */
		MELEE,
		/** Call a line of roots out of the ground at the target. */
		ROOTS,
		/** Call the wolves. */
		WOLVES,
		/** Fly off as a flock of crows to a tree near the target. */
		FLY
	}

	private LeshyLogic() {
	}

	/** Natural leshies appear only once the world is old enough. */
	public static boolean oldEnough(int day, int minDay) {
		return day >= minDay;
	}

	/**
	 * Whether the region holding block ({@code x}, {@code z}) is a leshy's wood. The same world seed always gives the same
	 * answer, so a forest either has its leshy or never will.
	 */
	public static boolean leshyWood(long seed, int x, int z, int regionSize, float chance) {
		long rx = Math.floorDiv(x, regionSize);
		long rz = Math.floorDiv(z, regionSize);
		long h = seed ^ 0x6C65736879L;
		h = mix(h + rx * 0x9E3779B97F4A7C15L);
		h = mix(h + rz * 0xC2B2AE3D27D4EB4FL);
		// The top 24 bits as a number in [0, 1).
		return (h >>> 40) / (float) (1 << 24) < chance;
	}

	private static long mix(long z) {
		z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
		z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
		return z ^ (z >>> 31);
	}

	/** Next to a tree it is not hurt at all, unless it burns. */
	public static boolean shielded(boolean besideTree, boolean burning) {
		return besideTree && !burning;
	}

	/** Damage it takes: more while it burns. */
	public static float damageTaken(float damage, boolean burning, float fireMultiplier) {
		return burning ? damage * fireMultiplier : damage;
	}

	/**
	 * Where the false footsteps sound: {@code distance} from the player at ({@code px}, {@code pz}), on the side away from
	 * the leshy at ({@code lx}, {@code lz}), turned {@code offset} degrees off straight away.
	 */
	public static Point decoy(double px, double pz, double lx, double lz, double distance, float offset) {
		double dx = px - lx;
		double dz = pz - lz;
		double length = Math.sqrt(dx * dx + dz * dz);
		if (length < 1.0E-6) {
			dx = 1.0;
			dz = 0.0;
			length = 1.0;
		}
		double angle = Math.atan2(dz, dx) + Math.toRadians(offset);
		return new Point(px + Math.cos(angle) * distance, pz + Math.sin(angle) * distance);
	}

	/**
	 * A point {@code distance} behind a player at ({@code px}, {@code pz}) looking along {@code yaw}, turned {@code offset}
	 * degrees off straight behind: where the path they came by grows over.
	 */
	public static Point behind(double px, double pz, float yaw, double distance, float offset) {
		// Minecraft yaw 0 looks along +z; the look vector is (-sin, cos).
		double angle = Math.toRadians(yaw + 180.0F + offset);
		return new Point(px - Math.sin(angle) * distance, pz + Math.cos(angle) * distance);
	}

	/**
	 * Its next move in a fight, by the distance to the target and its cooldowns. The flight comes first when the target
	 * got away or it is hurt in the open; then the wolves, when none are left; then the roots at a target out of reach.
	 */
	public static Move move(double distance, boolean fleeHurt, int flyCooldown, double flyDistance, int wolvesCooldown, int wolvesLeft,
			int wolvesCount, int rootCooldown, double rootMin, double rootMax) {
		if (flyCooldown <= 0 && (distance > flyDistance || fleeHurt)) {
			return Move.FLY;
		}
		if (wolvesCooldown <= 0 && wolvesCount > 0 && wolvesLeft == 0) {
			return Move.WOLVES;
		}
		if (rootCooldown <= 0 && distance >= rootMin && distance <= rootMax) {
			return Move.ROOTS;
		}
		return Move.MELEE;
	}

	/**
	 * How many roots a line toward a target {@code distance} away holds, {@code spacing} apart: up to the target and two
	 * past it, at most {@code max}.
	 */
	public static int rootCount(double distance, double spacing, int max) {
		return Math.max(1, Math.min(max, (int) Math.ceil(distance / spacing) + 2));
	}

	/** A heading in degrees that jumps about every half second, as a map's marker does in the Nether. */
	public static float spinningYaw(long gameTime, int seed) {
		long step = gameTime / 10L + seed;
		return (float) Math.floorMod(step * step * 34187121L + step * 121L >> 15, 360L);
	}

	/** Whether a player is under the peace an offering bought until game time {@code peaceUntil}. */
	public static boolean atPeace(long gameTime, long peaceUntil) {
		return gameTime < peaceUntil;
	}
}
