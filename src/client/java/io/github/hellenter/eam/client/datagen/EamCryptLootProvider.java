package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.world.structures.crypt.CryptModule;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.SimpleFabricLootTableSubProvider;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import net.minecraft.world.level.storage.loot.functions.EnchantWithLevelsFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemDamageFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.floats.ContextFloatProviders;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * Crypt chests: some junk and supplies, plus jewels, enchanted books and broken enchanted weapons. Tier 1 (bamboo stair
 * marker) gives few jewels, weak table-only enchantments on books and worn-out early weapons, each book or weapon only now
 * and then. Tier 2 (cherry stair marker) gives emeralds and diamonds, books from a full enchanting table or with any loot
 * enchantment (treasure ones such as Mending included) and nearly always an iron, silver or, rarely, mithril or ancient
 * metal weapon with strong enchantments. Weapons come with 5-20% (tier 2: 10-35%) of their durability left.
 */
public class EamCryptLootProvider extends SimpleFabricLootTableSubProvider {
	private final CompletableFuture<HolderLookup.Provider> registries;
	private HolderGetter<Enchantment> enchantments;

	public EamCryptLootProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries, LootContextParamSets.CHEST);
		this.registries = registries;
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
		enchantments = registries.join().lookupOrThrow(Registries.ENCHANTMENT);
		output.accept(CryptModule.TIER_1_LOOT, tierOne());
		output.accept(CryptModule.TIER_2_LOOT, tierTwo());
	}

	private LootTable.Builder tierOne() {
		LootPool.Builder supplies = LootPool.lootPool().setRolls(ContextIntProviders.between(2, 4))
				.add(item(Items.BONE, 20, 1, 3))
				.add(item(Items.ROTTEN_FLESH, 20, 1, 3))
				.add(item(Items.ARROW, 10, 2, 6))
				.add(item(Items.BREAD, 10, 1, 2))
				.add(item(Items.COPPER_NUGGET, 10, 2, 5))
				.add(item(EamItems.SILVER_NUGGET, 6, 1, 3))
				.add(item(Items.IRON_NUGGET, 6, 1, 3));
		LootPool.Builder jewels = LootPool.lootPool().setRolls(ContextIntProviders.between(1, 2))
				.add(item(Items.AMETHYST_SHARD, 12, 2, 4))
				.add(item(Items.GOLD_NUGGET, 10, 2, 6))
				.add(item(Items.LAPIS_LAZULI, 8, 2, 5))
				.add(item(Items.EMERALD, 3, 1, 1))
				.add(item(Items.DIAMOND, 1, 1, 1));
		LootPool.Builder books = LootPool.lootPool().setRolls(ContextIntProviders.exactly(1))
				.add(book(35, 5, 15, true))
				.add(EmptyLootItem.emptyItem().setWeight(65));
		LootPool.Builder weapons = LootPool.lootPool().setRolls(ContextIntProviders.exactly(1))
				.add(brokenWeapon(EamItems.RUSTY_IRON_SWORD, 4, 0.05F, 0.2F, 5, 12, true))
				.add(brokenWeapon(EamItems.RUSTY_IRON_AXE, 3, 0.05F, 0.2F, 5, 12, true))
				.add(brokenWeapon(Items.COPPER_SWORD, 3, 0.05F, 0.2F, 5, 12, true))
				.add(brokenWeapon(Items.BOW, 2, 0.05F, 0.2F, 5, 12, true))
				.add(EmptyLootItem.emptyItem().setWeight(22));
		return LootTable.lootTable().withPool(supplies).withPool(jewels).withPool(books).withPool(weapons);
	}

	private LootTable.Builder tierTwo() {
		LootPool.Builder supplies = LootPool.lootPool().setRolls(ContextIntProviders.between(2, 4))
				.add(item(Items.BONE, 15, 1, 3))
				.add(item(Items.ARROW, 10, 3, 9))
				.add(item(Items.BREAD, 10, 1, 3))
				.add(item(EamItems.SILVER_NUGGET, 10, 2, 6))
				.add(item(Items.IRON_NUGGET, 10, 2, 6))
				.add(item(Items.IRON_INGOT, 5, 1, 2))
				.add(item(Items.NAME_TAG, 1, 1, 1));
		LootPool.Builder jewels = LootPool.lootPool().setRolls(ContextIntProviders.between(2, 3))
				.add(item(Items.EMERALD, 8, 1, 2))
				.add(item(Items.GOLD_NUGGET, 8, 3, 7))
				.add(item(Items.AMETHYST_SHARD, 6, 2, 5))
				.add(item(Items.LAPIS_LAZULI, 6, 3, 7))
				.add(item(Items.DIAMOND, 5, 1, 2))
				.add(item(Items.GOLD_INGOT, 4, 1, 2));
		LootPool.Builder books = LootPool.lootPool().setRolls(ContextIntProviders.between(1, 2))
				.add(book(55, 18, 30, false))
				.add(LootItem.lootTableItem(Items.BOOK).setWeight(15)
						.apply(EnchantRandomlyFunction.randomApplicableEnchantment(enchantments)))
				.add(EmptyLootItem.emptyItem().setWeight(30));
		LootPool.Builder weapons = LootPool.lootPool().setRolls(ContextIntProviders.exactly(1))
				.add(brokenWeapon(Items.IRON_SWORD, 5, 0.1F, 0.35F, 15, 30, false))
				.add(brokenWeapon(EamItems.SILVER_SWORD, 4, 0.1F, 0.35F, 15, 30, false))
				.add(brokenWeapon(Items.IRON_AXE, 3, 0.1F, 0.35F, 15, 30, false))
				.add(brokenWeapon(Items.BOW, 3, 0.1F, 0.35F, 15, 30, false))
				.add(brokenWeapon(EamItems.SILVER_AXE, 2, 0.1F, 0.35F, 15, 30, false))
				.add(brokenWeapon(EamItems.MITHRIL_SWORD, 1, 0.1F, 0.35F, 15, 30, false))
				.add(brokenWeapon(EamItems.ANCIENT_METAL_SWORD, 1, 0.1F, 0.35F, 15, 30, false))
				.add(EmptyLootItem.emptyItem().setWeight(6));
		return LootTable.lootTable().withPool(supplies).withPool(jewels).withPool(books).withPool(weapons);
	}

	/** A book enchanted like on a table with {@code minLevels}-{@code maxLevels} experience levels. */
	private LootPoolEntryContainer.Builder<?> book(int weight, int minLevels, int maxLevels, boolean tableOnly) {
		return LootItem.lootTableItem(Items.BOOK).setWeight(weight).apply(enchant(minLevels, maxLevels, tableOnly));
	}

	/** A weapon worn down to {@code minLeft}-{@code maxLeft} of its durability, enchanted like on a table. */
	private LootPoolEntryContainer.Builder<?> brokenWeapon(ItemLike weapon, int weight, float minLeft, float maxLeft,
			int minLevels, int maxLevels, boolean tableOnly) {
		return LootItem.lootTableItem(weapon).setWeight(weight)
				.apply(SetItemDamageFunction.setDamage(ContextFloatProviders.between(minLeft, maxLeft)))
				.apply(enchant(minLevels, maxLevels, tableOnly));
	}

	/**
	 * Enchanting with {@code minLevels}-{@code maxLevels} levels: only what an enchanting table offers, or every
	 * enchantment found in loot, treasure ones such as Mending and curses included.
	 */
	private EnchantWithLevelsFunction.Builder enchant(int minLevels, int maxLevels, boolean tableOnly) {
		EnchantWithLevelsFunction.Builder enchant = EnchantWithLevelsFunction.enchantWithLevels(enchantments,
				ContextIntProviders.between(minLevels, maxLevels));
		return tableOnly ? enchant.withOptions(enchantments.getOrThrow(EnchantmentTags.IN_ENCHANTING_TABLE)) : enchant;
	}

	private static LootPoolEntryContainer.Builder<?> item(ItemLike item, int weight, int min, int max) {
		return LootItem.lootTableItem(item).setWeight(weight)
				.apply(SetItemCountFunction.setCount(ContextIntProviders.between(min, max)));
	}
}
