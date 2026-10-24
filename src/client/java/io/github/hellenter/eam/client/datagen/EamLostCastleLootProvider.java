package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.world.structures.lostcastle.LostCastleModule;

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
 * The Lost castle's containers. Lime stair marker (barrel): provisions, dried food that keeps for years. Yellow
 * (chest): the soldiers' belongings, odds and ends, coins of metal, now and then a worn knife or a book. Orange (chest):
 * arrows, bows, battered shields and healing for the defence. Red (chest): the treasure, ancient metal and gold, gems,
 * a golden apple in most chests, and a piece of ancient metal gear, often enchanted.
 */
public class EamLostCastleLootProvider extends SimpleFabricLootTableSubProvider {
	private final CompletableFuture<HolderLookup.Provider> registries;
	private HolderGetter<Enchantment> enchantments;

	public EamLostCastleLootProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries, LootContextParamSets.CHEST);
		this.registries = registries;
	}

	@Override
	public String getName() {
		return "Lost castle loot tables";
	}

	/** Vanilla's entry point; Fabric's datagen calls {@link #generate(BiConsumer)} instead. */
	@Override
	public void run() {
	}

	@Override
	public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
		enchantments = registries.join().lookupOrThrow(Registries.ENCHANTMENT);
		output.accept(LostCastleModule.PROVISIONS_LOOT, provisions());
		output.accept(LostCastleModule.BELONGINGS_LOOT, belongings());
		output.accept(LostCastleModule.SUPPLIES_LOOT, supplies());
		output.accept(LostCastleModule.TREASURE_LOOT, treasure());
	}

	private static LootTable.Builder provisions() {
		return LootTable.lootTable().withPool(LootPool.lootPool().setRolls(ContextIntProviders.between(3, 6))
				.add(item(EamItems.DRIED_BEEF, 6, 1, 4))
				.add(item(EamItems.DRIED_PORKCHOP, 6, 1, 4))
				.add(item(EamItems.DRIED_MUTTON, 5, 1, 4))
				.add(item(EamItems.DRIED_CHICKEN, 3, 1, 3))
				.add(item(EamItems.DRIED_COD, 3, 1, 3))
				.add(item(EamItems.DRIED_SALMON, 2, 1, 3))
				.add(item(EamItems.DRIED_BREAD, 6, 1, 4))
				.add(item(EamItems.DRIED_APPLE, 4, 1, 3))
				.add(item(EamItems.DRIED_CARROT, 3, 1, 3))
				.add(item(EamItems.DRIED_BEETROOT, 2, 1, 3))
				.add(item(EamItems.DRIED_GARLIC, 2, 1, 3))
				.add(item(EamItems.DRIED_CRANBERRY, 2, 1, 4))
				.add(item(EamItems.DRIED_WOLFBERRY, 2, 1, 4))
				.add(item(Items.WHEAT, 2, 2, 5))
				.add(LootItem.lootTableItem(Items.POTION).setWeight(4).apply(SetPotionFunction.setPotion(Potions.WATER))));
	}

	private static LootTable.Builder belongings() {
		LootPool.Builder odds = LootPool.lootPool().setRolls(ContextIntProviders.between(2, 5))
				.add(item(Items.BONE, 6, 1, 3))
				.add(item(Items.STRING, 5, 1, 4))
				.add(item(Items.LEATHER, 4, 1, 2))
				.add(item(EamItems.LEATHER_THREAD, 3, 1, 3))
				.add(item(Items.FLINT, 4, 1, 3))
				.add(item(Items.TORCH, 4, 1, 4))
				.add(item(Items.PAPER, 3, 1, 3))
				.add(item(Items.CANDLE, 2, 1, 2))
				.add(item(Items.GOLD_NUGGET, 5, 2, 7))
				.add(item(EamItems.SILVER_NUGGET, 4, 2, 6))
				.add(item(EamItems.ANCIENT_METAL_NUGGET, 3, 1, 4))
				.add(item(EamItems.ANCIENT_METAL_RING, 2, 2, 6))
				.add(item(Items.EMERALD, 1, 1, 1));
		LootPool.Builder keepsake = LootPool.lootPool().setRolls(ContextIntProviders.exactly(1))
				.add(worn(EamItems.ANCIENT_METAL_KNIFE, 2, 0.2F, 0.6F))
				.add(worn(EamItems.RUSTY_IRON_SWORD, 2, 0.3F, 0.7F))
				.add(worn(EamItems.ANCIENT_METAL_CHAINMAIL_HELMET, 1, 0.2F, 0.6F))
				.add(worn(EamItems.ANCIENT_METAL_CHAINMAIL_BOOTS, 1, 0.2F, 0.6F))
				.add(item(Items.BOOK, 2, 1, 1))
				.add(item(Items.CLOCK, 1, 1, 1))
				.add(item(Items.COMPASS, 1, 1, 1))
				.add(EmptyLootItem.emptyItem().setWeight(10));
		return LootTable.lootTable().withPool(odds).withPool(keepsake);
	}

	private static LootTable.Builder supplies() {
		LootPool.Builder arrows = LootPool.lootPool().setRolls(ContextIntProviders.between(2, 4))
				.add(item(Items.ARROW, 10, 6, 16))
				.add(item(EamItems.IRON_ARROW, 5, 3, 8))
				.add(item(EamItems.ANCIENT_METAL_ARROW, 4, 2, 6))
				.add(item(EamItems.SILVER_ARROW, 3, 3, 6));
		LootPool.Builder weapons = LootPool.lootPool().setRolls(ContextIntProviders.exactly(1))
				.add(worn(Items.BOW, 4, 0.3F, 0.8F))
				.add(worn(Items.SHIELD, 4, 0.3F, 0.8F))
				.add(worn(EamItems.ANCIENT_METAL_SPEAR, 2, 0.3F, 0.7F))
				.add(worn(EamItems.LONGBOW, 1, 0.3F, 0.7F))
				.add(EmptyLootItem.emptyItem().setWeight(6));
		LootPool.Builder potions = LootPool.lootPool().setRolls(ContextIntProviders.between(0, 2))
				.add(potion(Potions.HEALING, 6))
				.add(potion(Potions.REGENERATION, 3))
				.add(potion(Potions.FIRE_RESISTANCE, 3))
				.add(potion(Potions.STRENGTH, 2))
				.add(potion(Potions.STRONG_HEALING, 1));
		LootPool.Builder stores = LootPool.lootPool().setRolls(ContextIntProviders.between(1, 2))
				.add(item(Items.STRING, 4, 2, 5))
				.add(item(Items.FEATHER, 4, 2, 6))
				.add(item(Items.FLINT, 3, 2, 4))
				.add(item(Items.STICK, 3, 2, 6))
				.add(item(Items.TORCH, 3, 2, 6))
				.add(item(Items.COAL, 2, 2, 5));
		return LootTable.lootTable().withPool(arrows).withPool(weapons).withPool(potions).withPool(stores);
	}

	private LootTable.Builder treasure() {
		LootPool.Builder valuables = LootPool.lootPool().setRolls(ContextIntProviders.between(3, 5))
				.add(item(EamItems.ANCIENT_METAL_INGOT, 6, 1, 3))
				.add(item(EamItems.ANCIENT_METAL_NUGGET, 6, 3, 8))
				.add(item(Items.GOLD_INGOT, 8, 1, 4))
				.add(item(Items.GOLD_NUGGET, 6, 4, 9))
				.add(item(Items.EMERALD, 7, 1, 4))
				.add(item(Items.DIAMOND, 2, 1, 2))
				.add(item(Items.AMETHYST_SHARD, 4, 2, 5))
				.add(item(Items.LAPIS_LAZULI, 4, 3, 8))
				.add(item(EamItems.MITHRIL_NUGGET, 1, 1, 3));
		LootPool.Builder gear = LootPool.lootPool().setRolls(ContextIntProviders.exactly(1))
				.add(worn(EamItems.ANCIENT_METAL_SWORD, 4, 0.5F, 0.9F))
				.add(worn(EamItems.ANCIENT_METAL_AXE, 3, 0.5F, 0.9F))
				.add(worn(EamItems.ANCIENT_METAL_HELMET, 3, 0.5F, 0.9F))
				.add(worn(EamItems.ANCIENT_METAL_CHESTPLATE, 2, 0.5F, 0.9F))
				.add(worn(EamItems.ANCIENT_METAL_LEGGINGS, 2, 0.5F, 0.9F))
				.add(worn(EamItems.ANCIENT_METAL_BOOTS, 2, 0.5F, 0.9F))
				.add(worn(EamItems.LONGBOW, 2, 0.6F, 1.0F))
				.add(enchanted(EamItems.ANCIENT_METAL_SWORD, 2))
				.add(enchanted(EamItems.ANCIENT_METAL_CHESTPLATE, 1))
				.add(enchanted(EamItems.ANCIENT_METAL_HELMET, 1))
				.add(EmptyLootItem.emptyItem().setWeight(6));
		LootPool.Builder apples = LootPool.lootPool().setRolls(ContextIntProviders.exactly(1))
				.add(item(Items.GOLDEN_APPLE, 6, 1, 2))
				.add(item(Items.ENCHANTED_GOLDEN_APPLE, 1, 1, 1))
				.add(EmptyLootItem.emptyItem().setWeight(3));
		LootPool.Builder book = LootPool.lootPool().setRolls(ContextIntProviders.exactly(1))
				.add(LootItem.lootTableItem(Items.BOOK).setWeight(1)
						.apply(EnchantWithLevelsFunction.enchantWithLevels(enchantments, ContextIntProviders.between(15, 30))
								.withOptions(enchantments.getOrThrow(EnchantmentTags.IN_ENCHANTING_TABLE))))
				.add(EmptyLootItem.emptyItem().setWeight(2));
		return LootTable.lootTable().withPool(valuables).withPool(gear).withPool(apples).withPool(book);
	}

	/** A worn piece of gear with a table enchantment. */
	private LootPoolEntryContainer.Builder<?> enchanted(ItemLike gear, int weight) {
		return LootItem.lootTableItem(gear).setWeight(weight)
				.apply(SetItemDamageFunction.setDamage(ContextFloatProviders.between(0.5F, 0.9F)))
				.apply(EnchantWithLevelsFunction.enchantWithLevels(enchantments, ContextIntProviders.between(12, 25))
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
