package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.content.EamItems;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.SimpleFabricLootTableSubProvider;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.entries.UniformContainerBase;
import net.minecraft.world.level.storage.loot.functions.EnchantWithLevelsFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemDamageFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.floats.ContextFloatProviders;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * Abandoned camps (the vanilla tents) and shipwrecks hold only early game things instead of vanilla's iron, gold and
 * diamonds: simple food, sticks and string, damaged flint tools and slingshots, up to 3 copper or silver nuggets, now
 * and then a couple of emeralds, a worn bow or a book with a level I enchantment from the enchanting table. Shipwreck
 * map chests keep vanilla's table.
 */
public class EamEarlyLootProvider extends SimpleFabricLootTableSubProvider {
	/** Enchanting levels for books: low enough that nearly every enchantment comes out at level I. */
	private static final int BOOK_MIN_LEVELS = 1;
	private static final int BOOK_MAX_LEVELS = 5;

	private final CompletableFuture<HolderLookup.Provider> registries;
	private HolderGetter<Enchantment> enchantments;

	public EamEarlyLootProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries, LootContextParamSets.CHEST);
		this.registries = registries;
	}

	@Override
	public String getName() {
		return "Early structure loot tables";
	}

	/** Vanilla's entry point; Fabric's datagen calls {@link #generate(BiConsumer)} instead. */
	@Override
	public void run() {
	}

	@Override
	public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
		enchantments = registries.join().lookupOrThrow(Registries.ENCHANTMENT);
		output.accept(BuiltInLootTables.ABANDONED_CAMP_COMMON_CHEST, campChest());
		output.accept(BuiltInLootTables.ABANDONED_CAMP_SECRET_CHEST, campSecretChest());
		output.accept(BuiltInLootTables.ABANDONED_CAMP_BARREL, campBarrel());
		output.accept(BuiltInLootTables.SHIPWRECK_SUPPLY, shipwreckSupply());
		output.accept(BuiltInLootTables.SHIPWRECK_TREASURE, shipwreckTreasure());
	}

	private LootTable.Builder campChest() {
		return LootTable.lootTable()
				.withPool(food(2, 4))
				.withPool(supplies(1, 3))
				.withPool(tools(1, 8))
				.withPool(nuggets(10))
				.withPool(chance(item(Items.EMERALD, 1, 1, 2), 15))
				.withPool(chance(book(), 15))
				.withPool(chance(bow(), 10));
	}

	/** The camp's buried chest: always a tool and nuggets, more often emeralds, a book or a bow. */
	private LootTable.Builder campSecretChest() {
		return LootTable.lootTable()
				.withPool(food(1, 3))
				.withPool(tools(1, 0))
				.withPool(nuggets(0))
				.withPool(chance(item(Items.EMERALD, 1, 1, 2), 50))
				.withPool(chance(book(), 40))
				.withPool(chance(bow(), 30));
	}

	private LootTable.Builder campBarrel() {
		return LootTable.lootTable()
				.withPool(food(2, 4))
				.withPool(supplies(2, 4))
				.withPool(tools(1, 20));
	}

	private LootTable.Builder shipwreckSupply() {
		LootPool.Builder sea = LootPool.lootPool().setRolls(ContextIntProviders.between(1, 3))
				.add(item(Items.DRIED_KELP, 10, 2, 5))
				.add(item(Items.PAPER, 8, 1, 4))
				.add(item(Items.COD, 6, 1, 2))
				.add(item(Items.SALMON, 4, 1, 2))
				.add(item(Items.COAL, 4, 1, 3));
		return LootTable.lootTable()
				.withPool(food(2, 4))
				.withPool(sea)
				.withPool(supplies(1, 3))
				.withPool(tools(1, 12));
	}

	private LootTable.Builder shipwreckTreasure() {
		return LootTable.lootTable()
				.withPool(supplies(1, 2))
				.withPool(tools(1, 8))
				.withPool(nuggets(0))
				.withPool(chance(item(Items.EMERALD, 1, 1, 2), 50))
				.withPool(chance(book(), 30))
				.withPool(chance(bow(), 25));
	}

	private static LootPool.Builder food(int minRolls, int maxRolls) {
		return LootPool.lootPool().setRolls(ContextIntProviders.between(minRolls, maxRolls))
				.add(item(Items.BREAD, 10, 1, 2))
				.add(item(Items.APPLE, 10, 1, 3))
				.add(item(Items.CARROT, 8, 1, 3))
				.add(item(Items.POTATO, 8, 1, 3))
				.add(item(Items.BEETROOT, 6, 1, 3))
				.add(item(Items.SWEET_BERRIES, 6, 2, 5))
				.add(item(Items.WHEAT_SEEDS, 4, 2, 5));
	}

	private static LootPool.Builder supplies(int minRolls, int maxRolls) {
		return LootPool.lootPool().setRolls(ContextIntProviders.between(minRolls, maxRolls))
				.add(item(Items.STICK, 10, 2, 5))
				.add(item(EamItems.PEBBLE, 10, 3, 8))
				.add(item(Items.STRING, 8, 1, 3))
				.add(item(Items.FLINT, 6, 1, 2))
				.add(item(Items.LEATHER, 5, 1, 2))
				.add(item(Items.ARROW, 5, 2, 5))
				.add(item(Items.BOWL, 4, 1, 1))
				.add(item(Items.BONE, 4, 1, 2));
	}

	/** One slingshot or flint tool with 20-60% of its durability left, or nothing with weight {@code empty}. */
	private static LootPool.Builder tools(int rolls, int empty) {
		LootPool.Builder pool = LootPool.lootPool().setRolls(ContextIntProviders.exactly(rolls))
				.add(damaged(EamItems.FLINT_KNIFE, 6))
				.add(damaged(EamItems.FLINT_HATCHET, 6))
				.add(damaged(EamItems.SLINGSHOT, 5))
				.add(damaged(EamItems.FLINT_AXE, 4));
		return empty > 0 ? pool.add(EmptyLootItem.emptyItem().setWeight(empty)) : pool;
	}

	/** 1-3 copper or, less often, silver nuggets, or nothing with weight {@code empty}. */
	private static LootPool.Builder nuggets(int empty) {
		LootPool.Builder pool = LootPool.lootPool().setRolls(ContextIntProviders.exactly(1))
				.add(item(Items.COPPER_NUGGET, 6, 1, 3))
				.add(item(EamItems.SILVER_NUGGET, 3, 1, 3));
		return empty > 0 ? pool.add(EmptyLootItem.emptyItem().setWeight(empty)) : pool;
	}

	/** {@code entry} in {@code percent} of the chests. */
	private static LootPool.Builder chance(UniformContainerBase.Builder<?> entry, int percent) {
		return LootPool.lootPool().setRolls(ContextIntProviders.exactly(1))
				.add(entry.setWeight(percent))
				.add(EmptyLootItem.emptyItem().setWeight(100 - percent));
	}

	/** A book with a level I enchantment the enchanting table offers; no treasure ones. */
	private UniformContainerBase.Builder<?> book() {
		return LootItem.lootTableItem(Items.BOOK)
				.apply(EnchantWithLevelsFunction.enchantWithLevels(enchantments,
								ContextIntProviders.between(BOOK_MIN_LEVELS, BOOK_MAX_LEVELS))
						.withOptions(enchantments.getOrThrow(EnchantmentTags.IN_ENCHANTING_TABLE)));
	}

	private static UniformContainerBase.Builder<?> bow() {
		return LootItem.lootTableItem(Items.BOW)
				.apply(SetItemDamageFunction.setDamage(ContextFloatProviders.between(0.2F, 0.6F)));
	}

	private static LootPoolEntryContainer.Builder<?> damaged(ItemLike tool, int weight) {
		return LootItem.lootTableItem(tool).setWeight(weight)
				.apply(SetItemDamageFunction.setDamage(ContextFloatProviders.between(0.2F, 0.6F)));
	}

	private static UniformContainerBase.Builder<?> item(ItemLike item, int weight, int min, int max) {
		return LootItem.lootTableItem(item).setWeight(weight)
				.apply(SetItemCountFunction.setCount(ContextIntProviders.between(min, max)));
	}
}
