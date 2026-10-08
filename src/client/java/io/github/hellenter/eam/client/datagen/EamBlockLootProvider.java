package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.content.EamBlocks;
import io.github.hellenter.eam.content.EamItems;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootSubProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
import net.minecraft.world.level.storage.loot.predicates.BonusLevelTableCondition;

import java.util.concurrent.CompletableFuture;

public class EamBlockLootProvider extends FabricBlockLootSubProvider {
	/** Vanilla gravel's flint chance by Fortune level; flint shards now drop with it. */
	private static final float[] SHARD_CHANCES = {0.1F, 0.14285715F, 0.25F, 1.0F};
	/** Whole flint drops 6 times less often than vanilla flint. */
	private static final float FLINT_RARITY = 6.0F;

	public EamBlockLootProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	@Override
	public void generate() {
		dropSelf(EamBlocks.PEBBLE);
		dropSelf(EamBlocks.FLINT_SHARD);
		dropOther(EamBlocks.GROUND_STICK, Items.STICK);
		// The sieve keeps its wear as item damage.
		add(EamBlocks.SIEVE, LootTable.lootTable().withPool(applyExplosionCondition(EamBlocks.SIEVE, LootPool.lootPool()
				.setRolls(ContextIntProviders.exactly(1))
				.add(LootItem.lootTableItem(EamBlocks.SIEVE)
						.apply(CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY).include(DataComponents.DAMAGE))))));

		// Overrides vanilla gravel with one drop out of: rare whole flint, a flint shard with vanilla's flint chance,
		// a copper nugget as often as a flint shard, or else the gravel itself.
		var fortune = enchantments.getOrThrow(Enchantments.FORTUNE);
		float[] flintChances = new float[SHARD_CHANCES.length];
		float[] copperChances = new float[SHARD_CHANCES.length];
		for (int i = 0; i < flintChances.length; i++) {
			flintChances[i] = SHARD_CHANCES[i] / FLINT_RARITY;
			// Copper is only tried when no shard dropped, so s / (1 - s) gives it the shard's overall chance.
			copperChances[i] = SHARD_CHANCES[i] < 1.0F ? Math.min(1.0F, SHARD_CHANCES[i] / (1.0F - SHARD_CHANCES[i])) : 1.0F;
		}
		// Copper ore drops like iron ore: one raw copper, Fortune with the ore formula.
		add(Blocks.COPPER_ORE, block -> createOreDrop(block, Items.RAW_COPPER));
		add(Blocks.DEEPSLATE_COPPER_ORE, block -> createOreDrop(block, Items.RAW_COPPER));

		add(EamBlocks.SILVER_ORE, block -> createOreDrop(block, EamItems.RAW_SILVER));
		add(EamBlocks.DEEPSLATE_SILVER_ORE, block -> createOreDrop(block, EamItems.RAW_SILVER));
		dropSelf(EamBlocks.SILVER_BLOCK);

		add(Blocks.GRAVEL, block -> createSilkTouchDispatchTable(block, applyExplosionCondition(block,
				LootItem.lootTableItem(Items.FLINT)
						.when(BonusLevelTableCondition.bonusLevelFlatChance(fortune, flintChances))
						.otherwise(LootItem.lootTableItem(EamItems.FLINT_SHARD)
								.when(BonusLevelTableCondition.bonusLevelFlatChance(fortune, SHARD_CHANCES)))
						.otherwise(LootItem.lootTableItem(Items.COPPER_NUGGET)
								.when(BonusLevelTableCondition.bonusLevelFlatChance(fortune, copperChances)))
						.otherwise(LootItem.lootTableItem(block)))));
	}
}
