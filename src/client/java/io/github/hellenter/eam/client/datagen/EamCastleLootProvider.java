package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.world.structures.castle.CastleModule;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.SimpleFabricLootTableSubProvider;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
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
 * The Pillager King's castle containers, one step up from the outpost's. Lime stair marker (barrel): stores of food
 * and water. Yellow (chest): arrows, a potion or two and now and then a shield. Green (chest): the King's treasure,
 * gold, emeralds, silver and iron, a golden apple or a book now and then, and about every other chest a worn silver
 * or iron weapon or armour piece, sometimes enchanted.
 */
public class EamCastleLootProvider extends SimpleFabricLootTableSubProvider {
	private final CompletableFuture<HolderLookup.Provider> registries;
	private HolderGetter<Enchantment> enchantments;

	public EamCastleLootProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries, LootContextParamSets.CHEST);
		this.registries = registries;
	}

	@Override
	public String getName() {
		return "Pillager King castle loot tables";
	}

	/** Vanilla's entry point; Fabric's datagen calls {@link #generate(BiConsumer)} instead. */
	@Override
	public void run() {
	}

	@Override
	public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
		enchantments = registries.join().lookupOrThrow(Registries.ENCHANTMENT);
		output.accept(CastleModule.FOOD_LOOT, food());
		output.accept(CastleModule.ARMS_LOOT, arms());
		output.accept(CastleModule.TREASURE_LOOT, treasure());
	}

	private static LootTable.Builder food() {
		return LootTable.lootTable().withPool(LootPool.lootPool().setRolls(ContextIntProviders.between(3, 6))
				.add(item(Items.BREAD, 8, 1, 4))
				.add(item(Items.COOKED_BEEF, 5, 1, 3))
				.add(item(Items.COOKED_PORKCHOP, 5, 1, 3))
				.add(item(Items.COOKED_MUTTON, 4, 1, 3))
				.add(item(Items.BAKED_POTATO, 5, 1, 3))
				.add(item(EamItems.CHEESE, 3, 1, 2))
				.add(item(Items.APPLE, 5, 1, 3))
				.add(item(Items.CARROT, 4, 1, 4))
				.add(item(Items.POTATO, 4, 1, 4))
				.add(item(Items.WHEAT, 4, 2, 6))
				.add(item(Items.DRIED_KELP, 2, 2, 5))
				.add(LootItem.lootTableItem(Items.POTION).setWeight(6).apply(SetPotionFunction.setPotion(Potions.WATER))));
	}

	private static LootTable.Builder arms() {
		LootPool.Builder arrows = LootPool.lootPool().setRolls(ContextIntProviders.between(1, 3))
				.add(item(Items.ARROW, 10, 4, 12))
				.add(item(EamItems.COPPER_ARROW, 5, 2, 6))
				.add(item(EamItems.SILVER_ARROW, 3, 2, 5))
				.add(item(EamItems.IRON_ARROW, 1, 1, 4));
		LootPool.Builder potions = LootPool.lootPool().setRolls(ContextIntProviders.between(0, 2))
				.add(potion(Potions.HEALING, 6))
				.add(potion(Potions.SWIFTNESS, 4))
				.add(potion(Potions.STRENGTH, 3))
				.add(potion(Potions.REGENERATION, 2))
				.add(potion(Potions.FIRE_RESISTANCE, 2))
				.add(potion(Potions.STRONG_HEALING, 1));
		LootPool.Builder shield = LootPool.lootPool().setRolls(ContextIntProviders.exactly(1))
				.add(worn(Items.SHIELD, 1, 0.4F, 0.9F))
				.add(EmptyLootItem.emptyItem().setWeight(2));
		LootPool.Builder string = LootPool.lootPool().setRolls(ContextIntProviders.exactly(1))
				.add(item(Items.STRING, 4, 1, 3))
				.add(item(Items.FEATHER, 4, 1, 4))
				.add(item(Items.FLINT, 3, 1, 3))
				.add(EmptyLootItem.emptyItem().setWeight(4));
		return LootTable.lootTable().withPool(arrows).withPool(potions).withPool(shield).withPool(string);
	}

	private LootTable.Builder treasure() {
		LootPool.Builder valuables = LootPool.lootPool().setRolls(ContextIntProviders.between(2, 4))
				.add(item(Items.GOLD_INGOT, 8, 1, 3))
				.add(item(Items.GOLD_NUGGET, 8, 4, 9))
				.add(item(Items.EMERALD, 8, 1, 4))
				.add(item(EamItems.SILVER_INGOT, 6, 1, 3))
				.add(item(Items.IRON_INGOT, 4, 1, 2))
				.add(item(Items.IRON_NUGGET, 6, 2, 6))
				.add(item(Items.AMETHYST_SHARD, 4, 2, 4))
				.add(item(Items.LAPIS_LAZULI, 4, 3, 8));
		LootPool.Builder gear = LootPool.lootPool().setRolls(ContextIntProviders.exactly(1))
				.add(worn(EamItems.SILVER_SWORD, 4, 0.4F, 0.8F))
				.add(worn(EamItems.SILVER_AXE, 3, 0.4F, 0.8F))
				.add(worn(EamItems.SILVER_HELMET, 3, 0.4F, 0.8F))
				.add(worn(EamItems.SILVER_CHESTPLATE, 2, 0.4F, 0.8F))
				.add(worn(EamItems.SILVER_CHAINMAIL_CHESTPLATE, 2, 0.4F, 0.8F))
				.add(worn(Items.IRON_SWORD, 2, 0.3F, 0.7F))
				.add(worn(Items.IRON_AXE, 2, 0.3F, 0.7F))
				.add(worn(Items.IRON_HELMET, 1, 0.3F, 0.7F))
				.add(enchanted(Items.IRON_SWORD, 1))
				.add(enchanted(EamItems.SILVER_SWORD, 1))
				.add(EmptyLootItem.emptyItem().setWeight(20));
		LootPool.Builder apple = LootPool.lootPool().setRolls(ContextIntProviders.exactly(1))
				.add(item(Items.GOLDEN_APPLE, 1, 1, 1))
				.add(EmptyLootItem.emptyItem().setWeight(3));
		LootPool.Builder book = LootPool.lootPool().setRolls(ContextIntProviders.exactly(1))
				.add(LootItem.lootTableItem(Items.BOOK).setWeight(1)
						.apply(EnchantWithLevelsFunction.enchantWithLevels(enchantments, ContextIntProviders.between(10, 25))
								.withOptions(enchantments.getOrThrow(EnchantmentTags.IN_ENCHANTING_TABLE))))
				.add(EmptyLootItem.emptyItem().setWeight(2));
		return LootTable.lootTable().withPool(valuables).withPool(gear).withPool(apple).withPool(book);
	}

	/** A worn piece of gear with a table enchantment. */
	private LootPoolEntryContainer.Builder<?> enchanted(ItemLike gear, int weight) {
		return LootItem.lootTableItem(gear).setWeight(weight)
				.apply(SetItemDamageFunction.setDamage(ContextFloatProviders.between(0.4F, 0.8F)))
				.apply(EnchantWithLevelsFunction.enchantWithLevels(enchantments, ContextIntProviders.between(8, 20))
						.withOptions(enchantments.getOrThrow(EnchantmentTags.IN_ENCHANTING_TABLE)));
	}

	private static LootPoolEntryContainer.Builder<?> potion(Holder<Potion> potion, int weight) {
		return LootItem.lootTableItem(Items.POTION).setWeight(weight).apply(SetPotionFunction.setPotion(potion));
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
