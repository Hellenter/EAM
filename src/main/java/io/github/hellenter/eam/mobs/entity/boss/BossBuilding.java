package io.github.hellenter.eam.mobs.entity.boss;

import io.github.hellenter.eam.Eam;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;

import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Remembers the blocks players put up around a fighting boss (a pillar to stand on, a wall to hide behind) so the boss
 * may take them down again, and only them: the world around stays whole. Nothing is saved; a block counts for
 * {@code keep_ticks} after it was placed. Numbers are in {@code data/eam/mobs/boss_building.json}.
 */
public final class BossBuilding {
	private static final Identifier ID = Eam.id("boss_building_settings");
	private static final Identifier SETTINGS = Eam.id("boss_building");
	/** Past this many remembered blocks in a level, the forgotten ones are swept out. */
	private static final int SWEEP_AT = 2048;

	private static volatile BuildingRules rules = BuildingRules.DEFAULT;
	/** Per level: packed block position to the block put there and the game time it is forgotten at. */
	private static final Map<ResourceKey<Level>, Long2ObjectOpenHashMap<Placed>> PLACED = new HashMap<>();
	private static boolean registered;

	private BossBuilding() {
	}

	/** Called by every boss module that breaks player-placed blocks; registers the loader once. */
	public static synchronized void register() {
		if (registered) {
			return;
		}
		registered = true;
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(ID, new Loader());
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> PLACED.clear());
	}

	public static BuildingRules rules() {
		return rules;
	}

	/** A player placed a block: remembered when a boss near it is fighting. */
	public static void placed(Level level, BlockPos pos, @Nullable Player player, boolean placed) {
		if (!placed || !registered || player == null || !(level instanceof ServerLevel server)) {
			return;
		}
		double radius = rules.radius();
		AABB around = new AABB(pos).inflate(radius);
		if (server.getEntitiesOfClass(Mob.class, around, mob -> mob instanceof BuildingAware boss && boss.isFighting()).isEmpty()) {
			return;
		}
		Long2ObjectOpenHashMap<Placed> remembered = PLACED.computeIfAbsent(server.dimension(), key -> new Long2ObjectOpenHashMap<>());
		long now = server.getGameTime();
		if (remembered.size() >= SWEEP_AT) {
			remembered.values().removeIf(entry -> !BuildingRules.remembered(entry.until(), now));
		}
		remembered.put(pos.asLong(), new Placed(server.getBlockState(pos).getBlock(), now + rules.keepTicks()));
	}

	/** Whether a player put up the block at {@code pos} by a fighting boss a short while ago, and it still stands. */
	public static boolean isPlaced(Level level, BlockPos pos) {
		Long2ObjectOpenHashMap<Placed> remembered = PLACED.get(level.dimension());
		Placed entry = remembered == null ? null : remembered.get(pos.asLong());
		if (entry == null || !BuildingRules.remembered(entry.until(), level.getGameTime())) {
			return false;
		}
		if (!level.getBlockState(pos).is(entry.block())) {
			// Mined, or something else stands there now.
			remembered.remove(pos.asLong());
			return false;
		}
		return true;
	}

	/** Whether bosses may break blocks here at all: the mob griefing rule. */
	public static boolean mayBreak(ServerLevel level) {
		return level.getGameRules().get(GameRules.MOB_GRIEFING);
	}

	/**
	 * Breaks the player-placed block at {@code pos}, if it is one; with {@code topple} the player-placed blocks stacked
	 * on it fall too: a pillar cut at its foot comes down with whoever stands on it. A wall is broken without it, or
	 * its upper rows would drop into the gap. True if it broke.
	 */
	public static boolean breakPlaced(ServerLevel level, BlockPos pos, Entity breaker, boolean topple) {
		if (!mayBreak(level) || !isPlaced(level, pos)) {
			return false;
		}
		forget(level, pos);
		level.destroyBlock(pos, true, breaker);
		if (topple) {
			topple(level, pos.above(), breaker);
		}
		return true;
	}

	/** Breaks every player-placed block in {@code area}, toppling what stands on them if asked; how many it broke. */
	public static int breakPlacedIn(ServerLevel level, AABB area, Entity breaker, boolean topple) {
		int broken = 0;
		for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(area.minX, area.minY, area.minZ),
				BlockPos.containing(area.maxX, area.maxY - 1.0E-4, area.maxZ))) {
			if (breakPlaced(level, pos.immutable(), breaker, topple)) {
				broken++;
			}
		}
		return broken;
	}

	/** Whether {@code area} holds any player-placed block. */
	public static boolean anyPlacedIn(Level level, AABB area) {
		for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(area.minX, area.minY, area.minZ),
				BlockPos.containing(area.maxX, area.maxY - 1.0E-4, area.maxZ))) {
			if (isPlaced(level, pos)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Whether every block that collides in {@code area} was placed by a player, and there is at least one: a wall the boss
	 * can go through rather than stop at.
	 */
	public static boolean onlyPlacedIn(Level level, AABB area) {
		boolean any = false;
		for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(area.minX, area.minY, area.minZ),
				BlockPos.containing(area.maxX, area.maxY - 1.0E-4, area.maxZ))) {
			if (level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
				continue;
			}
			if (!isPlaced(level, pos)) {
				return false;
			}
			any = true;
		}
		return any;
	}

	/** The player-placed blocks from {@code from} up come down as falling blocks; ones that hold things just break. */
	private static void topple(ServerLevel level, BlockPos from, Entity breaker) {
		BlockPos.MutableBlockPos pos = from.mutable();
		for (int i = 0; i < rules.topple() && isPlaced(level, pos); i++, pos.move(0, 1, 0)) {
			BlockPos at = pos.immutable();
			BlockState state = level.getBlockState(at);
			forget(level, at);
			if (state.hasBlockEntity() || state.getCollisionShape(level, at).isEmpty()) {
				level.destroyBlock(at, true, breaker);
			} else {
				FallingBlockEntity.fall(level, at, state);
			}
		}
	}

	private static void forget(Level level, BlockPos pos) {
		Long2ObjectOpenHashMap<Placed> remembered = PLACED.get(level.dimension());
		if (remembered != null) {
			remembered.remove(pos.asLong());
		}
	}

	/** A block a player put up, and the game time it is forgotten at. */
	private record Placed(Block block, long until) {
	}

	private static final class Loader extends SimpleJsonResourceReloadListener<BuildingRules> {
		Loader() {
			super(BuildingRules.CODEC, FileToIdConverter.json("mobs"));
		}

		@Override
		protected void apply(Map<Identifier, BuildingRules> files, ResourceManager manager, ProfilerFiller profiler) {
			rules = files.getOrDefault(SETTINGS, BuildingRules.DEFAULT);
		}
	}
}
