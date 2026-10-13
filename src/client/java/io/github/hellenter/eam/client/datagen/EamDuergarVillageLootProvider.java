package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.world.structures.village.DuergarVillage;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.SimpleFabricLootTableSubProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * Duergar village loot: home chests hold household things and a little ore, barrels the clan's food (mushrooms, fish
 * from the lakes, potatoes), the smith's chest metal and now and then a mithril nugget. Taking it is theft, but the
 * duergar do not notice.
 */
public class EamDuergarVillageLootProvider extends SimpleFabricLootTableSubProvider {
	public EamDuergarVillageLootProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries, LootContextParamSets.CHEST);
	}

	@Override
	public String getName() {
		return "Duergar village loot tables";
	}

	/** Vanilla's entry point; Fabric's datagen calls {@link #generate(BiConsumer)} instead. */
	@Override
	public void run() {
	}

	@Override
	public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
		output.accept(DuergarVillage.CHEST_LOOT, LootTable.lootTable()
				.withPool(LootPool.lootPool().setRolls(ContextIntProviders.between(2, 5))
						.add(item(Items.TORCH, 8, 2, 6))
						.add(item(Items.COAL, 8, 2, 6))
						.add(item(Items.STRING, 4, 1, 3))
						.add(item(Items.LEATHER, 4, 1, 2))
						.add(item(Items.CANDLE, 3, 1, 2))
						.add(item(Items.BOOK, 2, 1, 1))
						.add(item(Items.BROWN_MUSHROOM, 5, 1, 4)))
				.withPool(LootPool.lootPool().setRolls(ContextIntProviders.exactly(1))
						.add(item(Items.RAW_IRON, 4, 1, 3))
						.add(item(Items.IRON_NUGGET, 6, 2, 6))
						.add(item(EamItems.SILVER_NUGGET, 4, 1, 4))
						.add(item(Items.AMETHYST_SHARD, 3, 1, 3))
						.add(item(EamItems.MITHRIL_NUGGET, 1, 1, 1))
						.add(EmptyLootItem.emptyItem().setWeight(6))));
		output.accept(DuergarVillage.BARREL_LOOT, LootTable.lootTable()
				.withPool(LootPool.lootPool().setRolls(ContextIntProviders.between(2, 4))
						.add(item(Items.BROWN_MUSHROOM, 6, 2, 5))
						.add(item(Items.RED_MUSHROOM, 4, 1, 3))
						.add(item(Items.MUSHROOM_STEW, 2, 1, 1))
						.add(item(Items.POTATO, 5, 2, 5))
						.add(item(Items.BAKED_POTATO, 3, 1, 3))
						.add(item(Items.COD, 3, 1, 3))
						.add(item(Items.DRIED_KELP, 3, 2, 5))
						.add(item(Items.COAL, 3, 1, 4))));
		output.accept(DuergarVillage.SMITH_LOOT, LootTable.lootTable()
				.withPool(LootPool.lootPool().setRolls(ContextIntProviders.between(2, 4))
						.add(item(Items.IRON_INGOT, 6, 1, 3))
						.add(item(Items.IRON_NUGGET, 6, 3, 8))
						.add(item(Items.COAL, 6, 3, 8))
						.add(item(EamItems.SILVER_INGOT, 3, 1, 2))
						.add(item(Items.GOLD_NUGGET, 3, 2, 6))
						.add(item(Items.IRON_CHAIN, 2, 1, 3))
						.add(item(EamItems.MITHRIL_NUGGET, 2, 1, 2)))
				.withPool(LootPool.lootPool().setRolls(ContextIntProviders.exactly(1))
						.add(item(Items.IRON_PICKAXE, 2, 1, 1))
						.add(item(Items.IRON_AXE, 2, 1, 1))
						.add(item(Items.SHEARS, 1, 1, 1))
						.add(EmptyLootItem.emptyItem().setWeight(6))));
	}

	private static LootPoolEntryContainer.Builder<?> item(ItemLike item, int weight, int min, int max) {
		return LootItem.lootTableItem(item).setWeight(weight)
				.apply(SetItemCountFunction.setCount(ContextIntProviders.between(min, max)));
	}
}
