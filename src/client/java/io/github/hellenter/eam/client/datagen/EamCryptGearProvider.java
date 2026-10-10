package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.mobs.vanilla.crypt.CryptSpawners;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.SimpleFabricLootTableSubProvider;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.AlternativesEntry;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.entries.NestedLootTable;
import net.minecraft.world.level.storage.loot.entries.UniformContainerBase;
import net.minecraft.world.level.storage.loot.functions.SetEnchantmentsFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * Ancient metal gear the crypt's trial spawners put on their skeletons ({@link CryptSpawners}).
 * <ul>
 * <li>Swordsmen and archers: 1-3 armor pieces (mostly 2) in the order chestplate, helmet, boots, leggings; each is
 * chainmail 90% of the time, plate otherwise. Ominous: 3-4 pieces, plate 25%, Protection I-II on each piece, and
 * Sharpness I-II or Power I-II on the weapon.</li>
 * <li>Knights: full plate and the sword, Protection I and Sharpness I; ominous Protection II-III and Sharpness II-III.
 * Their shield comes from the knight himself.</li>
 * </ul>
 */
public class EamCryptGearProvider extends SimpleFabricLootTableSubProvider {
	/** Armor pieces in the order a skeleton gets them: chainmail and plate. */
	private static final Item[][] PIECES = {
			{EamItems.ANCIENT_METAL_CHAINMAIL_CHESTPLATE, EamItems.ANCIENT_METAL_CHESTPLATE},
			{EamItems.ANCIENT_METAL_CHAINMAIL_HELMET, EamItems.ANCIENT_METAL_HELMET},
			{EamItems.ANCIENT_METAL_CHAINMAIL_BOOTS, EamItems.ANCIENT_METAL_BOOTS},
			{EamItems.ANCIENT_METAL_CHAINMAIL_LEGGINGS, EamItems.ANCIENT_METAL_LEGGINGS}};

	private final CompletableFuture<HolderLookup.Provider> registries;
	private HolderGetter<Enchantment> enchantments;

	public EamCryptGearProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries, LootContextParamSets.EQUIPMENT);
		this.registries = registries;
	}

	@Override
	public String getName() {
		return "Crypt skeleton gear";
	}

	/** Vanilla's entry point; Fabric's datagen calls {@link #generate(BiConsumer)} instead. */
	@Override
	public void run() {
	}

	@Override
	public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
		enchantments = registries.join().lookupOrThrow(Registries.ENCHANTMENT);
		output.accept(CryptSpawners.SWORDSMAN.gear(), skeleton(false, LootItem.lootTableItem(EamItems.ANCIENT_METAL_SWORD)));
		output.accept(CryptSpawners.SWORDSMAN.ominousGear(), skeleton(true,
				enchanted(LootItem.lootTableItem(EamItems.ANCIENT_METAL_SWORD), Enchantments.SHARPNESS, 1, 2)));
		output.accept(CryptSpawners.ARCHER.gear(), skeleton(false, LootItem.lootTableItem(Items.BOW)));
		output.accept(CryptSpawners.ARCHER.ominousGear(), skeleton(true,
				enchanted(LootItem.lootTableItem(Items.BOW), Enchantments.POWER, 1, 2)));
		output.accept(CryptSpawners.KNIGHT.gear(), knight(1, 1));
		output.accept(CryptSpawners.KNIGHT.ominousGear(), knight(2, 3));
	}

	private LootTable.Builder skeleton(boolean ominous, LootPoolEntryContainer.Builder<?> weapon) {
		LootPool.Builder armor = LootPool.lootPool().setRolls(ContextIntProviders.exactly(1));
		float plateChance = ominous ? 0.25F : 0.1F;
		if (ominous) {
			armor.add(pieces(3, plateChance, true).setWeight(1)).add(pieces(4, plateChance, true).setWeight(1));
		} else {
			armor.add(pieces(1, plateChance, false).setWeight(1))
					.add(pieces(2, plateChance, false).setWeight(3))
					.add(pieces(3, plateChance, false).setWeight(1));
		}
		return LootTable.lootTable()
				.withPool(armor)
				.withPool(single(weapon));
	}

	/**
	 * The first {@code count} pieces, each plate with {@code plateChance}, chainmail otherwise: an inline table with a
	 * pool per piece, since every pool gives one item.
	 */
	private UniformContainerBase.Builder<?> pieces(int count, float plateChance, boolean enchant) {
		LootTable.Builder table = LootTable.lootTable();
		for (int i = 0; i < count; i++) {
			LootPoolEntryContainer.Builder<?> chainmail = armorPiece(PIECES[i][0], enchant);
			LootPoolEntryContainer.Builder<?> plate = armorPiece(PIECES[i][1], enchant)
					.when(LootItemRandomChanceCondition.randomChance(plateChance));
			table.withPool(LootPool.lootPool().setRolls(ContextIntProviders.exactly(1)).add(AlternativesEntry.alternatives(plate, chainmail)));
		}
		return NestedLootTable.inlineLootTable(table.build());
	}

	private LootPoolEntryContainer.Builder<?> armorPiece(Item item, boolean enchant) {
		UniformContainerBase.Builder<?> piece = LootItem.lootTableItem(item);
		return enchant ? enchanted(piece, Enchantments.PROTECTION, 1, 2) : piece;
	}

	private LootTable.Builder knight(int min, int max) {
		LootTable.Builder table = LootTable.lootTable();
		for (Item[] piece : PIECES) {
			table.withPool(single(enchanted(LootItem.lootTableItem(piece[1]), Enchantments.PROTECTION, min, max)));
		}
		return table.withPool(single(enchanted(LootItem.lootTableItem(EamItems.ANCIENT_METAL_SWORD), Enchantments.SHARPNESS, min, max)));
	}

	private static LootPool.Builder single(LootPoolEntryContainer.Builder<?> entry) {
		return LootPool.lootPool().setRolls(ContextIntProviders.exactly(1)).add(entry);
	}

	private UniformContainerBase.Builder<?> enchanted(UniformContainerBase.Builder<?> item, ResourceKey<Enchantment> enchantment, int min, int max) {
		return item.apply(new SetEnchantmentsFunction.Builder().withEnchantment(enchantments.getOrThrow(enchantment),
				min == max ? ContextIntProviders.exactly(min) : ContextIntProviders.between(min, max)));
	}
}
