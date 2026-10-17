package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.world.structures.EmptyContainerLoot;
import io.github.hellenter.eam.world.structures.yagahut.YagaHutModule;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.SimpleFabricLootTableSubProvider;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
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
 * Loot of containers saved empty in templates: a few weak potions for barrels (Baba Yaga's hut, duergar houses), and in
 * Baba Yaga's chests valuables and the potion ingredients one finds in the surface world.
 */
public class EamHutLootProvider extends SimpleFabricLootTableSubProvider {
	public EamHutLootProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries, LootContextParamSets.CHEST);
	}

	@Override
	public String getName() {
		return "Hut and house container loot tables";
	}

	/** Vanilla's entry point; Fabric's datagen calls {@link #generate(BiConsumer)} instead. */
	@Override
	public void run() {
	}

	@Override
	public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
		output.accept(EmptyContainerLoot.LIGHT_POTIONS, LootTable.lootTable()
				.withPool(LootPool.lootPool().setRolls(ContextIntProviders.between(1, 3))
						.add(potion(Potions.HEALING, 5))
						.add(potion(Potions.REGENERATION, 3))
						.add(potion(Potions.SWIFTNESS, 4))
						.add(potion(Potions.NIGHT_VISION, 4))
						.add(potion(Potions.FIRE_RESISTANCE, 3))
						.add(potion(Potions.WATER_BREATHING, 3))
						.add(potion(Potions.LEAPING, 2))
						.add(potion(Potions.SLOW_FALLING, 2)))
				.withPool(LootPool.lootPool().setRolls(ContextIntProviders.between(0, 1))
						.add(item(Items.GLASS_BOTTLE, 1, 1, 3))));
		output.accept(YagaHutModule.CHEST_LOOT, LootTable.lootTable()
				.withPool(LootPool.lootPool().setRolls(ContextIntProviders.between(2, 4))
						.add(item(Items.GOLD_INGOT, 6, 1, 3))
						.add(item(Items.GOLD_NUGGET, 6, 3, 9))
						.add(item(Items.EMERALD, 5, 1, 3))
						.add(item(EamItems.SILVER_INGOT, 4, 1, 2))
						.add(item(Items.AMETHYST_SHARD, 4, 1, 4))
						.add(item(Items.LAPIS_LAZULI, 4, 2, 6))
						.add(item(Items.ENDER_PEARL, 2, 1, 1))
						.add(item(Items.DIAMOND, 1, 1, 1)))
				.withPool(LootPool.lootPool().setRolls(ContextIntProviders.between(3, 5))
						.add(item(Items.SPIDER_EYE, 6, 1, 3))
						.add(item(Items.FERMENTED_SPIDER_EYE, 3, 1, 2))
						.add(item(Items.SUGAR, 5, 2, 5))
						.add(item(Items.GLISTERING_MELON_SLICE, 3, 1, 2))
						.add(item(Items.GOLDEN_CARROT, 3, 1, 2))
						.add(item(Items.RABBIT_FOOT, 2, 1, 1))
						.add(item(Items.PUFFERFISH, 2, 1, 1))
						.add(item(Items.PHANTOM_MEMBRANE, 2, 1, 2))
						.add(item(Items.REDSTONE, 4, 2, 6))
						.add(item(Items.GUNPOWDER, 4, 1, 3))
						.add(item(Items.GLASS_BOTTLE, 5, 1, 3))));
	}

	private static LootPoolEntryContainer.Builder<?> potion(Holder<Potion> potion, int weight) {
		return LootItem.lootTableItem(Items.POTION).setWeight(weight).apply(SetPotionFunction.setPotion(potion));
	}

	private static LootPoolEntryContainer.Builder<?> item(ItemLike item, int weight, int min, int max) {
		return LootItem.lootTableItem(item).setWeight(weight)
				.apply(SetItemCountFunction.setCount(ContextIntProviders.between(min, max)));
	}
}
