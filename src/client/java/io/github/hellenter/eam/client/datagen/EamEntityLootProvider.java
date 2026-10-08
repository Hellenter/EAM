package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.mobs.vanilla.drops.MobDropsModule;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.SimpleFabricLootTableSubProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.EnchantedCountIncreaseFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.floats.ContextFloatProviders;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/** Extra death loot added to vanilla mobs by {@code MobDropsModule}. */
public class EamEntityLootProvider extends SimpleFabricLootTableSubProvider {
	private final CompletableFuture<HolderLookup.Provider> registries;

	public EamEntityLootProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries, LootContextParamSets.ENTITY);
		this.registries = registries;
	}

	/** Vanilla's entry point; Fabric's datagen calls {@link #generate(BiConsumer)} instead. */
	@Override
	public void run() {
	}

	@Override
	public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
		var enchantments = registries.join().lookupOrThrow(Registries.ENCHANTMENT);
		// A cow's leather roll (0-2, Looting adds 0-1 per level) that happens one kill in three: a third as much leather.
		output.accept(MobDropsModule.SMALL_LEATHER_LOOT, LootTable.lootTable().withPool(LootPool.lootPool()
				.setRolls(ContextIntProviders.exactly(1))
				.when(LootItemRandomChanceCondition.randomChance(1.0F / 3.0F))
				.add(LootItem.lootTableItem(Items.LEATHER)
						.apply(SetItemCountFunction.setCount(ContextIntProviders.between(0, 2)))
						.apply(EnchantedCountIncreaseFunction.lootingMultiplier(enchantments, ContextFloatProviders.between(0.0F, 1.0F))))));
	}
}
