package io.github.hellenter.eam.survival.sleep;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.survival.sleep.SafeRoomLogic.Cell;
import io.github.hellenter.eam.world.mining.HandBreakingModule;

import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.AbstractBedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

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

	private SafeRoomModule() {
	}

	/** Settings load whatever the config says, so the data is always valid. */
	public static void register() {
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(SafeRoomSettingsLoader.ID, new SafeRoomSettingsLoader());
	}

	public static void init() {
		enabled = true;
	}

	/** @return why the player may not sleep in the bed at {@code pos}, or null if the room is safe */
	public static Player.BedSleepingProblem problem(ServerLevel level, BlockState bed, BlockPos pos) {
		if (!enabled) {
			return null;
		}
		BlockPos other = pos.relative(AbstractBedBlock.getConnectedDirection(bed));
		int[][] starts = {{pos.getX(), pos.getY(), pos.getZ()}, {other.getX(), other.getY(), other.getZ()}};
		SafeRoomLogic.Result result = SafeRoomLogic.check(new LevelView(level), starts, settings);
		return switch (result) {
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

	static void setSettings(SafeRoomSettings value) {
		settings = value;
	}

	private record LevelView(ServerLevel level) implements SafeRoomLogic.RoomView {
		@Override
		public Cell cell(int x, int y, int z) {
			BlockPos pos = new BlockPos(x, y, z);
			if (!level.isLoaded(pos)) {
				return Cell.OPEN;
			}
			BlockState state = level.getBlockState(pos);
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
