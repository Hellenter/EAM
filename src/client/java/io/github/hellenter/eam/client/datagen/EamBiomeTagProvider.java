package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.mobs.entity.lost.LostModule;
import io.github.hellenter.eam.content.EamWorldgen;
import io.github.hellenter.eam.dimension.UnderworldDimension;
import io.github.hellenter.eam.survival.thirst.ThirstModule;
import io.github.hellenter.eam.world.structures.outpost.OutpostModule;
import io.github.hellenter.eam.world.swamp.SwampyForestBiomes;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;

import io.github.hellenter.eam.mobs.entity.bat.VampireBatModule;
import io.github.hellenter.eam.mobs.entity.lurker.SwampLurkerModule;
import io.github.hellenter.eam.mobs.entity.spider.SpiderModule;
import io.github.hellenter.eam.mobs.entity.warg.WargModule;
import io.github.hellenter.eam.mobs.entity.wight.WightModule;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

import java.util.concurrent.CompletableFuture;

public class EamBiomeTagProvider extends FabricTagsProvider<Biome> {
	public EamBiomeTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, Registries.BIOME, registries);
	}

	@Override
	protected void addTags(HolderLookup.Provider registries) {
		builder(OutpostModule.HAS_OUTPOST)
				.add(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.FOREST, Biomes.FLOWER_FOREST, Biomes.BIRCH_FOREST,
						Biomes.OLD_GROWTH_BIRCH_FOREST, Biomes.DAPPLED_FOREST, Biomes.DARK_FOREST);
		builder(SwampyForestBiomes.IS_SWAMPY_FOREST)
				.add(SwampyForestBiomes.SWAMPY_FOREST, SwampyForestBiomes.SWAMPY_LAKES, SwampyForestBiomes.SWAMPY_HUMMOCKS);
		// Temperate land biomes a swampy forest region takes over; rivers, beaches, oceans, mountains and caves stay.
		builder(SwampyForestBiomes.REPLACEABLE)
				.add(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.FOREST, Biomes.FLOWER_FOREST, Biomes.BIRCH_FOREST,
						Biomes.OLD_GROWTH_BIRCH_FOREST, Biomes.DAPPLED_FOREST, Biomes.DARK_FOREST, Biomes.SWAMP, Biomes.MEADOW, Biomes.TAIGA,
						Biomes.OLD_GROWTH_PINE_TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA, Biomes.SAVANNA,
						// The terrain under a region is flattened whatever the biome, so every land biome there becomes swamp
						// too: rivers, hills and the odd hotter or colder patch. Oceans, beaches and caves stay.
						Biomes.RIVER, Biomes.FROZEN_RIVER, Biomes.STONY_SHORE, Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_FOREST,
						Biomes.WINDSWEPT_GRAVELLY_HILLS, Biomes.GROVE, Biomes.SNOWY_SLOPES, Biomes.JAGGED_PEAKS, Biomes.STONY_PEAKS,
						Biomes.FROZEN_PEAKS, Biomes.CHERRY_GROVE, Biomes.PALE_GARDEN, Biomes.JUNGLE, Biomes.SPARSE_JUNGLE,
						Biomes.BAMBOO_JUNGLE, Biomes.MANGROVE_SWAMP, Biomes.DESERT, Biomes.BADLANDS, Biomes.ERODED_BADLANDS,
						Biomes.WOODED_BADLANDS, Biomes.SAVANNA_PLATEAU, Biomes.WINDSWEPT_SAVANNA, Biomes.SNOWY_PLAINS,
						Biomes.SNOWY_TAIGA, Biomes.ICE_SPIKES);
		builder(BiomeTags.IS_OVERWORLD).addTag(SwampyForestBiomes.IS_SWAMPY_FOREST);
		builder(UnderworldWorldgen.IS_UNDERWORLD).addAll(UnderworldDimension.BIOMES);
		builder(UnderworldWorldgen.LAVA_LAKES).add(UnderworldDimension.LAVA_CAVES, UnderworldDimension.DEPTHS);
		builder(UnderworldWorldgen.FROZEN_LAKES).add(UnderworldDimension.ICE_CAVES);
		builder(BiomeTags.IS_FOREST).add(SwampyForestBiomes.SWAMPY_FOREST);
		builder(BiomeTags.ALLOWS_SURFACE_SLIME_SPAWNS).addTag(SwampyForestBiomes.IS_SWAMPY_FOREST);
		builder(BiomeTags.WATER_ON_MAP_OUTLINES).addTag(SwampyForestBiomes.IS_SWAMPY_FOREST);
		builder(ThirstModule.DIRTY_WATER)
				.forceAddTag(BiomeTags.IS_JUNGLE)
				.add(Biomes.SWAMP, Biomes.MANGROVE_SWAMP)
				.addTag(SwampyForestBiomes.IS_SWAMPY_FOREST);
		builder(SwampLurkerModule.SPAWNS_IN)
				.add(Biomes.SWAMP, Biomes.MANGROVE_SWAMP)
				.addTag(SwampyForestBiomes.IS_SWAMPY_FOREST);
		// Squads of the Lost roam the underworld's upper level, not its depths.
		builder(LostModule.SPAWNS_IN).add(UnderworldDimension.LAVA_CAVES, UnderworldDimension.MUSHROOM_FOREST,
				UnderworldDimension.STONE_WASTES, UnderworldDimension.LAKE_CAVES, UnderworldDimension.ICE_CAVES);
		// Wild garlic grows in the plains, meadows and every forest.
		builder(EamWorldgen.HAS_WILD_GARLIC)
				.forceAddTag(BiomeTags.IS_FOREST)
				.add(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.MEADOW);
		// Warg packs hunt the snowy taiga and the snowy groves above it.
		builder(WargModule.SPAWNS_IN).add(Biomes.SNOWY_TAIGA, Biomes.GROVE);
		builder(WightModule.SPAWNS_IN)
				.forceAddTag(BiomeTags.IS_JUNGLE)
				.add(Biomes.SWAMP, Biomes.MANGROVE_SWAMP)
				.addTag(SwampyForestBiomes.IS_SWAMPY_FOREST);
		// Wood spiders spawn wherever trees grow thick; the spawn rules still want a tree right next to them.
		builder(SpiderModule.WOOD_SPIDERS_SPAWN_IN)
				.forceAddTag(BiomeTags.IS_FOREST)
				.forceAddTag(BiomeTags.IS_TAIGA)
				.forceAddTag(BiomeTags.IS_JUNGLE)
				.add(Biomes.SWAMP, Biomes.MANGROVE_SWAMP)
				.addTag(SwampyForestBiomes.IS_SWAMPY_FOREST);
		// Vampire bat flocks hunt the whole upper underworld, not the depths below the deep bedrock.
		builder(VampireBatModule.SPAWNS_IN)
				.add(UnderworldDimension.LAVA_CAVES, UnderworldDimension.MUSHROOM_FOREST, UnderworldDimension.STONE_WASTES,
						UnderworldDimension.LAKE_CAVES, UnderworldDimension.ICE_CAVES);
		builder(SpiderModule.BLACK_WIDOWS_SPAWN_IN)
				.forceAddTag(BiomeTags.IS_JUNGLE)
				.add(Biomes.DARK_FOREST, Biomes.PALE_GARDEN);
		builder(EamWorldgen.HAS_FOREST_PEBBLES)
				.forceAddTag(BiomeTags.IS_FOREST)
				.add(SwampyForestBiomes.SWAMPY_HUMMOCKS)
				.forceAddTag(BiomeTags.IS_TAIGA);
		builder(EamWorldgen.HAS_MOUNTAIN_PEBBLES)
				.forceAddTag(BiomeTags.IS_MOUNTAIN)
				.forceAddTag(BiomeTags.IS_HILL);
		builder(EamWorldgen.HAS_SHORE_PEBBLES)
				.add(Biomes.STONY_SHORE);
		builder(EamWorldgen.HAS_SILVER_ORE)
				.forceAddTag(BiomeTags.IS_OVERWORLD);
		builder(EamWorldgen.HAS_MOUNTAIN_SILVER_ORE)
				.forceAddTag(BiomeTags.IS_MOUNTAIN)
				.forceAddTag(BiomeTags.IS_HILL);
		builder(EamWorldgen.HAS_MITHRIL_ORE)
				.forceAddTag(BiomeTags.IS_OVERWORLD);
		builder(EamWorldgen.HAS_RICH_MITHRIL_ORE)
				.forceAddTag(BiomeTags.IS_MOUNTAIN)
				.add(SwampyForestBiomes.SWAMPY_FOREST)
				.add(SwampyForestBiomes.SWAMPY_LAKES)
				.add(SwampyForestBiomes.SWAMPY_HUMMOCKS);
		// Any biome: the feature itself only places sticks next to trees.
		builder(EamWorldgen.HAS_GROUND_STICKS)
				.forceAddTag(BiomeTags.IS_OVERWORLD);
	}
}
