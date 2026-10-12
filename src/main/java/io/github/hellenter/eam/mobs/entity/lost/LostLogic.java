package io.github.hellenter.eam.mobs.entity.lost;

import io.github.hellenter.eam.mobs.entity.knight.SkeletonKnightLogic;
import io.github.hellenter.eam.mobs.entity.knight.SkeletonKnightLogic.Attack;
import io.github.hellenter.eam.mobs.entity.knight.SkeletonKnightSettings;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;

/** The Lost's choices, free of game state so they can be unit tested. */
public final class LostLogic {
	/** Share of the extra followers (after the first shield bearer and the first archer) that are archers, then swordsmen. */
	static final double EXTRA_ARCHER = 0.4;
	static final double EXTRA_SWORDSMAN = 0.7;

	/** Where an archer goes next. */
	public enum ArcherMove {
		/** Too close to the player: back away. */
		RETREAT,
		/** In range and in sight: stand and shoot. */
		HOLD,
		/** Too far or out of sight: walk closer. */
		APPROACH
	}

	private LostLogic() {
	}

	/** How many followers a leader brings, from {@code min} to {@code max}; {@code roll} is in [0, 1). */
	public static int followerCount(int min, int max, double roll) {
		int low = Math.min(min, max);
		int high = Math.max(min, max);
		return low + Math.min(high - low, (int) (roll * (high - low + 1)));
	}

	/** Ticks until the next natural squad may spawn near a player, from a uniform roll in [0, 1): {@code min} to {@code max}. */
	public static int spawnCooldown(int min, int max, double roll) {
		int low = Math.min(min, max);
		return low + (int) ((Math.max(min, max) - low) * roll);
	}

	/**
	 * The followers' roles: the first carries a shield (sword or spear), the second a bow, the rest are mixed. So any
	 * squad of three or more has someone to stand in the player's way and someone to shoot past him.
	 */
	public static List<LostRole> followerRoles(int count, DoubleSupplier random) {
		List<LostRole> roles = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			double roll = random.getAsDouble();
			if (i == 0) {
				roles.add(roll < 0.5 ? LostRole.SWORDSMAN : LostRole.SPEARMAN);
			} else if (i == 1) {
				roles.add(LostRole.ARCHER);
			} else if (roll < EXTRA_ARCHER) {
				roles.add(LostRole.ARCHER);
			} else if (roll < EXTRA_SWORDSMAN) {
				roles.add(LostRole.SWORDSMAN);
			} else {
				roles.add(LostRole.SPEARMAN);
			}
		}
		return roles;
	}

	/**
	 * The attack to start, or {@code null} while the target is out of reach. A spearman only jabs; swordsmen and the
	 * leader fight like the skeleton knight, with their own numbers.
	 */
	public static @Nullable Attack choose(LostRole role, double distance, boolean targetBlocking, int comboCooldown, int heavyCooldown,
			SkeletonKnightSettings settings, double spearReach, double heavyRoll, double comboRoll) {
		return switch (role) {
			case ARCHER -> null;
			case SPEARMAN, CENTURION -> distance <= spearReach ? Attack.POKE : null;
			default -> SkeletonKnightLogic.choose(distance, targetBlocking, comboCooldown, heavyCooldown, settings, heavyRoll, comboRoll);
		};
	}

	/**
	 * Where a shield bearer stands to cover his archers: {@code distance} from the target towards them, moved
	 * {@code side} blocks sideways so two guards stand next to each other rather than in one spot. Returns
	 * {@code {x, z}}; with the archers right on the target, the target's own position.
	 */
	public static double[] guardPoint(double targetX, double targetZ, double archersX, double archersZ, double distance, double side) {
		double dx = archersX - targetX;
		double dz = archersZ - targetZ;
		double length = Math.sqrt(dx * dx + dz * dz);
		if (length < 1.0E-4) {
			return new double[] {targetX, targetZ};
		}
		dx /= length;
		dz /= length;
		// Never past the archers themselves.
		double along = Math.min(distance, length);
		return new double[] {targetX + dx * along - dz * side, targetZ + dz * along + dx * side};
	}

	/** Where an archer {@code distance} away from the player goes next. */
	public static ArcherMove archerMove(double distance, boolean sees, LostSettings.Archer archer) {
		if (distance < archer.minRange()) {
			return ArcherMove.RETREAT;
		}
		return !sees || distance > archer.maxRange() ? ArcherMove.APPROACH : ArcherMove.HOLD;
	}

	/**
	 * Turns a yaw {@code current} towards {@code target} by at most {@code step} degrees, all the way once it is within
	 * {@code step}. Unlike {@code Mth.rotateIfNecessary}, which only keeps an angle within {@code step} of the target and
	 * so can leave an archer aiming 13-40 degrees off forever.
	 */
	public static float turnTowards(float current, float target, float step) {
		float difference = (float) (((target - current) % 360.0 + 540.0) % 360.0 - 180.0);
		return current + Math.max(-step, Math.min(step, difference));
	}
}
