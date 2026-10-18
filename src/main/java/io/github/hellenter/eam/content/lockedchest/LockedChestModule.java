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

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * Locked chests: the block is registered in {@code EamBlocks}. The player who placed one gives it to a villager near it,
 * or to the village, from the list in its menu; what villagers do with theirs is in {@code VillagerChestModule}.
 */
public final class LockedChestModule {
	/** How far from the chest, in blocks, the villagers its placer can give it to may be. */
	private static final int OWNER_RADIUS = 16;

	public static final ExtendedMenuType<LockedChestMenu, LockedChestMenu.OpenData> MENU = Registry.register(BuiltInRegistries.MENU,
			Eam.id("locked_chest"), new ExtendedMenuType<>(LockedChestMenu::new, LockedChestMenu.OpenData.STREAM_CODEC));

	private LockedChestModule() {
	}

	/**
	 * Finds the villagers of the village a chest stands in, for its owner list; {@code null} when no village claims the
	 * spot. Set by the village elder's module.
	 */
	private static BiFunction<ServerLevel, BlockPos, @Nullable List<Villager>> village = (level, pos) -> null;

	/** Registers the menu and payloads and listens for the owner list; the chest works whatever the config says. */
	public static void register() {
		PayloadTypeRegistry.serverboundPlay().register(LockedChestOwnerPayload.TYPE, LockedChestOwnerPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(LockedChestOwnerNamePayload.TYPE, LockedChestOwnerNamePayload.STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(LockedChestOwnerPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			if (!(player.containerMenu instanceof LockedChestMenu menu) || menu.chest() == null || !menu.data().canChange()) return;
			LockedChestBlockEntity chest = menu.chest();
			ServerLevel level = player.level();
			if (payload.owner() == LockedChestMenu.OpenData.NOBODY) {
				chest.setOwner(null, false);
			} else if (payload.owner() == LockedChestMenu.OpenData.VILLAGE) {
				chest.setOwner(null, true);
			} else {
				for (Villager villager : villagers(level, chest.getBlockPos())) {
					if (villager.getId() == payload.owner()) chest.setOwner(villager.getUUID(), false);
				}
			}
			ServerPlayNetworking.send(player, new LockedChestOwnerNamePayload(menu.containerId, chest.ownerName(level), chest.ownerId(level)));
		});
	}

	/** Sets how the villagers of the village around a chest are found. */
	public static void setVillage(BiFunction<ServerLevel, BlockPos, @Nullable List<Villager>> finder) {
		village = finder;
	}

	/** The villagers a chest at {@code pos} can be given to, for its menu. */
	static List<LockedChestMenu.Candidate> candidates(ServerLevel level, BlockPos pos) {
		List<LockedChestMenu.Candidate> candidates = new ArrayList<>();
		for (Villager villager : villagers(level, pos)) {
			candidates.add(new LockedChestMenu.Candidate(villager.getId(), Optional.ofNullable(villager.getCustomName()),
					villager.getVillagerData().profession().value().name(), villager.getVillagerData().level(),
					(int) Math.round(Math.sqrt(villager.distanceToSqr(Vec3.atCenterOf(pos))))));
		}
		return candidates;
	}

	/** The grown villagers of the village around the chest, or without a village those near it, nearest first. */
	private static List<Villager> villagers(ServerLevel level, BlockPos pos) {
		List<Villager> found = village.apply(level, pos);
		if (found != null) return found;
		List<Villager> villagers = new ArrayList<>(level.getEntitiesOfClass(Villager.class, new AABB(pos).inflate(OWNER_RADIUS),
				villager -> villager.isAlive() && !villager.isBaby()));
		villagers.sort(Comparator.comparingDouble(villager -> villager.distanceToSqr(Vec3.atCenterOf(pos))));
		return villagers;
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
