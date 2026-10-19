package io.github.hellenter.eam.survival.sleep;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.survival.sleep.SafeRoomLogic.Cell;
import io.github.hellenter.eam.world.mining.HandBreakingModule;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.AbstractBedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * A player sleeps through the night only in a safe room: closed, walled with blocks mobs cannot break (planks are
 * fine), lit, and with a door if the room is smaller than 3x3. The check runs once when the player clicks the bed,
 * after vanilla set the spawn point and found it is night. Limits are in {@code data/eam/sleep/safe_room.json}.
 */
public final class SafeRoomModule {
	/** Blocks mobs can break that still make a safe wall: planks, wooden slabs and stairs, trapdoors, fence gates. */
	public static final TagKey<Block> SAFE_WALLS = TagKey.create(Registries.BLOCK, Eam.id("safe_room_walls"));

	private static volatile SafeRoomSettings settings = SafeRoomSettings.DEFAULT;
	private static boolean enabled;

	/** Particles a player sees on the weak spot of the room after a refusal; server-only, not saved. */
	private static final Map<UUID, Marker> MARKERS = new HashMap<>();
	private static final DustParticleOptions MARKER_PARTICLE = new DustParticleOptions(0xFF3020, 1.2F);
	/** How long the weak spot is shown, and how often the particles are sent. */
	private static final int MARKER_TICKS = 100;
	private static final int MARKER_INTERVAL = 5;

	private SafeRoomModule() {
	}

	/** Settings load whatever the config says, so the data is always valid. */
	public static void register() {
		RestedModule.registerTypes();
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(SafeRoomSettingsLoader.ID, new SafeRoomSettingsLoader());
	}

	public static void init() {
		enabled = true;
		RestedModule.init();
		ServerTickEvents.END_SERVER_TICK.register(SafeRoomModule::tickMarkers);
	}

	/**
	 * @return why the player may not sleep in the bed at {@code pos}, or null if the room is safe. On a refusal the weak
	 * spot of the room, if there is one, sparkles red for the player for a few seconds.
	 */
	public static Player.BedSleepingProblem problem(ServerPlayer player, BlockState bed, BlockPos pos) {
		if (!enabled) {
			return null;
		}
		BlockPos other = pos.relative(AbstractBedBlock.getConnectedDirection(bed));
		int[][] starts = {{pos.getX(), pos.getY(), pos.getZ()}, {other.getX(), other.getY(), other.getZ()}};
		SafeRoomLogic.Report report = SafeRoomLogic.inspect(new LevelView(player.level()), starts, settings);
		if (report.result() == SafeRoomLogic.Result.SAFE) {
			RestedModule.sleepsSafely(player);
		} else if (report.spot() != null) {
			MARKERS.put(player.getUUID(), marker(player.level(), report));
		}
		return switch (report.result()) {
			case SAFE -> null;
			case NOT_ENCLOSED -> problem("not_enclosed");
			case WEAK_BLOCKS -> problem("weak_blocks");
			case NEEDS_DOOR -> problem("needs_door");
			case NEEDS_LIGHT -> problem("needs_light");
		};
	}

	private static Player.BedSleepingProblem problem(String key) {
		return new Player.BedSleepingProblem(Component.translatable("block.eam.bed." + key));
	}

	static SafeRoomSettings settings() {
		return settings;
	}

	static void setSettings(SafeRoomSettings value) {
		settings = value;
	}

	/** An open gap sparkles through its middle; a solid weak block on its face toward the room, where it can be seen. */
	private static Marker marker(ServerLevel level, SafeRoomLogic.Report report) {
		int[] spot = report.spot();
		BlockPos pos = new BlockPos(spot[0], spot[1], spot[2]);
		Vec3 center = Vec3.atCenterOf(pos);
		if (level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
			return new Marker(level.dimension(), center, new Vec3(0.25, 0.25, 0.25), MARKER_TICKS);
		}
		int[] from = report.seenFrom();
		Vec3 normal = new Vec3(from[0] - spot[0], from[1] - spot[1], from[2] - spot[2]);
		Vec3 spread = new Vec3(normal.x == 0 ? 0.3 : 0.02, normal.y == 0 ? 0.3 : 0.02, normal.z == 0 ? 0.3 : 0.02);
		return new Marker(level.dimension(), center.add(normal.scale(0.56)), spread, MARKER_TICKS);
	}

	private static void tickMarkers(MinecraftServer server) {
		Iterator<Map.Entry<UUID, Marker>> it = MARKERS.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, Marker> entry = it.next();
			Marker marker = entry.getValue();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player == null || marker.ticksLeft() <= 0 || !player.level().dimension().equals(marker.dimension())) {
				it.remove();
				continue;
			}
			if (marker.ticksLeft() % MARKER_INTERVAL == 0) {
				Vec3 pos = marker.pos();
				Vec3 spread = marker.spread();
				player.level().sendParticles(player, MARKER_PARTICLE, true, true, pos.x, pos.y, pos.z, 8, spread.x, spread.y, spread.z, 0.0);
			}
			entry.setValue(new Marker(marker.dimension(), marker.pos(), marker.spread(), marker.ticksLeft() - 1));
		}
	}

	private record Marker(ResourceKey<Level> dimension, Vec3 pos, Vec3 spread, int ticksLeft) {
	}

	private record LevelView(ServerLevel level) implements SafeRoomLogic.RoomView {
		@Override
		public Cell cell(int x, int y, int z) {
			BlockPos pos = new BlockPos(x, y, z);
			if (!level.isLoaded(pos)) {
				return Cell.OPEN;
			}
			BlockState state = level.getBlockState(pos);
			// An open door, trapdoor or gate lets mobs through, whatever its collision box says.
			if (state.hasProperty(BlockStateProperties.OPEN) && state.getValue(BlockStateProperties.OPEN)) {
				return Cell.OPEN;
			}
			if (state.is(BlockTags.DOORS)) {
				return Cell.DOOR;
			}
			if (state.getCollisionShape(level, pos).isEmpty()) {
				return Cell.OPEN;
			}
			if (state.is(SAFE_WALLS)) {
				return Cell.STRONG;
			}
			// Mobs break what a player breaks bare-handed, and anything that breaks instantly.
			float hardness = state.getDestroySpeed(level, pos);
			boolean weak = hardness >= 0.0F && (hardness == 0.0F || state.is(HandBreakingModule.HAND_BREAKABLE));
			return weak ? Cell.WEAK : Cell.STRONG;
		}

		@Override
		public int blockLight(int x, int y, int z) {
			return level.getBrightness(LightLayer.BLOCK, new BlockPos(x, y, z));
		}
	}
}
