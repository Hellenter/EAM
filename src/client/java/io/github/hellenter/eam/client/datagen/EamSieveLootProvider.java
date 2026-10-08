package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.content.sieve.SieveModule;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.SimpleFabricLootTableSubProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * What one sifted gravel gives: always exactly one item. Flint shards and copper nuggets as often as each other, as
 * from broken gravel; whole flint rarely.
 */
public class EamSieveLootProvider extends SimpleFabricLootTableSubProvider {
	public EamSieveLootProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries, LootContextParamSets.CHEST);
	}

	/** Vanilla's entry point; Fabric's datagen calls {@link #generate(BiConsumer)} instead. */
	@Override
	public void run() {
	}

	@Override
	public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
		output.accept(SieveModule.LOOT, LootTable.lootTable().withPool(LootPool.lootPool()
				.setRolls(ContextIntProviders.exactly(1))
				.add(LootItem.lootTableItem(EamItems.FLINT_SHARD).setWeight(10))
				.add(LootItem.lootTableItem(Items.COPPER_NUGGET).setWeight(10))
				.add(LootItem.lootTableItem(Items.FLINT).setWeight(2))));
	}
}
