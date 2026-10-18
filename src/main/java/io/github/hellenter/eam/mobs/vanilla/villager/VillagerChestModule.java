package io.github.hellenter.eam.mobs.vanilla.villager;

import io.github.hellenter.eam.content.EamBlocks;
import io.github.hellenter.eam.content.lockedchest.LockedChestBlock;
import io.github.hellenter.eam.content.lockedchest.LockedChestBlockEntity;
import io.github.hellenter.eam.content.lockedchest.LockedChestModule;

import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.PackType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Each villager with a job site puts a locked chest of his own beside it, and the elder puts one for the village by his
 * cartography table unless the village has a store already. A villager by his chest puts in what fills his inventory
 * past a few slots, seeds aside, and takes food out when he is hungry and his trade's goods when he has none. Chests a
 * player gives to a villager or to the village count the same. Numbers are in {@code data/eam/villagers/chests.json}.
 */
public final class VillagerChestModule {
	private static volatile ChestSettings settings = ChestSettings.DEFAULT;
	private static boolean enabled;

	private VillagerChestModule() {
	}

	/** Settings load whatever the config says, so the data is always valid. */
	public static void register() {
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(ChestSettingsLoader.ID, new ChestSettingsLoader());
	}

	public static void init() {
		enabled = true;
	}

	static void setSettings(ChestSettings value) {
		settings = value;
	}

	/** The village's store: the locked chests given to the village within {@code radius} of its elder, nearest first. */
	static List<LockedChestBlockEntity> store(ServerLevel level, Villager elder, int radius) {
		if (!enabled) return List.of();
		return LockedChestModule.near(level, elder.blockPosition(), radius, LockedChestBlockEntity::village);
	}

	/** Called every tick a villager thinks on the server: now and then he places his chest or uses it. */
	public static void tick(Villager villager, ServerLevel level) {
		if (!enabled || villager.isBaby() || villager.isSleeping()) return;
		ChestSettings current = settings;
		long time = level.getGameTime();
		boolean elder = VillagerElderModule.isElder(villager);
		if (VillagerRepairLogic.checksNow(time, villager.getId(), current.placeIntervalTicks())) {
			BlockPos jobSite = jobSite(villager, level);
			if (jobSite != null && !hasChest(villager, level, jobSite, elder, current)) place(villager, level, jobSite, elder);
		}
		if (elder || !VillagerRepairLogic.checksNow(time, villager.getId(), current.intervalTicks())) return;
		for (LockedChestBlockEntity chest : LockedChestModule.ownedBy(level, villager.blockPosition(), current.reach() + 1, villager.getUUID())) {
			if (villager.distanceToSqr(Vec3.atCenterOf(chest.getBlockPos())) > current.reach() * current.reach()) continue;
			if (use(villager, chest, current)) {
				villager.getLookControl().setLookAt(Vec3.atCenterOf(chest.getBlockPos()));
				level.playSound(null, chest.getBlockPos(), SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.3F, 1.1F);
			}
			return;
		}
	}

	/** Puts in one stack that fills his inventory past {@code keep_slots}, or takes out one he needs. Whether he did. */
	private static boolean use(Villager villager, LockedChestBlockEntity chest, ChestSettings current) {
		SimpleContainer inventory = villager.getInventory();
		int filled = 0;
		for (ItemStack stack : inventory.getItems()) {
			if (!stack.isEmpty()) filled++;
		}
		if (filled > current.keepSlots()) {
			ItemStack stored = toStore(inventory);
			if (stored != null && chest.insert(stored)) {
				inventory.setChanged();
				return true;
			}
		}
		if (filled >= inventory.getContainerSize()) return false;
		for (int i = 0; i < chest.getContainerSize(); i++) {
			ItemStack stack = chest.getItem(i);
			if (stack.isEmpty()) continue;
			int count = 0;
			if (villager.wantsMoreFood() && stack.has(DataComponents.VILLAGER_FOOD)) {
				count = current.foodFromChest();
			} else if (filled < current.keepSlots() && VillagerGearModule.goods(villager).map(stack::is).orElse(false)
					&& inventory.countItem(stack.getItem()) == 0) {
				count = stack.getCount();
			}
			if (count <= 0) continue;
			ItemStack left = inventory.addItem(stack.split(Math.min(count, stack.getCount())));
			stack.grow(left.getCount());
			chest.setChanged();
			return true;
		}
		return false;
	}

	/** What he puts away first: the last thing that is not seed or food, else the biggest food stack but one. */
	private static @Nullable ItemStack toStore(SimpleContainer inventory) {
		ItemStack food = null;
		int foodStacks = 0;
		for (int i = inventory.getContainerSize() - 1; i >= 0; i--) {
			ItemStack stack = inventory.getItem(i);
			if (stack.isEmpty() || stack.is(ItemTags.VILLAGER_PLANTABLE_SEEDS)) continue;
			if (!stack.has(DataComponents.VILLAGER_FOOD)) return stack;
			foodStacks++;
			if (food == null || stack.getCount() > food.getCount()) food = stack;
		}
		return foodStacks > 1 ? food : null;
	}

	private static @Nullable BlockPos jobSite(Villager villager, ServerLevel level) {
		return villager.getBrain().getMemory(MemoryModuleType.JOB_SITE)
				.filter(site -> site.dimension() == level.dimension())
				.map(GlobalPos::pos).orElse(null);
	}

	/** An elder has one when the village has a store; anyone else when a chest near his job site is his. */
	private static boolean hasChest(Villager villager, ServerLevel level, BlockPos jobSite, boolean elder, ChestSettings current) {
		if (elder) return !store(level, villager, VillagerElderModule.radius()).isEmpty();
		return !LockedChestModule.ownedBy(level, jobSite, current.findRadius(), villager.getUUID()).isEmpty();
	}

	/**
	 * Places a chest on free floor beside the job site, the nearest spot first, its lock toward the villager; never in
	 * a one-block gap or beside a door, so it blocks no way.
	 */
	private static void place(Villager villager, ServerLevel level, BlockPos jobSite, boolean elder) {
		BlockPos best = null;
		double bestDistance = Double.MAX_VALUE;
		for (BlockPos pos : BlockPos.betweenClosed(jobSite.offset(-2, -1, -2), jobSite.offset(2, 1, 2))) {
			if (pos.equals(jobSite) || !free(level, pos) || !level.getEntitiesOfClass(LivingEntity.class, new AABB(pos)).isEmpty()) continue;
			double distance = pos.distSqr(jobSite);
			if (distance < bestDistance) {
				best = pos.immutable();
				bestDistance = distance;
			}
		}
		if (best == null) return;
		Direction facing = Direction.getApproximateNearest(villager.getX() - best.getX() - 0.5, 0.0, villager.getZ() - best.getZ() - 0.5);
		if (facing.getAxis().isVertical()) facing = Direction.NORTH;
		level.setBlockAndUpdate(best, EamBlocks.LOCKED_CHEST.defaultBlockState().setValue(LockedChestBlock.FACING, facing));
		if (level.getBlockEntity(best) instanceof LockedChestBlockEntity chest) chest.claimByVillager(villager.getUUID(), elder);
		level.playSound(null, best, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
		villager.getLookControl().setLookAt(Vec3.atCenterOf(best));
	}

	private static boolean free(ServerLevel level, BlockPos pos) {
		if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) return false;
		if (!level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) return false;
		for (Direction side : Direction.Plane.HORIZONTAL) {
			BlockState next = level.getBlockState(pos.relative(side));
			if (next.is(BlockTags.DOORS) || next.is(BlockTags.FENCE_GATES)) return false;
		}
		// A spot between two blocks on opposite sides is a passage.
		return !(solid(level, pos.north()) && solid(level, pos.south())) && !(solid(level, pos.east()) && solid(level, pos.west()));
	}

	private static boolean solid(ServerLevel level, BlockPos pos) {
		return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
	}
}
