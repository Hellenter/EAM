package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.content.brewing.BrewingModule;
import io.github.hellenter.eam.mobs.entity.yaga.grave.YagaGrave;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.SimpleFabricLootTableSubProvider;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.functions.SetPotionFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * The chest buried in Baba Yaga's grave: two of her things (brews and the ingredients of the silver stand, now and then a
 * heart in a jar), always diamonds and emeralds, and two or three stacks of metal ingots, mithril the rarest.
 */
public class EamYagaGraveLootProvider extends SimpleFabricLootTableSubProvider {
	public EamYagaGraveLootProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries, LootContextParamSets.CHEST);
	}

	@Override
	public String getName() {
		return "Baba Yaga's grave loot table";
	}

	/** Vanilla's entry point; Fabric's datagen calls {@link #generate(BiConsumer)} instead. */
	@Override
	public void run() {
	}

	@Override
	public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
		LootPool.Builder hers = LootPool.lootPool().setRolls(ContextIntProviders.exactly(2))
				.add(potion(BrewingModule.WHITE_HONEY, 10, 1, 2))
				.add(potion(BrewingModule.BLACK_BLOOD, 8, 1, 2))
				.add(potion(BrewingModule.HOLY_WATER, 8, 2, 3))
				.add(item(EamItems.HERBAL_HONEY, 10, 1, 3))
				.add(item(EamItems.GARLIC_ORCHID_MIX, 8, 1, 3))
				.add(item(EamItems.FLOWER_MIX, 8, 1, 3))
				.add(item(EamItems.SILVER_REDSTONE_DUST, 8, 2, 4))
				.add(item(EamItems.LAPIS_GUNPOWDER_MIX, 6, 1, 3))
				.add(item(EamItems.FERMENTED_ENDER_EYE, 5, 1, 2))
				.add(item(EamItems.HEART_IN_JAR, 3, 1, 1));
		LootPool.Builder diamonds = LootPool.lootPool().setRolls(ContextIntProviders.exactly(1)).add(item(Items.DIAMOND, 1, 2, 4));
		LootPool.Builder emeralds = LootPool.lootPool().setRolls(ContextIntProviders.exactly(1)).add(item(Items.EMERALD, 1, 3, 7));
		LootPool.Builder ingots = LootPool.lootPool().setRolls(ContextIntProviders.between(2, 3))
				.add(item(Items.IRON_INGOT, 10, 3, 6))
				.add(item(EamItems.SILVER_INGOT, 10, 2, 5))
				.add(item(Items.GOLD_INGOT, 8, 2, 5))
				.add(item(Items.COPPER_INGOT, 8, 4, 8))
				.add(item(EamItems.MITHRIL_INGOT, 3, 1, 2));
		output.accept(YagaGrave.CHEST_LOOT, LootTable.lootTable().withPool(hers).withPool(diamonds).withPool(emeralds).withPool(ingots));
	}

	private static LootPoolEntryContainer.Builder<?> item(ItemLike item, int weight, int min, int max) {
		return LootItem.lootTableItem(item).setWeight(weight)
				.apply(SetItemCountFunction.setCount(min == max ? ContextIntProviders.exactly(min) : ContextIntProviders.between(min, max)));
	}

	private static LootPoolEntryContainer.Builder<?> potion(Holder<Potion> potion, int weight, int min, int max) {
		return LootItem.lootTableItem(Items.POTION).setWeight(weight).apply(SetPotionFunction.setPotion(potion))
				.apply(SetItemCountFunction.setCount(min == max ? ContextIntProviders.exactly(min) : ContextIntProviders.between(min, max)));
	}
}
