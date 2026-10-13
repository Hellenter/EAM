package io.github.hellenter.eam.mobs.entity.lost;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Where each spearman of a shield wall stands. Commanders fighting the same enemy close to each other put all their
 * spearmen in one row; the row is worked out once for everyone against that enemy and kept for a few ticks, so every
 * spearman reads the same row. Server thread only.
 */
final class ShieldWall {
	/** Ticks a worked-out row is kept. */
	private static final int REFRESH_TICKS = 5;
	/** Commanders further than this from the enemy are not counted. */
	private static final double RANGE = 32.0;

	/** Rows against each enemy; forgotten with the enemy. */
	private static final Map<LivingEntity, Rows> ROWS = new WeakHashMap<>();

	private ShieldWall() {
	}

	/** The spot of {@code spearman} in a row against {@code target}, or {@code null} with no commander of his to cover. */
	static @Nullable Vec3 spotOf(ServerLevel level, Lost spearman, LivingEntity target) {
		long now = level.getGameTime();
		Rows rows = ROWS.get(target);
		if (rows == null || now - rows.time >= REFRESH_TICKS || now < rows.time) {
			rows = new Rows(now, work(level, target));
			ROWS.put(target, rows);
		}
		return rows.spots.get(spearman.getUUID());
	}

	private record Rows(long time, Map<UUID, Vec3> spots) {
	}

	/** Groups the commanders against {@code target} by distance and lines up each group's spearmen. */
	private static Map<UUID, Vec3> work(ServerLevel level, LivingEntity target) {
		ShieldWallSettings settings = LostModule.settings().shieldWall();
		List<LostWallCommander> commanders = new ArrayList<>(level.getEntitiesOfClass(LostWallCommander.class,
				target.getBoundingBox().inflate(RANGE), commander -> commander.isAlive() && commander.getTarget() == target));
		commanders.sort(Comparator.comparingInt(LostWallCommander::getId));
		Map<UUID, Vec3> spots = new HashMap<>();
		Set<LostWallCommander> grouped = new LinkedHashSet<>();
		for (LostWallCommander seed : commanders) {
			if (grouped.contains(seed)) {
				continue;
			}
			List<LostWallCommander> group = new ArrayList<>();
			for (LostWallCommander other : commanders) {
				if (!grouped.contains(other) && other.distanceTo(seed) <= settings.joinRange()) {
					group.add(other);
					grouped.add(other);
				}
			}
			line(level, target, group, settings, spots);
		}
		return spots;
	}

	/** One row of all the spearmen of {@code group}'s commanders, in front of the commanders' centre. */
	private static void line(ServerLevel level, LivingEntity target, List<LostWallCommander> group, ShieldWallSettings settings,
			Map<UUID, Vec3> spots) {
		double x = 0.0;
		double y = 0.0;
		double z = 0.0;
		Set<UUID> squads = new LinkedHashSet<>();
		AABB area = null;
		for (LostWallCommander commander : group) {
			x += commander.getX();
			y += commander.getY();
			z += commander.getZ();
			squads.add(commander.getUUID());
			AABB around = commander.getBoundingBox().inflate(Lost.SQUAD_RANGE);
			area = area == null ? around : area.minmax(around);
		}
		x /= group.size();
		y /= group.size();
		z /= group.size();
		List<Lost> members = level.getEntitiesOfClass(Lost.class, area, lost -> lost.isAlive() && lost.role() == LostRole.WALL_SPEARMAN
				&& !lost.defending() && squads.contains(lost.squadId()));
		if (members.isEmpty()) {
			return;
		}
		members.sort(Comparator.comparingInt(Lost::getId));
		double[][] at = new double[members.size()][];
		for (int i = 0; i < at.length; i++) {
			at[i] = new double[] {members.get(i).getX(), members.get(i).getZ()};
		}
		double[][] row = LostLogic.wallSpots(x, z, target.getX(), target.getZ(), at, settings.spacing(), settings.frontDistance());
		for (int i = 0; i < row.length; i++) {
			spots.put(members.get(i).getUUID(), new Vec3(row[i][0], y, row[i][1]));
		}
	}
}
