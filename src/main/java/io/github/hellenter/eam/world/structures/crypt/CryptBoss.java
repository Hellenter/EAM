package io.github.hellenter.eam.world.structures.crypt;

import io.github.hellenter.eam.content.EamEntities;
import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLord;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.List;

/**
 * The Fallen Lord lies in the sarcophagus in the middle of the boss room. The room leaves a marker in the sarcophagus;
 * the first time a player walks into the room the marker goes, he breaks the lid and rises.
 */
public final class CryptBoss {
	/** Where he lies in the boss template: the hollow of the sarcophagus, in the middle of the room. */
	static final BlockPos SARCOPHAGUS = new BlockPos(CryptModule.BOSS_SIZE / 2, 4, CryptModule.BOSS_SIZE / 2);
	/** The sarcophagus lid lies on the layer above him, within this many blocks of the middle whichever way it is turned. */
	private static final int LID_REACH = 2;
	/** The boss room's height, in blocks. */
	private static final int ROOM_HEIGHT = 18;
	private static final String MARKER_TAG = "eam.crypt_boss";
	private static final int CHECK_INTERVAL = 20;

	private CryptBoss() {
	}

	static void register() {
		ServerTickEvents.END_LEVEL_TICK.register(level -> {
			if (level.getGameTime() % CHECK_INTERVAL != 0) return;
			for (ServerPlayer player : level.players()) {
				if (!player.isSpectator()) check(level, player);
			}
		});
	}

	/** Called while the boss room is placed, in the chunk that has the sarcophagus. */
	static void placeMarker(WorldGenLevel level, BlockPos sarcophagus) {
		Marker marker = EntityTypes.MARKER.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
		if (marker == null) return;
		marker.snapTo(sarcophagus.getX() + 0.5, sarcophagus.getY(), sarcophagus.getZ() + 0.5);
		marker.addTag(MARKER_TAG);
		level.addFreshEntity(marker);
	}

	/** The boss room around the sarcophagus at {@code sarcophagus}: the same whichever way the room is turned. */
	public static BoundingBox room(BlockPos sarcophagus) {
		BlockPos min = sarcophagus.offset(-SARCOPHAGUS.getX(), -SARCOPHAGUS.getY(), -SARCOPHAGUS.getZ());
		return BoundingBox.fromCorners(min, min.offset(CryptModule.BOSS_SIZE - 1, ROOM_HEIGHT - 1, CryptModule.BOSS_SIZE - 1));
	}

	private static void check(ServerLevel level, ServerPlayer player) {
		List<Marker> markers = level.getEntities(EntityTypes.MARKER,
				player.getBoundingBox().inflate(CryptModule.BOSS_SIZE / 2.0, ROOM_HEIGHT, CryptModule.BOSS_SIZE / 2.0),
				marker -> marker.entityTags().contains(MARKER_TAG));
		for (Marker marker : markers) {
			if (room(marker.blockPosition()).isInside(player.blockPosition())) rise(level, marker);
		}
	}

	private static void rise(ServerLevel level, Marker marker) {
		BlockPos sarcophagus = marker.blockPosition();
		marker.discard();
		for (BlockPos lid : BlockPos.betweenClosed(sarcophagus.offset(-LID_REACH, 1, -LID_REACH), sarcophagus.offset(LID_REACH, 1, LID_REACH))) {
			if (!level.getBlockState(lid).isAir()) level.destroyBlock(lid, false);
		}
		FallenLord boss = EamEntities.FALLEN_LORD.create(level, EntitySpawnReason.TRIGGERED);
		if (boss == null) return;
		boss.snapTo(sarcophagus.getX() + 0.5, sarcophagus.getY(), sarcophagus.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
		boss.setArena(room(sarcophagus));
		boss.finalizeSpawn(level, level.getCurrentDifficultyAt(sarcophagus), EntitySpawnReason.TRIGGERED, null);
		level.addFreshEntity(boss);
	}
}
