package io.github.hellenter.eam.mobs.vanilla.breaking;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.state.BlockState;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Break progress of blocks mobs are working on, shared by all mobs: two zombies on one block break it twice as fast,
 * and a mob that comes back finds the damage where it was left. Damage heals slowly once nobody works on a block.
 * Kept in memory only; a restart heals everything.
 */
final class BlockDamage {
	private static final Map<ServerLevel, Long2ObjectMap<Entry>> LEVELS = new IdentityHashMap<>();
	/** Crack overlay ids; negative so they never clash with entity ids. */
	private static int nextBreakerId = -1;

	private static final class Entry {
		final BlockState state;
		final int breakerId;
		final int total;
		float done;
		long lastWorked;
		int stage = -1;

		Entry(BlockState state, int total) {
			this.state = state;
			this.breakerId = nextBreakerId--;
			this.total = total;
		}
	}

	private BlockDamage() {
	}

	/** Break ticks already done on the block at {@code pos}. */
	static float done(ServerLevel level, long pos) {
		Long2ObjectMap<Entry> blocks = LEVELS.get(level);
		Entry entry = blocks == null ? null : blocks.get(pos);
		return entry == null ? 0.0F : entry.done;
	}

	/**
	 * One tick of {@code mob} hitting the block. Breaks it when the shared progress is full.
	 *
	 * @param total break ticks the block needs
	 * @return true if the block broke
	 */
	static boolean work(ServerLevel level, BlockPos pos, BlockState state, int total, Mob mob) {
		Long2ObjectMap<Entry> blocks = LEVELS.computeIfAbsent(level, l -> new Long2ObjectOpenHashMap<>());
		long key = pos.asLong();
		Entry entry = blocks.get(key);
		if (entry == null || entry.state != state) {
			if (entry != null) {
				level.destroyBlockProgress(entry.breakerId, pos, -1);
			}
			entry = new Entry(state, total);
			blocks.put(key, entry);
		}
		entry.done += 1.0F;
		entry.lastWorked = level.getGameTime();
		if (entry.done >= entry.total) {
			blocks.remove(key);
			level.destroyBlockProgress(entry.breakerId, pos, -1);
			level.destroyBlock(pos, true, mob);
			return true;
		}
		showStage(level, pos, entry);
		return false;
	}

	/** Heals blocks nobody works on and forgets blocks that changed. Runs once a second per level. */
	static void tick(ServerLevel level, BlockBreakingSettings settings, int elapsed) {
		Long2ObjectMap<Entry> blocks = LEVELS.get(level);
		if (blocks == null || blocks.isEmpty()) {
			return;
		}
		long now = level.getGameTime();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		Iterator<Long2ObjectMap.Entry<Entry>> it = blocks.long2ObjectEntrySet().iterator();
		while (it.hasNext()) {
			Long2ObjectMap.Entry<Entry> mapEntry = it.next();
			Entry entry = mapEntry.getValue();
			pos.set(mapEntry.getLongKey());
			if (!level.isLoaded(pos)) {
				it.remove();
				continue;
			}
			if (level.getBlockState(pos) != entry.state) {
				level.destroyBlockProgress(entry.breakerId, pos, -1);
				it.remove();
				continue;
			}
			entry.done = BlockBreakingLogic.decay(entry.done, now - entry.lastWorked,
					settings.decayDelayTicks(), settings.decayPerTick(), elapsed);
			if (entry.done <= 0.0F) {
				level.destroyBlockProgress(entry.breakerId, pos, -1);
				it.remove();
			} else {
				showStage(level, pos, entry);
			}
		}
	}

	static void clear() {
		LEVELS.clear();
	}

	private static void showStage(ServerLevel level, BlockPos pos, Entry entry) {
		int stage = BlockBreakingLogic.stage(entry.done, entry.total);
		if (stage != entry.stage) {
			entry.stage = stage;
			level.destroyBlockProgress(entry.breakerId, pos, stage);
		}
	}
}
