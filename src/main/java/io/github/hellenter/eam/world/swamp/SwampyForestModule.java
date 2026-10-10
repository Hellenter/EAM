package io.github.hellenter.eam.world.swamp;

import io.github.hellenter.eam.Eam;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * The swampy forest: a huge temperate swamp of oak and birch forest, bog ponds 1 to 3 blocks deep, hummocks and dead
 * trunks, in three parts ({@link SwampyForestBiomes}), on terrain flattened almost to sea level ({@link SwampyForestTerrain}).
 * The biomes, their features and spawns are datagen JSON; this module only places the biomes into the overworld.
 * Flag: {@code swampy_forest}.
 */
public final class SwampyForestModule {
	private SwampyForestModule() {
	}

	/** The pond feature type exists whatever the config says, so the biome JSON always loads. */
	public static void register() {
		Registry.register(BuiltInRegistries.FEATURE_TYPE, Eam.id("swamp_pond"), SwampPondFeature.CODEC);
		Registry.register(BuiltInRegistries.FEATURE_TYPE, Eam.id("duckweed_mat"), DuckweedMatFeature.CODEC);
		Registry.register(BuiltInRegistries.FEATURE_TYPE, Eam.id("shelf_mushrooms"), ShelfMushroomFeature.CODEC);
		Registry.register(BuiltInRegistries.FEATURE_TYPE, Eam.id("fallen_log"), FallenLogFeature.CODEC);
		Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, Eam.id("swampy_flatness"), SwampyFlatness.CODEC);
	}

	public static void init() {
		SwampyForestBiomes.enable();
	}
}
