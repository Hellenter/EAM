package io.github.hellenter.eam.mobs.vanilla.villager;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.feeder.FeederBlockEntity;
import io.github.hellenter.eam.content.feeder.FeederModule;

import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.PackType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.food.VillagerFood;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * How villagers feed the village. Of the livestock feed a player gives a farmer, half goes straight to the feeders near
 * him and half to his own bank, which he keeps for the villagers: the feeders only take what he has beyond it. A
 * fisherman at work near water catches fish into his bank. Fish are villager food, so like a farmer's bread they go to
 * hungry villagers and let them breed. Numbers are in {@code data/eam/villagers/food.json}.
 */
public final class VillagerFoodModule {
	/** Food points a farmer keeps for the villagers out of what players gave him, beyond the feeders' own reserve. */
	public static final AttachmentType<Integer> BANK = AttachmentRegistry.create(Eam.id("villager_food_bank"),
			builder -> builder.persistent(Codec.INT));

	private static volatile FoodSettings settings = FoodSettings.DEFAULT;
	private static boolean enabled;

	private VillagerFoodModule() {
	}

	/** Settings load whatever the config says, so the data is always valid. */
	public static void register() {
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(FoodSettingsLoader.ID, new FoodSettingsLoader());
	}

	public static void init() {
		enabled = true;
		Map<Item, Integer> fish = Map.of(Items.COD, 2, Items.SALMON, 2, Items.COOKED_COD, 4, Items.COOKED_SALMON, 4);
		DefaultItemComponentEvents.MODIFY.register(context -> context.modify(fish::containsKey,
				(builder, item) -> builder.set(DataComponents.VILLAGER_FOOD, new VillagerFood(fish.get(item)))));
	}

	static void setSettings(FoodSettings value) {
		settings = value;
	}

	/** Livestock feed in the villager's inventory, by item, to compare before and after a player gave some. */
	static Map<Item, Integer> feed(Villager villager) {
		Map<Item, Integer> feed = new HashMap<>();
		for (ItemStack stack : villager.getInventory().getItems()) {
			if (stack.is(FeederModule.FEED)) feed.merge(stack.getItem(), stack.getCount(), Integer::sum);
		}
		return feed;
	}

	/** Villager food points in the inventory. */
	static int points(Villager villager) {
		int points = 0;
		for (ItemStack stack : villager.getInventory().getItems()) {
			VillagerFood food = stack.get(DataComponents.VILLAGER_FOOD);
			if (food != null) points += stack.getCount() * food.nutrition();
		}
		return points;
	}

	/**
	 * A player closed a farmer's inventory: of the feed he added, the livestock share goes to the feeders near the
	 * farmer, and what food is left of the gift joins the farmer's bank.
	 */
	static void afterGiving(Villager villager, Map<Item, Integer> feedBefore, int pointsBefore) {
		if (!enabled || !villager.getVillagerData().profession().is(VillagerProfession.FARMER)
				|| !(villager.level() instanceof ServerLevel level)) {
			return;
		}
		FoodSettings current = settings;
		List<FeederBlockEntity> feeders = feeders(level, villager, current.feederRadius());
		boolean fed = false;
		for (Map.Entry<Item, Integer> entry : feed(villager).entrySet()) {
			int added = entry.getValue() - feedBefore.getOrDefault(entry.getKey(), 0);
			int toLivestock = VillagerFoodLogic.livestockShare(added, current.livestockShare());
			for (FeederBlockEntity feeder : feeders) {
				if (toLivestock <= 0) break;
				int put = feeder.add(entry.getKey(), toLivestock);
				if (put <= 0) continue;
				villager.getInventory().removeItemType(entry.getKey(), put);
				toLivestock -= put;
				fed = true;
			}
		}
		if (fed) level.playSound(null, villager.blockPosition(), SoundEvents.CROP_PLANTED, SoundSource.NEUTRAL, 0.7F, 0.9F);
		int gift = points(villager) - pointsBefore;
		if (gift > 0) villager.setAttached(BANK, bank(villager) + gift);
	}

	/**
	 * Food points the farmer keeps for the villagers out of his bank, never more than he holds of {@code points}: what
	 * he gave away or ate leaves the bank too.
	 */
	public static int farmerKeeps(Villager farmer, int points) {
		if (!enabled) return 0;
		int bank = bank(farmer);
		int kept = VillagerFoodLogic.bankLeft(bank, points);
		if (kept != bank) farmer.setAttached(BANK, kept);
		return kept;
	}

	/** Called every tick a villager thinks on the server: a fisherman at work near water now and then catches a fish. */
	public static void tick(Villager villager, ServerLevel level) {
		if (!enabled) return;
		FoodSettings current = settings;
		if (!VillagerRepairLogic.checksNow(level.getGameTime(), villager.getId(), current.fishIntervalTicks())) return;
		if (villager.isBaby() || !villager.getVillagerData().profession().is(VillagerProfession.FISHERMAN)) return;
		if (!villager.getBrain().isActive(Activity.WORK)) return;
		SimpleContainer inventory = villager.getInventory();
		if (countFish(inventory) >= current.fishMax()) return;
		BlockPos water = water(level, villager.blockPosition(), current.fishWaterRadius(), current.fishWaterBlocks());
		if (water == null) return;
		LootParams params = new LootParams.Builder(level)
				.withParameter(LootContextParams.ORIGIN, villager.position())
				.withParameter(LootContextParams.TOOL, new ItemStack(Items.FISHING_ROD))
				.withOptionalParameter(LootContextParams.THIS_ENTITY, villager)
				.create(LootContextParamSets.FISHING);
		boolean caught = false;
		for (ItemStack stack : level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.FISHING_FISH).getRandomItems(params)) {
			caught |= inventory.addItem(stack).getCount() < stack.getCount();
		}
		if (!caught) return;
		villager.getLookControl().setLookAt(water.getX() + 0.5, water.getY() + 0.5, water.getZ() + 0.5);
		level.playSound(null, water, SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.NEUTRAL, 0.5F, 1.0F);
	}

	private static int bank(Villager villager) {
		Integer bank = villager.getAttached(BANK);
		return bank == null ? 0 : bank;
	}

	private static int countFish(SimpleContainer inventory) {
		int count = 0;
		for (ItemStack stack : inventory.getItems()) {
			if (stack.is(ItemTags.FISHES)) count += stack.getCount();
		}
		return count;
	}

	/** Feeders within {@code radius} of the villager, nearest first. */
	private static List<FeederBlockEntity> feeders(ServerLevel level, Villager villager, int radius) {
		List<FeederBlockEntity> feeders = new ArrayList<>();
		BlockPos center = villager.blockPosition();
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
			if (level.getBlockEntity(pos) instanceof FeederBlockEntity feeder) feeders.add(feeder);
		}
		feeders.sort(Comparator.comparingDouble(feeder -> feeder.getBlockPos().distSqr(center)));
		return feeders;
	}

	/** A water block near {@code center} if there are at least {@code needed} of them within {@code radius}. */
	private static BlockPos water(ServerLevel level, BlockPos center, int radius, int needed) {
		BlockPos found = null;
		int count = 0;
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -3, -radius), center.offset(radius, 1, radius))) {
			if (!level.getFluidState(pos).is(FluidTags.WATER)) continue;
			if (found == null) found = pos.immutable();
			if (++count >= needed) return found;
		}
		return null;
	}
}
