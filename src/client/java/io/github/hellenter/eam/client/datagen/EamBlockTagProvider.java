package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.content.EamBlocks;
import io.github.hellenter.eam.mobs.vanilla.drops.MobDropsModule;
import io.github.hellenter.eam.mobs.vanilla.leaves.ThroughLeavesModule;
import io.github.hellenter.eam.survival.sleep.SafeRoomModule;
import io.github.hellenter.eam.world.mining.HandBreakingModule;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.tags.BlockItemTagAppender;
import net.minecraft.references.BlockIds;
import net.minecraft.references.BlockItemIds;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;

import java.util.concurrent.CompletableFuture;

public class EamBlockTagProvider extends FabricTagsProvider.BlockTagsProvider {
	public EamBlockTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	@Override
	protected void addTags(HolderLookup.Provider registries) {
		// Blocks that break instantly (flowers, grass, torches, ground items) need no entry: they are always hand-breakable.
		BlockItemTagAppender<Block> tag = builder(HandBreakingModule.HAND_BREAKABLE);
		// Dirt and loose blocks: dirt, grass, farmland, mud, sand, gravel, clay, snow, soul sand, concrete powder.
		tag.forceAddTag(BlockTags.MINEABLE_WITH_SHOVEL);
		tag.forceAddTag(BlockTags.DIRT);
		tag.add(BlockItemIds.MANGROVE_ROOTS);
		// Plants and leaves.
		tag.forceAddTag(BlockTags.LEAVES);
		tag.forceAddTag(BlockTags.SAPLINGS);
		tag.forceAddTag(BlockTags.FLOWERS);
		tag.forceAddTag(BlockTags.CROPS);
		tag.forceAddTag(BlockTags.CAVE_VINES);
		tag.forceAddTag(BlockTags.WART_BLOCKS);
		tag.forceAddTag(BlockTags.REPLACEABLE_BY_TREES);
		tag.add(BlockItemIds.CACTUS, BlockItemIds.BAMBOO, BlockItemIds.SUGAR_CANE, BlockItemIds.VINE, BlockItemIds.COCOA_CROP,
				BlockItemIds.MELON, BlockItemIds.PUMPKIN, BlockItemIds.CARVED_PUMPKIN,
				BlockItemIds.BROWN_MUSHROOM_BLOCK, BlockItemIds.RED_MUSHROOM_BLOCK, BlockItemIds.MUSHROOM_STEM,
				BlockItemIds.KELP, BlockItemIds.SEAGRASS, BlockItemIds.SEA_PICKLE, BlockItemIds.LILY_PAD,
				BlockItemIds.MOSS_CARPET, BlockItemIds.PALE_MOSS_CARPET, BlockItemIds.PALE_HANGING_MOSS,
				BlockItemIds.BIG_DRIPLEAF, BlockItemIds.SMALL_DRIPLEAF, BlockItemIds.GLOW_LICHEN,
				BlockItemIds.CHORUS_PLANT, BlockItemIds.CHORUS_FLOWER, BlockItemIds.NETHER_WART,
				BlockItemIds.TWISTING_VINES, BlockItemIds.WEEPING_VINES, BlockItemIds.HANGING_ROOTS, BlockItemIds.SPORE_BLOSSOM,
				BlockItemIds.HAY_BLOCK, BlockItemIds.DRIED_KELP_BLOCK);
		tag.add(BlockIds.KELP_PLANT, BlockIds.TALL_SEAGRASS, BlockIds.BIG_DRIPLEAF_STEM,
				BlockIds.TWISTING_VINES_PLANT, BlockIds.WEEPING_VINES_PLANT);
		// Planks and everything made from them.
		tag.forceAddTag(BlockTags.PLANKS);
		tag.forceAddTag(BlockTags.WOODEN_SLABS);
		tag.forceAddTag(BlockTags.WOODEN_STAIRS);
		tag.forceAddTag(BlockTags.WOODEN_FENCES);
		tag.forceAddTag(BlockTags.FENCE_GATES);
		tag.forceAddTag(BlockTags.WOODEN_DOORS);
		tag.forceAddTag(BlockTags.WOODEN_TRAPDOORS);
		tag.forceAddTag(BlockTags.WOODEN_BUTTONS);
		tag.forceAddTag(BlockTags.WOODEN_PRESSURE_PLATES);
		tag.forceAddTag(BlockTags.WOODEN_SHELVES);
		tag.forceAddTag(BlockTags.ALL_SIGNS);
		tag.add(BlockItemIds.LADDER, BlockItemIds.COMPOSTER, BlockItemIds.NOTE_BLOCK, BlockItemIds.JUKEBOX);
		// Wool.
		tag.forceAddTag(BlockTags.WOOL);
		tag.forceAddTag(BlockTags.WOOL_CARPETS);
		tag.forceAddTag(BlockTags.WOOL_SLABS);
		tag.forceAddTag(BlockTags.WOOL_STAIRS);
		// Furniture.
		tag.forceAddTag(BlockTags.BEDS);
		tag.forceAddTag(BlockTags.BANNERS);
		tag.forceAddTag(BlockTags.CANDLES);
		tag.add(BlockItemIds.CHEST, BlockItemIds.TRAPPED_CHEST, BlockItemIds.BARREL, BlockItemIds.CRAFTING_TABLE,
				BlockItemIds.BOOKSHELF, BlockItemIds.CHISELED_BOOKSHELF, BlockItemIds.LECTERN, BlockItemIds.LOOM,
				BlockItemIds.CARTOGRAPHY_TABLE, BlockItemIds.FLETCHING_TABLE, BlockItemIds.SMITHING_TABLE,
				BlockItemIds.BEEHIVE, BlockItemIds.DECORATED_POT);
		// Our own blocks.
		tag.add(EamBlocks.CAMP_FIRE.builtInRegistryHolder().key(),
				EamBlocks.PEBBLE.builtInRegistryHolder().key(),
				EamBlocks.FLINT_SHARD.builtInRegistryHolder().key(),
				EamBlocks.GROUND_STICK.builtInRegistryHolder().key(),
				EamBlocks.SIEVE.builtInRegistryHolder().key());
		builder(BlockTags.MINEABLE_WITH_AXE).add(EamBlocks.SIEVE.builtInRegistryHolder().key());

		builder(BlockTags.MINEABLE_WITH_PICKAXE).add(EamBlocks.SILVER_ORE.builtInRegistryHolder().key(),
				EamBlocks.DEEPSLATE_SILVER_ORE.builtInRegistryHolder().key(), EamBlocks.SILVER_BLOCK.builtInRegistryHolder().key(),
				EamBlocks.SILVER_BREWING_STAND.builtInRegistryHolder().key());
		// Like copper ore and the iron block: a copper pickaxe or better.
		// Our anvils are anvils: the menu accepts them, they fall and hurt like the vanilla one.
		builder(BlockTags.ANVIL).add(EamBlocks.COPPER_ANVIL.builtInRegistryHolder().key(), EamBlocks.SILVER_ANVIL.builtInRegistryHolder().key());
		builder(BlockTags.MINEABLE_WITH_PICKAXE).add(EamBlocks.COPPER_ANVIL.builtInRegistryHolder().key(),
				EamBlocks.SILVER_ANVIL.builtInRegistryHolder().key());
		builder(BlockTags.NEEDS_STONE_TOOL).add(EamBlocks.SILVER_ORE.builtInRegistryHolder().key(),
				EamBlocks.DEEPSLATE_SILVER_ORE.builtInRegistryHolder().key(), EamBlocks.SILVER_BLOCK.builtInRegistryHolder().key());

		// Mithril: an iron pickaxe or better. Obsidian moves from diamond to iron, so the obsidian blast furnace can be built.
		builder(BlockTags.MINEABLE_WITH_PICKAXE).add(EamBlocks.MITHRIL_ORE.builtInRegistryHolder().key(),
				EamBlocks.DEEPSLATE_MITHRIL_ORE.builtInRegistryHolder().key(), EamBlocks.MITHRIL_BLOCK.builtInRegistryHolder().key(),
				EamBlocks.MITHRIL_ANVIL.builtInRegistryHolder().key(), EamBlocks.OBSIDIAN_BLAST_FURNACE.builtInRegistryHolder().key());
		builder(BlockTags.NEEDS_IRON_TOOL).add(EamBlocks.MITHRIL_ORE.builtInRegistryHolder().key(),
				EamBlocks.DEEPSLATE_MITHRIL_ORE.builtInRegistryHolder().key(), EamBlocks.MITHRIL_BLOCK.builtInRegistryHolder().key(),
				EamBlocks.OBSIDIAN_BLAST_FURNACE.builtInRegistryHolder().key());
		builder(BlockTags.NEEDS_IRON_TOOL).add(BlockItemIds.OBSIDIAN);
		// Replaces the vanilla list, which also has obsidian.
		BlockItemTagAppender<Block> diamondTool = builder(BlockTags.NEEDS_DIAMOND_TOOL);
		diamondTool.setReplace(true);
		diamondTool.add(BlockItemIds.CRYING_OBSIDIAN, BlockItemIds.NETHERITE_BLOCK, BlockItemIds.RESPAWN_ANCHOR, BlockItemIds.ANCIENT_DEBRIS);
		builder(BlockTags.ANVIL).add(EamBlocks.MITHRIL_ANVIL.builtInRegistryHolder().key());

		builder(ThroughLeavesModule.ARROWS_PASS).forceAddTag(BlockTags.LEAVES);
		// Wood a creeper blows out of a tree is lost, so explosions are no shortcut to logs.
		builder(MobDropsModule.LOST_TO_CREEPERS).forceAddTag(BlockTags.LOGS).forceAddTag(BlockTags.PLANKS);
		// Mobs break these, but a wooden room is still safe to sleep in. Doors always count as walls.
		builder(SafeRoomModule.SAFE_WALLS).forceAddTag(BlockTags.PLANKS).forceAddTag(BlockTags.WOODEN_SLABS)
				.forceAddTag(BlockTags.WOODEN_STAIRS).forceAddTag(BlockTags.TRAPDOORS).forceAddTag(BlockTags.FENCE_GATES);
	}
}
