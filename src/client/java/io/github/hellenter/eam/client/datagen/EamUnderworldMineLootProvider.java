package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.world.structures.mine.UnderworldMine;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.SimpleFabricLootTableSubProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemDamageFunction;
import net.minecraft.world.level.storage.loot.functions.SetPotionFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.floats.ContextFloatProviders;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * Underworld mine loot: what miners left behind. Chests hold mining supplies, ore and nuggets (mithril rarely) and about
 * every other time a worn copper or rusty iron tool; barrels hold their food, now mostly spoiled, and lighting.
 */
public class EamUnderworldMineLootProvider extends SimpleFabricLootTableSubProvider {
	public EamUnderworldMineLootProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries, LootContextParamSets.CHEST);
	}

	@Override
	public String getName() {
		return "Underworld mine loot tables";
	}

	/** Vanilla's entry point; Fabric's datagen calls {@link #generate(BiConsumer)} instead. */
	@Override
	public void run() {
	}

	@Override
	public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
		output.accept(UnderworldMine.CHEST_LOOT, chest());
		output.accept(UnderworldMine.BARREL_LOOT, barrel());
	}

	private static LootTable.Builder chest() {
		LootPool.Builder supplies = LootPool.lootPool().setRolls(ContextIntProviders.between(2, 4))
				.add(item(Items.RAIL, 10, 2, 6))
				.add(item(Items.TORCH, 8, 1, 4))
				.add(item(Items.COAL, 8, 2, 5))
				.add(item(Items.STICK, 6, 2, 5))
				.add(item(Items.STRING, 4, 1, 2))
				.add(item(Items.FLINT, 4, 1, 3))
				.add(item(Items.LANTERN, 2, 1, 1));
		LootPool.Builder ore = LootPool.lootPool().setRolls(ContextIntProviders.between(1, 2))
				.add(item(Items.RAW_COPPER, 6, 1, 3))
				.add(item(Items.COPPER_NUGGET, 8, 2, 5))
				.add(item(EamItems.SILVER_NUGGET, 5, 1, 3))
				.add(item(Items.RAW_IRON, 3, 1, 2))
				.add(item(Items.IRON_NUGGET, 4, 1, 3))
				.add(item(EamItems.MITHRIL_NUGGET, 1, 1, 1))
				.add(EmptyLootItem.emptyItem().setWeight(6));
		LootPool.Builder tools = LootPool.lootPool().setRolls(ContextIntProviders.exactly(1))
				.add(worn(Items.COPPER_PICKAXE, 4, 0.2F, 0.6F))
				.add(worn(Items.COPPER_SHOVEL, 3, 0.2F, 0.6F))
				.add(worn(EamItems.RUSTY_IRON_PICKAXE, 3, 0.2F, 0.6F))
				.add(worn(EamItems.RUSTY_IRON_SHOVEL, 2, 0.2F, 0.6F))
				.add(worn(EamItems.RUSTY_CHAINMAIL_HELMET, 1, 0.2F, 0.5F))
				.add(EmptyLootItem.emptyItem().setWeight(13));
		return LootTable.lootTable().withPool(supplies).withPool(ore).withPool(tools);
	}

	private static LootTable.Builder barrel() {
		return LootTable.lootTable().withPool(LootPool.lootPool().setRolls(ContextIntProviders.between(1, 4))
				.add(item(Items.ROTTEN_FLESH, 6, 1, 3))
				.add(item(Items.BREAD, 4, 1, 2))
				.add(item(Items.POTATO, 4, 1, 3))
				.add(item(Items.POISONOUS_POTATO, 3, 1, 2))
				.add(item(Items.DRIED_KELP, 3, 2, 4))
				.add(item(Items.TORCH, 5, 2, 6))
				.add(item(Items.COAL, 4, 1, 4))
				.add(item(Items.STICK, 3, 2, 6))
				.add(LootItem.lootTableItem(Items.POTION).setWeight(4).apply(SetPotionFunction.setPotion(Potions.WATER))));
	}

	/** A piece of gear with {@code minLeft}-{@code maxLeft} of its durability left. */
	private static LootPoolEntryContainer.Builder<?> worn(ItemLike gear, int weight, float minLeft, float maxLeft) {
		return LootItem.lootTableItem(gear).setWeight(weight)
				.apply(SetItemDamageFunction.setDamage(ContextFloatProviders.between(minLeft, maxLeft)));
	}

	private static LootPoolEntryContainer.Builder<?> item(ItemLike item, int weight, int min, int max) {
		return LootItem.lootTableItem(item).setWeight(weight)
				.apply(SetItemCountFunction.setCount(ContextIntProviders.between(min, max)));
	}
}
