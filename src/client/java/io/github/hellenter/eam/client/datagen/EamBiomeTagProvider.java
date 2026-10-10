package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.content.EamWorldgen;
import io.github.hellenter.eam.survival.thirst.ThirstModule;
import io.github.hellenter.eam.world.swamp.SwampyForestBiomes;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;

import io.github.hellenter.eam.mobs.entity.lurker.SwampLurkerModule;
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
		builder(SwampyForestBiomes.IS_SWAMPY_FOREST)
				.add(SwampyForestBiomes.SWAMPY_FOREST, SwampyForestBiomes.SWAMPY_LAKES, SwampyForestBiomes.SWAMPY_HUMMOCKS);
		// Temperate land biomes a swampy forest region takes over; rivers, beaches, oceans, mountains and caves stay.
		builder(SwampyForestBiomes.REPLACEABLE)
				.add(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.FOREST, Biomes.FLOWER_FOREST, Biomes.BIRCH_FOREST,
						Biomes.OLD_GROWTH_BIRCH_FOREST, Biomes.DARK_FOREST, Biomes.SWAMP, Biomes.MEADOW, Biomes.TAIGA,
						Biomes.OLD_GROWTH_PINE_TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA, Biomes.SAVANNA);
		builder(BiomeTags.IS_OVERWORLD).addTag(SwampyForestBiomes.IS_SWAMPY_FOREST);
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
		builder(WightModule.SPAWNS_IN)
				.forceAddTag(BiomeTags.IS_JUNGLE)
				.add(Biomes.SWAMP, Biomes.MANGROVE_SWAMP)
				.addTag(SwampyForestBiomes.IS_SWAMPY_FOREST);
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
		// Any biome: the feature itself only places sticks next to trees.
		builder(EamWorldgen.HAS_GROUND_STICKS)
				.forceAddTag(BiomeTags.IS_OVERWORLD);
	}
}
