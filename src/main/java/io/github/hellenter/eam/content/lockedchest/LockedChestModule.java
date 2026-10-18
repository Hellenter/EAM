package io.github.hellenter.eam.content.lockedchest;

import io.github.hellenter.eam.Eam;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Locked chests: the block is registered in {@code EamBlocks}. The player who placed one gives it to a villager near it,
 * or to the village, with the button in its menu; what villagers do with theirs is in {@code VillagerChestModule}.
 */
public final class LockedChestModule {
	/** How far from the chest, in blocks, the villagers its placer can give it to may be. */
	private static final int OWNER_RADIUS = 16;

	public static final ExtendedMenuType<LockedChestMenu, LockedChestMenu.OpenData> MENU = Registry.register(BuiltInRegistries.MENU,
			Eam.id("locked_chest"), new ExtendedMenuType<>(LockedChestMenu::new, LockedChestMenu.OpenData.STREAM_CODEC));

	private LockedChestModule() {
	}

	/** Registers the menu and payloads and listens for the owner button; the chest works whatever the config says. */
	public static void register() {
		PayloadTypeRegistry.serverboundPlay().register(LockedChestOwnerPayload.TYPE, LockedChestOwnerPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(LockedChestOwnerNamePayload.TYPE, LockedChestOwnerNamePayload.STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(LockedChestOwnerPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			if (!(player.containerMenu instanceof LockedChestMenu menu) || menu.chest() == null || !menu.data().canChange()) return;
			LockedChestBlockEntity chest = menu.chest();
			nextOwner(chest, player.level());
			ServerPlayNetworking.send(player, new LockedChestOwnerNamePayload(menu.containerId, chest.ownerName(player.level())));
		});
	}

	/** Gives the chest to the next one in turn: nobody, the village, then each grown villager near it, nearest first. */
	private static void nextOwner(LockedChestBlockEntity chest, ServerLevel level) {
		BlockPos pos = chest.getBlockPos();
		List<Villager> villagers = new ArrayList<>(level.getEntitiesOfClass(Villager.class, new AABB(pos).inflate(OWNER_RADIUS),
				villager -> villager.isAlive() && !villager.isBaby()));
		villagers.sort(Comparator.comparingDouble(villager -> villager.distanceToSqr(Vec3.atCenterOf(pos))));
		int current = 0;
		if (chest.village()) {
			current = 1;
		} else if (chest.owner() != null) {
			for (int i = 0; i < villagers.size(); i++) {
				if (villagers.get(i).getUUID().equals(chest.owner())) current = i + 2;
			}
		}
		int next = (current + 1) % (villagers.size() + 2);
		if (next == 0) {
			chest.setOwner(null, false);
		} else if (next == 1) {
			chest.setOwner(null, true);
		} else {
			chest.setOwner(villagers.get(next - 2).getUUID(), false);
		}
	}

	/** The locked chests in loaded chunks within {@code radius} blocks of {@code center} that pass {@code test}. */
	public static List<LockedChestBlockEntity> near(ServerLevel level, BlockPos center, int radius, Predicate<LockedChestBlockEntity> test) {
		List<LockedChestBlockEntity> chests = new ArrayList<>();
		long reach = (long) radius * radius;
		for (int cx = SectionPos.blockToSectionCoord(center.getX() - radius); cx <= SectionPos.blockToSectionCoord(center.getX() + radius); cx++) {
			for (int cz = SectionPos.blockToSectionCoord(center.getZ() - radius); cz <= SectionPos.blockToSectionCoord(center.getZ() + radius); cz++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
				if (chunk == null) continue;
				for (BlockEntity entity : chunk.getBlockEntities().values()) {
					if (entity instanceof LockedChestBlockEntity chest && !chest.isRemoved()
							&& chest.getBlockPos().distSqr(center) <= reach && test.test(chest)) {
						chests.add(chest);
					}
				}
			}
		}
		chests.sort(Comparator.comparingDouble(chest -> chest.getBlockPos().distSqr(center)));
		return chests;
	}

	/** The chests within {@code radius} of {@code center} that {@code villager} owns. */
	public static List<LockedChestBlockEntity> ownedBy(ServerLevel level, BlockPos center, int radius, UUID villager) {
		return near(level, center, radius, chest -> !chest.village() && villager.equals(chest.owner()));
	}
}
