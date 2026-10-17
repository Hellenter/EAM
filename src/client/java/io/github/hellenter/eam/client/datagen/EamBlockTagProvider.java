package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.content.EamBlocks;
import io.github.hellenter.eam.content.fridge.FridgeModule;
import io.github.hellenter.eam.content.swamp.CattailBlock;
import io.github.hellenter.eam.dimension.feature.GiantMushroomFeature;
import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLordModule;
import io.github.hellenter.eam.mobs.entity.draugr.DraugrModule;
import io.github.hellenter.eam.mobs.vanilla.breaking.BlockBreakingModule;
import io.github.hellenter.eam.mobs.entity.lurker.SwampLurkerModule;
import io.github.hellenter.eam.mobs.vanilla.drops.MobDropsModule;
import io.github.hellenter.eam.mobs.vanilla.leaves.ThroughLeavesModule;
import io.github.hellenter.eam.survival.sleep.SafeRoomModule;
import io.github.hellenter.eam.world.mining.HandBreakingModule;
import io.github.hellenter.eam.world.bluemoon.BlueMoonModule;
import io.github.hellenter.eam.world.farming.HarvestModule;
import io.github.hellenter.eam.world.structures.VillageModule;
import io.github.hellenter.eam.world.swamp.SwampPondFeature;

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
				EamBlocks.SIEVE.builtInRegistryHolder().key(),
				EamBlocks.FOOD_DRYER.builtInRegistryHolder().key());
		builder(BlockTags.MINEABLE_WITH_AXE).add(EamBlocks.SIEVE.builtInRegistryHolder().key(), EamBlocks.FOOD_DRYER.builtInRegistryHolder().key(), EamBlocks.FEEDER.builtInRegistryHolder().key(),
				EamBlocks.SMALL_FRIDGE.builtInRegistryHolder().key(), EamBlocks.STRIPPED_POPLAR_FENCE.builtInRegistryHolder().key());
		builder(BlockTags.WOODEN_FENCES).add(EamBlocks.STRIPPED_POPLAR_FENCE.builtInRegistryHolder().key());

		builder(BlockTags.MINEABLE_WITH_PICKAXE).add(EamBlocks.METAL_FEEDER.builtInRegistryHolder().key(),
				EamBlocks.LARGE_FRIDGE.builtInRegistryHolder().key(), EamBlocks.SILVER_ORE.builtInRegistryHolder().key(),
				EamBlocks.DEEPSLATE_SILVER_ORE.builtInRegistryHolder().key(), EamBlocks.SILVER_BLOCK.builtInRegistryHolder().key(),
				EamBlocks.SILVER_BREWING_STAND.builtInRegistryHolder().key());
		// Like copper ore and the iron block: a copper pickaxe or better.
		// Our anvils are anvils: the menu accepts them, they fall and hurt like the vanilla one.
		builder(FridgeModule.CHILLERS).add(BlockItemIds.SNOW, BlockItemIds.SNOW_BLOCK, BlockItemIds.POWDER_SNOW, BlockItemIds.ICE, BlockItemIds.PACKED_ICE,
				BlockItemIds.BLUE_ICE);
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
		builder(BlockTags.MINEABLE_WITH_PICKAXE).add(EamBlocks.ANCIENT_METAL_BLOCK.builtInRegistryHolder().key());
		builder(BlockTags.NEEDS_IRON_TOOL).add(EamBlocks.ANCIENT_METAL_BLOCK.builtInRegistryHolder().key());
		// Replaces the vanilla list, which also has obsidian.
		BlockItemTagAppender<Block> diamondTool = builder(BlockTags.NEEDS_DIAMOND_TOOL);
		diamondTool.setReplace(true);
		diamondTool.add(BlockItemIds.CRYING_OBSIDIAN, BlockItemIds.NETHERITE_BLOCK, BlockItemIds.RESPAWN_ANCHOR, BlockItemIds.ANCIENT_DEBRIS);
		builder(BlockTags.ANVIL).add(EamBlocks.MITHRIL_ANVIL.builtInRegistryHolder().key());
		// Adamantium: a mithril pickaxe or better.
		builder(BlockTags.MINEABLE_WITH_PICKAXE).add(EamBlocks.ADAMANTIUM_ORE.builtInRegistryHolder().key(),
				EamBlocks.DEEPSLATE_ADAMANTIUM_ORE.builtInRegistryHolder().key(), EamBlocks.ADAMANTIUM_BLOCK.builtInRegistryHolder().key(),
				EamBlocks.ADAMANTIUM_ANVIL.builtInRegistryHolder().key());
		diamondTool.add(EamBlocks.ADAMANTIUM_ORE.builtInRegistryHolder().key(), EamBlocks.DEEPSLATE_ADAMANTIUM_ORE.builtInRegistryHolder().key(),
				EamBlocks.ADAMANTIUM_BLOCK.builtInRegistryHolder().key());
		builder(BlockTags.ANVIL).add(EamBlocks.ADAMANTIUM_ANVIL.builtInRegistryHolder().key());

		// Silver and mithril bars, doors and trapdoors: tagged like the iron ones, so safe rooms count the doors as doors.
		builder(BlockTags.BARS).add(EamBlocks.SILVER_BARS.builtInRegistryHolder().key(), EamBlocks.MITHRIL_BARS.builtInRegistryHolder().key());
		builder(BlockTags.DOORS).add(EamBlocks.SILVER_DOOR.builtInRegistryHolder().key(), EamBlocks.MITHRIL_DOOR.builtInRegistryHolder().key());
		builder(BlockTags.TRAPDOORS).add(EamBlocks.SILVER_TRAPDOOR.builtInRegistryHolder().key(),
				EamBlocks.MITHRIL_TRAPDOOR.builtInRegistryHolder().key());
		builder(BlockTags.MINEABLE_WITH_PICKAXE).add(EamBlocks.SILVER_BARS.builtInRegistryHolder().key(),
				EamBlocks.SILVER_DOOR.builtInRegistryHolder().key(), EamBlocks.SILVER_TRAPDOOR.builtInRegistryHolder().key(),
				EamBlocks.MITHRIL_BARS.builtInRegistryHolder().key(), EamBlocks.MITHRIL_DOOR.builtInRegistryHolder().key(),
				EamBlocks.MITHRIL_TRAPDOOR.builtInRegistryHolder().key());
		builder(BlockTags.NEEDS_IRON_TOOL).add(EamBlocks.MITHRIL_BARS.builtInRegistryHolder().key(),
				EamBlocks.MITHRIL_DOOR.builtInRegistryHolder().key(), EamBlocks.MITHRIL_TRAPDOOR.builtInRegistryHolder().key());

		// Logs, wood, stems and hyphae (stripped too) break only with an axe.
		builder(HandBreakingModule.AXE_ONLY).forceAddTag(BlockTags.LOGS);
		// Hollow logs are bark: an axe like any log.
		for (Block hollow : EamBlocks.HOLLOW_LOGS.values()) {
			builder(HandBreakingModule.AXE_ONLY).add(hollow.builtInRegistryHolder().key());
			builder(BlockTags.MINEABLE_WITH_AXE).add(hollow.builtInRegistryHolder().key());
		}
		builder(BlockTags.MINEABLE_WITH_AXE).add(EamBlocks.SHELF_MUSHROOM.builtInRegistryHolder().key());
		builder(BlockTags.MINEABLE_WITH_AXE).add(EamBlocks.CHANTERELLE_CAP.builtInRegistryHolder().key(),
				EamBlocks.CHANTERELLE_STEM.builtInRegistryHolder().key(), EamBlocks.CHAMPIGNON_CAP.builtInRegistryHolder().key(),
				EamBlocks.GLOWING_CHAMPIGNON_CAP.builtInRegistryHolder().key());
		builder(BlockTags.MINEABLE_WITH_AXE).add(EamBlocks.BURNT_MUSHROOM_CAP.builtInRegistryHolder().key(), EamBlocks.BURNT_MUSHROOM_STEM.builtInRegistryHolder().key(), EamBlocks.SMOLDERING_MUSHROOM_CAP.builtInRegistryHolder().key());

		// Swamp ground a swamp lurker digs itself into; the block turns to mud.
		builder(SwampLurkerModule.BURROWS_IN).add(BlockItemIds.MUD, BlockItemIds.GRASS_BLOCK, BlockItemIds.DIRT, BlockItemIds.COARSE_DIRT,
				BlockItemIds.CLAY);

		// Rock duergar renegades dig through with their picks: natural stone and cobble, never bricks or other built blocks.
		builder(BlockBreakingModule.STONE)
				.add(BlockItemIds.COBBLESTONE, BlockItemIds.MOSSY_COBBLESTONE, BlockItemIds.COBBLED_DEEPSLATE, BlockItemIds.CALCITE,
						BlockItemIds.SMOOTH_BASALT, BlockItemIds.PACKED_ICE, BlockItemIds.ICE)
				.forceAddTag(BlockTags.BASE_STONE_OVERWORLD).forceAddTag(BlockTags.BASE_STONE_NETHER);

		// Ice-cave floors draugr lie frozen in.
		// Giant underworld mushrooms grow from the cave floor only, never from another mushroom's cap.
		builder(GiantMushroomFeature.GROUND).add(BlockItemIds.GRAVEL, BlockItemIds.CLAY).forceAddTag(BlockTags.SUBSTRATE_OVERWORLD)
				.forceAddTag(BlockTags.BASE_STONE_OVERWORLD);
		builder(DraugrModule.BURIED_IN).add(BlockItemIds.SNOW_BLOCK, BlockItemIds.POWDER_SNOW, BlockItemIds.PACKED_ICE, BlockItemIds.ICE,
				BlockItemIds.BLUE_ICE);

		// Swampy forest plants: cattails root in mud, clay, sand and gravel too; ponds are dug only into soft ground.
		builder(CattailBlock.SUPPORTS_CATTAIL).add(BlockItemIds.CLAY, BlockItemIds.SAND, BlockItemIds.RED_SAND, BlockItemIds.GRAVEL)
				.forceAddTag(BlockTags.SUPPORTS_VEGETATION);
		builder(SwampPondFeature.DIGGABLE).add(BlockItemIds.PODZOL).forceAddTag(BlockTags.SUBSTRATE_OVERWORLD);
		builder(BlockTags.SMALL_FLOWERS).add(EamBlocks.MARSH_MARIGOLD.builtInRegistryHolder().key());
		builder(BlockTags.FLOWERS).add(EamBlocks.FIREWEED.builtInRegistryHolder().key());
		// Garlic is a crop like carrots: bees tend it and it keeps its farmland moist.
		builder(BlockTags.CROPS).add(EamBlocks.GARLIC_CROP.builtInRegistryHolder().key());
		builder(BlockTags.MAINTAINS_FARMLAND).add(EamBlocks.GARLIC_CROP.builtInRegistryHolder().key());
		// Plants whose harvest grows under a blue moon; a placed pumpkin is left out so it cannot be broken over and over.
		BlockItemTagAppender<Block> harvest = builder(BlueMoonModule.HARVEST_BLOCKS);
		harvest.forceAddTag(BlockTags.CROPS);
		harvest.forceAddTag(BlockTags.CAVE_VINES);
		harvest.add(BlockItemIds.MELON, BlockItemIds.SWEET_BERRY_CROP, BlockItemIds.COCOA_CROP);
		BlockItemTagAppender<Block> harvestCrops = builder(HarvestModule.CROPS);
		harvestCrops.forceAddTag(BlockTags.CROPS);
		harvestCrops.add(BlockItemIds.MELON, BlockItemIds.SWEET_BERRY_CROP, BlockItemIds.COCOA_CROP);
		builder(BlockTags.REPLACEABLE_BY_TREES).add(EamBlocks.SWAMP_GRASS.builtInRegistryHolder().key(),
				EamBlocks.TALL_SWAMP_GRASS.builtInRegistryHolder().key(), EamBlocks.SWAMP_SHRUB.builtInRegistryHolder().key(),
				EamBlocks.CATTAIL.builtInRegistryHolder().key(), EamBlocks.FIREWEED.builtInRegistryHolder().key(),
				EamBlocks.MARSH_MARIGOLD.builtInRegistryHolder().key(), EamBlocks.WILD_GARLIC.builtInRegistryHolder().key());
		// The dense shrub is a leafy bush: a hoe or shears take it fast, and bare hands can too.
		builder(HandBreakingModule.HAND_BREAKABLE).add(EamBlocks.DENSE_SHRUB.builtInRegistryHolder().key(),
				EamBlocks.BIG_SHRUB_LEAVES.builtInRegistryHolder().key(), EamBlocks.BIG_SHRUB_STEM.builtInRegistryHolder().key(),
				EamBlocks.SHELF_MUSHROOM.builtInRegistryHolder().key());
		builder(HandBreakingModule.HAND_BREAKABLE).add(EamBlocks.CHANTERELLE_CAP.builtInRegistryHolder().key(),
				EamBlocks.CHANTERELLE_STEM.builtInRegistryHolder().key(), EamBlocks.CHAMPIGNON_CAP.builtInRegistryHolder().key(),
				EamBlocks.GLOWING_CHAMPIGNON_CAP.builtInRegistryHolder().key());
		builder(HandBreakingModule.HAND_BREAKABLE).add(EamBlocks.BURNT_MUSHROOM_CAP.builtInRegistryHolder().key(), EamBlocks.BURNT_MUSHROOM_STEM.builtInRegistryHolder().key(), EamBlocks.SMOLDERING_MUSHROOM_CAP.builtInRegistryHolder().key());
		builder(BlockTags.CLIMBABLE).add(EamBlocks.GLOWSHROOM_VINES.builtInRegistryHolder().key(), EamBlocks.GLOWSHROOM_VINES_PLANT.builtInRegistryHolder().key());
		builder(BlockTags.MINEABLE_WITH_HOE).add(EamBlocks.DENSE_SHRUB.builtInRegistryHolder().key(),
				EamBlocks.BIG_SHRUB_LEAVES.builtInRegistryHolder().key());
		builder(BlockTags.SWORD_EFFICIENT).add(EamBlocks.DENSE_SHRUB.builtInRegistryHolder().key(),
				EamBlocks.BIG_SHRUB_LEAVES.builtInRegistryHolder().key());
		builder(BlockTags.MINEABLE_WITH_AXE).add(EamBlocks.BIG_SHRUB_STEM.builtInRegistryHolder().key());
		builder(BlockTags.REPLACEABLE_BY_MUSHROOMS).add(EamBlocks.SWAMP_GRASS.builtInRegistryHolder().key(),
				EamBlocks.TALL_SWAMP_GRASS.builtInRegistryHolder().key(), EamBlocks.SWAMP_SHRUB.builtInRegistryHolder().key());

		builder(ThroughLeavesModule.ARROWS_PASS).forceAddTag(BlockTags.LEAVES);
		// Wood a creeper blows out of a tree is lost, so explosions are no shortcut to logs.
		builder(MobDropsModule.LOST_TO_CREEPERS).forceAddTag(BlockTags.LOGS).forceAddTag(BlockTags.PLANKS);
		// Abandoned villages have been looted: no beds, chests, furnaces or hay.
		BlockItemTagAppender<Block> looted = builder(VillageModule.LOOTED);
		looted.forceAddTag(BlockTags.BEDS);
		looted.add(BlockItemIds.CHEST, BlockItemIds.TRAPPED_CHEST, BlockItemIds.FURNACE, BlockItemIds.SMOKER, BlockItemIds.BLAST_FURNACE,
				BlockItemIds.HAY_BLOCK);
		looted.add(EamBlocks.OBSIDIAN_BLAST_FURNACE.builtInRegistryHolder().key());
		// Mobs break these, but a wooden room is still safe to sleep in. Doors always count as walls.
		// The Fallen Lord's dashes go through earth, wood, cobblestone and tuff.
		BlockItemTagAppender<Block> smashes = builder(FallenLordModule.DASH_SMASHES);
		smashes.forceAddTag(BlockTags.DIRT).forceAddTag(BlockTags.LOGS).forceAddTag(BlockTags.PLANKS)
				.forceAddTag(BlockTags.WOODEN_STAIRS).forceAddTag(BlockTags.WOODEN_SLABS).forceAddTag(BlockTags.WOODEN_FENCES);
		smashes.add(BlockItemIds.COBBLESTONE, BlockItemIds.COBBLESTONE_STAIRS, BlockItemIds.COBBLESTONE_SLAB, BlockItemIds.COBBLESTONE_WALL,
						BlockItemIds.MOSSY_COBBLESTONE, BlockItemIds.MOSSY_COBBLESTONE_STAIRS, BlockItemIds.MOSSY_COBBLESTONE_SLAB,
						BlockItemIds.MOSSY_COBBLESTONE_WALL, BlockItemIds.GRAVEL, BlockItemIds.SAND, BlockItemIds.RED_SAND, BlockItemIds.DIRT_PATH,
						BlockItemIds.FARMLAND, BlockItemIds.CLAY);
		// And through tuff, the stone of the crypts.
		smashes.add(BlockItemIds.TUFF, BlockItemIds.TUFF_SLAB, BlockItemIds.TUFF_STAIRS, BlockItemIds.TUFF_WALL, BlockItemIds.CHISELED_TUFF,
				BlockItemIds.POLISHED_TUFF, BlockItemIds.POLISHED_TUFF_SLAB, BlockItemIds.POLISHED_TUFF_STAIRS, BlockItemIds.POLISHED_TUFF_WALL,
				BlockItemIds.TUFF_BRICKS, BlockItemIds.TUFF_BRICK_SLAB, BlockItemIds.TUFF_BRICK_STAIRS, BlockItemIds.TUFF_BRICK_WALL,
				BlockItemIds.CHISELED_TUFF_BRICKS);
		builder(SafeRoomModule.SAFE_WALLS).forceAddTag(BlockTags.PLANKS).forceAddTag(BlockTags.WOODEN_SLABS)
				.forceAddTag(BlockTags.WOODEN_STAIRS).forceAddTag(BlockTags.TRAPDOORS).forceAddTag(BlockTags.FENCE_GATES);
		// Deep bedrock holds like bedrock against bosses and worldgen.
		builder(BlockTags.WITHER_IMMUNE).add(EamBlocks.DEEP_BEDROCK.builtInRegistryHolder().key());
		builder(BlockTags.DRAGON_IMMUNE).add(EamBlocks.DEEP_BEDROCK.builtInRegistryHolder().key());
		builder(BlockTags.FEATURES_CANNOT_REPLACE).add(EamBlocks.DEEP_BEDROCK.builtInRegistryHolder().key());
		builder(BlockTags.GEODE_INVALID_BLOCKS).add(EamBlocks.DEEP_BEDROCK.builtInRegistryHolder().key());
	}
}
