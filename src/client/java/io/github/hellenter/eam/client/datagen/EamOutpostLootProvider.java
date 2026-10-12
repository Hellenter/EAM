package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.world.structures.outpost.OutpostModule;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.SimpleFabricLootTableSubProvider;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.EnchantWithLevelsFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemDamageFunction;
import net.minecraft.world.level.storage.loot.functions.SetPotionFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.floats.ContextFloatProviders;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * Pillager outpost chests, kept sparse for a starting monument (Maxim, 2026-10-12). Lime stair marker: a few arrows.
 * Yellow: basic things, a little food, copper or silver nuggets and now and then worn leather armour. Orange: better
 * food, a few jewels, and about every other chest a worn copper tool or armour piece, iron one time in thirty. Red: the
 * map to the Pillager King's castle, emeralds, maybe a golden apple and a book with a weak table enchantment. Barrels:
 * two to five stacks of food and water bottles.
 */
public class EamOutpostLootProvider extends SimpleFabricLootTableSubProvider {
	private final CompletableFuture<HolderLookup.Provider> registries;
	private HolderGetter<Enchantment> enchantments;

	public EamOutpostLootProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries, LootContextParamSets.CHEST);
		this.registries = registries;
	}

	@Override
	public String getName() {
		return "Pillager outpost loot tables";
	}

	/** Vanilla's entry point; Fabric's datagen calls {@link #generate(BiConsumer)} instead. */
	@Override
	public void run() {
	}

	@Override
	public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
		enchantments = registries.join().lookupOrThrow(Registries.ENCHANTMENT);
		output.accept(OutpostModule.ARROWS_LOOT, arrows());
		output.accept(OutpostModule.BASIC_LOOT, basic());
		output.accept(OutpostModule.BETTER_LOOT, better());
		output.accept(OutpostModule.SPECIAL_LOOT, special());
		output.accept(OutpostModule.BARREL_LOOT, barrel());
	}

	private static LootTable.Builder arrows() {
		return LootTable.lootTable().withPool(LootPool.lootPool().setRolls(ContextIntProviders.exactly(1))
				.add(item(Items.ARROW, 10, 2, 6))
				.add(item(EamItems.COPPER_ARROW, 3, 1, 3)));
	}

	private static LootTable.Builder basic() {
		LootPool.Builder supplies = LootPool.lootPool().setRolls(ContextIntProviders.between(1, 3))
				.add(item(Items.STICK, 10, 2, 4))
				.add(item(Items.STRING, 8, 1, 2))
				.add(item(Items.ARROW, 6, 2, 4))
				.add(item(Items.FLINT, 6, 1, 2))
				.add(item(Items.LEATHER, 4, 1, 1))
				.add(item(Items.WHEAT, 4, 1, 3));
		LootPool.Builder food = LootPool.lootPool().setRolls(ContextIntProviders.between(1, 2))
				.add(item(Items.BREAD, 10, 1, 1))
				.add(item(Items.APPLE, 8, 1, 2))
				.add(item(Items.CARROT, 6, 1, 2))
				.add(item(Items.POTATO, 6, 1, 2))
				.add(item(Items.DRIED_KELP, 4, 2, 4));
		LootPool.Builder nuggets = LootPool.lootPool().setRolls(ContextIntProviders.exactly(1))
				.add(item(Items.COPPER_NUGGET, 6, 1, 3))
				.add(item(EamItems.SILVER_NUGGET, 3, 1, 2))
				.add(EmptyLootItem.emptyItem().setWeight(6));
		LootPool.Builder armor = LootPool.lootPool().setRolls(ContextIntProviders.exactly(1))
				.add(worn(Items.LEATHER_HELMET, 3, 0.1F, 0.4F))
				.add(worn(Items.LEATHER_CHESTPLATE, 2, 0.1F, 0.4F))
				.add(worn(Items.LEATHER_LEGGINGS, 2, 0.1F, 0.4F))
				.add(worn(Items.LEATHER_BOOTS, 3, 0.1F, 0.4F))
				.add(EmptyLootItem.emptyItem().setWeight(20));
		return LootTable.lootTable().withPool(supplies).withPool(food).withPool(nuggets).withPool(armor);
	}

	private static LootTable.Builder better() {
		LootPool.Builder supplies = LootPool.lootPool().setRolls(ContextIntProviders.between(1, 2))
				.add(item(Items.ARROW, 8, 3, 6))
				.add(item(Items.STRING, 6, 1, 3))
				.add(item(Items.LEATHER, 5, 1, 2))
				.add(item(Items.COPPER_NUGGET, 5, 2, 4))
				.add(item(EamItems.SILVER_NUGGET, 4, 1, 3))
				.add(item(Items.IRON_NUGGET, 2, 1, 2));
		LootPool.Builder food = LootPool.lootPool().setRolls(ContextIntProviders.between(1, 2))
				.add(item(Items.COOKED_BEEF, 6, 1, 2))
				.add(item(Items.COOKED_PORKCHOP, 6, 1, 2))
				.add(item(Items.COOKED_MUTTON, 5, 1, 2))
				.add(item(Items.BAKED_POTATO, 6, 1, 2))
				.add(item(Items.BREAD, 6, 1, 2))
				.add(item(Items.PUMPKIN_PIE, 2, 1, 1));
		LootPool.Builder jewels = LootPool.lootPool().setRolls(ContextIntProviders.exactly(1))
				.add(item(Items.GOLD_NUGGET, 8, 2, 4))
				.add(item(Items.AMETHYST_SHARD, 6, 1, 3))
				.add(item(Items.LAPIS_LAZULI, 6, 2, 4))
				.add(item(Items.EMERALD, 2, 1, 1))
				.add(EmptyLootItem.emptyItem().setWeight(15));
		LootPool.Builder gear = LootPool.lootPool().setRolls(ContextIntProviders.exactly(1))
				.add(worn(Items.COPPER_SWORD, 5, 0.3F, 0.7F))
				.add(worn(Items.COPPER_AXE, 5, 0.3F, 0.7F))
				.add(worn(Items.COPPER_PICKAXE, 4, 0.3F, 0.7F))
				.add(worn(Items.COPPER_SHOVEL, 3, 0.3F, 0.7F))
				.add(worn(Items.COPPER_HELMET, 4, 0.3F, 0.7F))
				.add(worn(Items.COPPER_CHESTPLATE, 2, 0.3F, 0.7F))
				.add(worn(Items.COPPER_LEGGINGS, 3, 0.3F, 0.7F))
				.add(worn(Items.COPPER_BOOTS, 4, 0.3F, 0.7F))
				.add(worn(Items.IRON_SWORD, 1, 0.2F, 0.5F))
				.add(worn(Items.IRON_HELMET, 1, 0.2F, 0.5F))
				.add(EmptyLootItem.emptyItem().setWeight(32));
		return LootTable.lootTable().withPool(supplies).withPool(food).withPool(jewels).withPool(gear);
	}

	private LootTable.Builder special() {
		LootPool.Builder map = LootPool.lootPool().setRolls(ContextIntProviders.exactly(1))
				.add(LootItem.lootTableItem(EamItems.KING_CASTLE_MAP));
		LootPool.Builder emeralds = LootPool.lootPool().setRolls(ContextIntProviders.exactly(1))
				.add(item(Items.EMERALD, 6, 1, 3))
				.add(EmptyLootItem.emptyItem().setWeight(4));
		LootPool.Builder apple = LootPool.lootPool().setRolls(ContextIntProviders.exactly(1))
				.add(item(Items.GOLDEN_APPLE, 1, 1, 1))
				.add(EmptyLootItem.emptyItem().setWeight(3));
		LootPool.Builder book = LootPool.lootPool().setRolls(ContextIntProviders.exactly(1))
				.add(LootItem.lootTableItem(Items.BOOK).setWeight(1)
						.apply(EnchantWithLevelsFunction.enchantWithLevels(enchantments, ContextIntProviders.between(5, 15))
								.withOptions(enchantments.getOrThrow(EnchantmentTags.IN_ENCHANTING_TABLE))))
				.add(EmptyLootItem.emptyItem().setWeight(1));
		LootPool.Builder supplies = LootPool.lootPool().setRolls(ContextIntProviders.between(1, 2))
				.add(item(Items.BREAD, 6, 1, 2))
				.add(item(Items.ARROW, 6, 3, 6))
				.add(item(Items.GOLD_NUGGET, 4, 2, 5))
				.add(item(Items.PAPER, 3, 1, 3));
		return LootTable.lootTable().withPool(map).withPool(emeralds).withPool(apple).withPool(book).withPool(supplies);
	}

	private static LootTable.Builder barrel() {
		return LootTable.lootTable().withPool(LootPool.lootPool().setRolls(ContextIntProviders.between(2, 5))
				.add(item(Items.COD, 6, 1, 3))
				.add(item(Items.SALMON, 4, 1, 3))
				.add(item(Items.BREAD, 6, 1, 3))
				.add(item(Items.BEEF, 4, 1, 3))
				.add(item(Items.PORKCHOP, 4, 1, 3))
				.add(item(Items.MUTTON, 3, 1, 2))
				.add(item(Items.APPLE, 4, 1, 3))
				.add(item(Items.CARROT, 3, 1, 3))
				.add(item(Items.POTATO, 3, 1, 3))
				.add(LootItem.lootTableItem(Items.POTION).setWeight(6).apply(SetPotionFunction.setPotion(Potions.WATER))));
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
