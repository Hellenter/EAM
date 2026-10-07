package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.content.EamWorldgen;
import io.github.hellenter.eam.survival.thirst.ThirstModule;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;

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
		builder(ThirstModule.DIRTY_WATER)
				.forceAddTag(BiomeTags.IS_JUNGLE)
				.add(Biomes.SWAMP, Biomes.MANGROVE_SWAMP);
		builder(EamWorldgen.HAS_SCATTERED_PEBBLES)
				.forceAddTag(BiomeTags.IS_FOREST)
				.forceAddTag(BiomeTags.IS_TAIGA)
				.forceAddTag(BiomeTags.IS_MOUNTAIN)
				.forceAddTag(BiomeTags.IS_HILL);
		builder(EamWorldgen.HAS_PEBBLES_NEAR_GRAVEL)
				.forceAddTag(BiomeTags.IS_OVERWORLD);
	}
}
