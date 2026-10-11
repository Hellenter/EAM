package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.content.EamBlocks;
import io.github.hellenter.eam.content.EamItems;
import io.github.hellenter.eam.content.anvil.MetalAnvilBlock;
import io.github.hellenter.eam.content.swamp.BerryShrub;
import io.github.hellenter.eam.content.swamp.CattailBlock;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootSubProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.advancements.predicates.StatePropertiesPredicate;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.functions.CopyBlockState;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
import net.minecraft.world.level.storage.loot.predicates.BonusLevelTableCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchBlock;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class EamBlockLootProvider extends FabricBlockLootSubProvider {
	/** Vanilla gravel's flint chance by Fortune level; flint shards now drop with it. */
	private static final float[] SHARD_CHANCES = {0.1F, 0.14285715F, 0.25F, 1.0F};
	/** Whole flint drops 6 times less often than vanilla flint. */
	private static final float FLINT_RARITY = 6.0F;

	public EamBlockLootProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	private LootItemCondition.Builder matches(Block block, StatePropertiesPredicate.Builder properties) {
		return MatchBlock.blockMatches(this.blocks, block, properties);
	}

	/** One to {@code max} berries from a shrub broken while ripe. */
	private LootPool.Builder berries(Block block, Item berry, int max) {
		return LootPool.lootPool().setRolls(ContextIntProviders.exactly(1))
				.when(matches(block, StatePropertiesPredicate.Builder.properties().hasProperty(BerryShrub.BERRIES, BerryShrub.RIPE)))
				.add(applyExplosionDecay(block, LootItem.lootTableItem(berry).apply(SetItemCountFunction.setCount(ContextIntProviders.between(1, max)))));
	}

	@Override
	public void generate() {
		dropSelf(EamBlocks.PEBBLE);
		dropSelf(EamBlocks.FLINT_SHARD);
		dropOther(EamBlocks.GROUND_STICK, Items.STICK);
		// Swampy forest plants drop like their vanilla look-alikes.
		add(EamBlocks.DUCKWEED, this::createSegmentedBlockDrops);
		dropSelf(EamBlocks.MARSH_MARIGOLD);
		// A clump of cattails gives one per stalk, from its lower half.
		add(EamBlocks.CATTAIL, block -> LootTable.lootTable().withPool(LootPool.lootPool()
				.setRolls(ContextIntProviders.exactly(1))
				.when(matches(block, StatePropertiesPredicate.Builder.properties().hasProperty(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER)))
				.add(applyExplosionDecay(block, LootItem.lootTableItem(block).apply(List.of(2, 3, 4),
						count -> SetItemCountFunction.setCount(ContextIntProviders.exactly(count))
								.when(matches(block, StatePropertiesPredicate.Builder.properties().hasProperty(CattailBlock.AMOUNT, count))))))));
		add(EamBlocks.FIREWEED, block -> createSinglePropConditionTable(block, DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
		add(EamBlocks.SWAMP_GRASS, this::createGrassDrops);
		add(EamBlocks.TALL_SWAMP_GRASS, block -> createDoublePlantWithSeedDrops(block, EamBlocks.SWAMP_GRASS));
		add(EamBlocks.SWAMP_SHRUB, block -> createShearsOnlyDrop(block));
		dropSelf(EamBlocks.SHELF_MUSHROOM);
		// Underworld mushroom blocks, like vanilla stems, give themselves only to silk touch.
		dropWhenSilkTouch(EamBlocks.CHANTERELLE_CAP);
		dropWhenSilkTouch(EamBlocks.CHANTERELLE_STEM);
		dropWhenSilkTouch(EamBlocks.CHAMPIGNON_CAP);
		dropWhenSilkTouch(EamBlocks.GLOWING_CHAMPIGNON_CAP);
		EamBlocks.HOLLOW_LOGS.values().forEach(this::dropSelf);
		// A dense shrub gives itself to shears or silk touch, otherwise a stick now and then.
		add(EamBlocks.DENSE_SHRUB, block -> createSilkTouchOrShearsDispatchTable(block, applyExplosionDecay(block,
				LootItem.lootTableItem(Items.STICK).apply(SetItemCountFunction.setCount(ContextIntProviders.between(0, 2)))))
				.withPool(berries(block, EamItems.CRANBERRY, 3)));
		// Big shrubs: leaves only to shears or silk touch, the post drops itself; ripe ones drop their berries too.
		add(EamBlocks.BIG_SHRUB_LEAVES, block -> createShearsOrSilkTouchOnlyDrop(block).withPool(berries(block, EamItems.WOLFBERRY, 2)));
		dropSelf(EamBlocks.BIG_SHRUB_STEM);
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
		// Anvils keep their wear, as vanilla's chipped anvil drops a chipped anvil.
		add(EamBlocks.MITHRIL_ORE, block -> createOreDrop(block, EamItems.RAW_MITHRIL));
		add(EamBlocks.DEEPSLATE_MITHRIL_ORE, block -> createOreDrop(block, EamItems.RAW_MITHRIL));
		dropSelf(EamBlocks.MITHRIL_BLOCK);
		dropSelf(EamBlocks.ANCIENT_METAL_BLOCK);
		add(EamBlocks.OBSIDIAN_BLAST_FURNACE, this::createNameableBlockEntityTable);
		for (Block anvil : List.of(EamBlocks.COPPER_ANVIL, EamBlocks.SILVER_ANVIL, EamBlocks.MITHRIL_ANVIL)) {
			add(anvil, LootTable.lootTable().withPool(applyExplosionCondition(anvil, LootPool.lootPool()
					.setRolls(ContextIntProviders.exactly(1))
					.add(LootItem.lootTableItem(anvil).apply(CopyBlockState.copyState(anvil).copy(MetalAnvilBlock.WEAR))))));
		}
		add(EamBlocks.SILVER_BREWING_STAND, this::createNameableBlockEntityTable);
		for (Block block : List.of(EamBlocks.SILVER_BARS, EamBlocks.SILVER_TRAPDOOR, EamBlocks.MITHRIL_BARS, EamBlocks.MITHRIL_TRAPDOOR)) {
			dropSelf(block);
		}
		add(EamBlocks.SILVER_DOOR, this::createDoorTable);
		add(EamBlocks.MITHRIL_DOOR, this::createDoorTable);

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
