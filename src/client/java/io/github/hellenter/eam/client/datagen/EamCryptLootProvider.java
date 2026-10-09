package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.world.structures.crypt.CryptModule;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.SimpleFabricLootTableSubProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/** Crypt chests. Placeholders until the crypt's own loot is decided; tier 2 rolls twice as often and adds iron. */
public class EamCryptLootProvider extends SimpleFabricLootTableSubProvider {
	public EamCryptLootProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries, LootContextParamSets.CHEST);
	}

	@Override
	public String getName() {
		return "Crypt chest loot tables";
	}

	/** Vanilla's entry point; Fabric's datagen calls {@link #generate(BiConsumer)} instead. */
	@Override
	public void run() {
	}

	@Override
	public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
		output.accept(CryptModule.TIER_1_LOOT, chest(1));
		output.accept(CryptModule.TIER_2_LOOT, chest(2));
	}

	private static LootTable.Builder chest(int tier) {
		LootPool.Builder pool = LootPool.lootPool()
				.setRolls(ContextIntProviders.between(3 * tier, 5 * tier))
				.add(item(Items.BONE, 20, 1, 4))
				.add(item(Items.ROTTEN_FLESH, 20, 1, 4))
				.add(item(Items.ARROW, 10, 2, 8))
				.add(item(Items.BREAD, 10, 1, 2))
				.add(item(Items.COPPER_NUGGET, 10, 2, 6))
				.add(item(Items.IRON_NUGGET, 8, 1, 4))
				.add(item(Items.GOLD_NUGGET, 5, 1, 4));
		if (tier >= 2) pool.add(item(Items.IRON_INGOT, 4, 1, 2)).add(item(Items.NAME_TAG, 1, 1, 1));
		return LootTable.lootTable().withPool(pool);
	}

	private static LootPoolEntryContainer.Builder<?> item(ItemLike item, int weight, int min, int max) {
		return LootItem.lootTableItem(item).setWeight(weight)
				.apply(SetItemCountFunction.setCount(ContextIntProviders.between(min, max)));
	}
}
