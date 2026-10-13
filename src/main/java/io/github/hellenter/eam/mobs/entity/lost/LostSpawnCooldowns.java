package io.github.hellenter.eam.mobs.entity.lost;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * After a squad of the Lost spawns naturally near a player, the caves around that player stay quiet for a while, so
 * a slain squad is not replaced at once. Kept in memory only: a restart
 * simply ends every rest, and so does a server stop (a singleplayer game leaving one world for another).
 */
final class LostSpawnCooldowns {
	/** Natural spawns happen within 128 blocks of a player; the nearest one within that range owns the rest. */
	private static final double PLAYER_RANGE = 128.0;
	/** Game time at which each player's rest ends. */
	private static final Map<UUID, Long> REST_UNTIL = new ConcurrentHashMap<>();

	private LostSpawnCooldowns() {
	}

	/** Whether a squad may spawn here: the nearest player is not resting from the last one. */
	static boolean ready(ServerLevel level, BlockPos pos) {
		Player player = level.getNearestPlayer(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, PLAYER_RANGE, false);
		return player == null || level.getGameTime() >= REST_UNTIL.getOrDefault(player.getUUID(), Long.MIN_VALUE);
	}

	/** The server stopped: every rest ends. */
	static void clear() {
		REST_UNTIL.clear();
	}

	/** A squad has just spawned here: the nearest player gets a rest. */
	static void start(ServerLevel level, BlockPos pos, LostSettings.Squad squad, RandomSource random) {
		Player player = level.getNearestPlayer(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, PLAYER_RANGE, false);
		if (player != null) {
			int ticks = LostLogic.spawnCooldown(squad.spawnCooldownMin(), squad.spawnCooldownMax(), random.nextDouble());
			REST_UNTIL.put(player.getUUID(), level.getGameTime() + ticks);
		}
	}
}
