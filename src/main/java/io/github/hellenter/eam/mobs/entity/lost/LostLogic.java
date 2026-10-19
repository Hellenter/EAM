package io.github.hellenter.eam.mobs.entity.lost;

import io.github.hellenter.eam.mobs.entity.knight.SkeletonKnightLogic;
import io.github.hellenter.eam.mobs.entity.knight.SkeletonKnightLogic.Attack;
import io.github.hellenter.eam.mobs.entity.knight.SkeletonKnightSettings;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
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
			case ARCHER, WALL_COMMANDER, LONGBOWMAN -> null;
			case SPEARMAN, CENTURION, WALL_SPEARMAN -> distance <= spearReach ? Attack.POKE : null;
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

	/**
	 * The spots of a shield wall: one row across the line from the commanders' centre to the enemy,
	 * {@code frontDistance} in front of the centre (halfway to a closer enemy), neighbours {@code spacing} apart. Each
	 * member, given as {@code {x, z}}, gets the spot on his own side of the row, so nobody crosses the others' way.
	 * Returns a {@code {x, z}} spot for each member, in the members' order.
	 */
	public static double[][] wallSpots(double centreX, double centreZ, double targetX, double targetZ, double[][] members, double spacing,
			double frontDistance) {
		double dx = targetX - centreX;
		double dz = targetZ - centreZ;
		double length = Math.sqrt(dx * dx + dz * dz);
		if (length < 1.0E-4) {
			dx = 0.0;
			dz = 1.0;
		} else {
			dx /= length;
			dz /= length;
		}
		double along = Math.min(frontDistance, length * 0.5);
		double rowX = centreX + dx * along;
		double rowZ = centreZ + dz * along;
		// Along the row: to the right of someone facing the enemy.
		double sideX = -dz;
		double sideZ = dx;
		int count = members.length;
		Integer[] order = new Integer[count];
		double[] side = new double[count];
		for (int i = 0; i < count; i++) {
			order[i] = i;
			side[i] = (members[i][0] - rowX) * sideX + (members[i][1] - rowZ) * sideZ;
		}
		Arrays.sort(order, (a, b) -> side[a] != side[b] ? Double.compare(side[a], side[b]) : Integer.compare(a, b));
		double[][] spots = new double[count][];
		for (int slot = 0; slot < count; slot++) {
			double offset = (slot - (count - 1) * 0.5) * spacing;
			spots[order[slot]] = new double[] {rowX + sideX * offset, rowZ + sideZ * offset};
		}
		return spots;
	}

	/**
	 * The walking input that moves someone turned to {@code yaw} by {@code offsetX}, {@code offsetZ} without turning:
	 * {forward, left}, as {@code zza} and {@code xxa} of a mob. Its length is the offset's, capped at 1, so the step slows
	 * down near the spot.
	 */
	public static float[] stepInput(float yaw, double offsetX, double offsetZ) {
		double sin = Math.sin(Math.toRadians(yaw));
		double cos = Math.cos(Math.toRadians(yaw));
		double forward = -offsetX * sin + offsetZ * cos;
		double left = offsetX * cos + offsetZ * sin;
		double length = Math.sqrt(forward * forward + left * left);
		double scale = length > 1.0 ? 1.0 / length : 1.0;
		return new float[] {(float) (forward * scale), (float) (left * scale)};
	}

	/** Where the shield wall's commander {@code distance} away from the enemy goes next; he holds still rather than strafe. */
	public static ArcherMove commanderMove(double distance, boolean sees, ShieldWallSettings.Crossbow crossbow) {
		if (distance < crossbow.minRange()) {
			return ArcherMove.RETREAT;
		}
		return !sees || distance > crossbow.maxRange() ? ArcherMove.APPROACH : ArcherMove.HOLD;
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
