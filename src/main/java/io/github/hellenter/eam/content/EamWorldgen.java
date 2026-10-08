package io.github.hellenter.eam.content;

import io.github.hellenter.eam.Eam;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Worldgen keys for EAM content and the biome hooks that add them. The features themselves are datagen
 * JSON under {@code data/eam/worldgen}, so datapacks can change how often things spawn.
 */
public final class EamWorldgen {
	public static final ResourceKey<Feature> PEBBLE = feature("pebble");
	public static final ResourceKey<Feature> FLINT_SHARD = feature("flint_shard");
	public static final ResourceKey<Feature> GROUND_STICK = feature("ground_stick");
	/** Copper ore veins the size of diamond veins; they replace vanilla copper when the ores module is on. */
	public static final ResourceKey<Feature> ORE_COPPER = feature("ore_copper");

	/** Pebbles on the forest floor. */
	public static final ResourceKey<PlacedFeature> PEBBLE_FOREST = placed("pebble_forest");
	public static final ResourceKey<PlacedFeature> FLINT_SHARD_FOREST = placed("flint_shard_forest");
	/** Pebbles on stone and gravel in the mountains. */
	public static final ResourceKey<PlacedFeature> PEBBLE_MOUNTAIN = placed("pebble_mountain");
	public static final ResourceKey<PlacedFeature> FLINT_SHARD_MOUNTAIN = placed("flint_shard_mountain");
	/** Sticks on the ground under tree crowns. */
	public static final ResourceKey<PlacedFeature> GROUND_STICK_UNDER_TREES = placed("ground_stick_under_trees");
	public static final ResourceKey<PlacedFeature> ORE_COPPER_PLACED = placed("ore_copper");

	public static final TagKey<Biome> HAS_FOREST_PEBBLES = TagKey.create(Registries.BIOME, Eam.id("has_forest_pebbles"));
	public static final TagKey<Biome> HAS_MOUNTAIN_PEBBLES = TagKey.create(Registries.BIOME, Eam.id("has_mountain_pebbles"));
	public static final TagKey<Biome> HAS_GROUND_STICKS = TagKey.create(Registries.BIOME, Eam.id("has_ground_sticks"));

	private EamWorldgen() {
	}

	public static void init() {
		GenerationStep.Decoration step = GenerationStep.Decoration.VEGETAL_DECORATION;
		BiomeModifications.addFeature(BiomeSelectors.tag(HAS_FOREST_PEBBLES), step, PEBBLE_FOREST);
		BiomeModifications.addFeature(BiomeSelectors.tag(HAS_FOREST_PEBBLES), step, FLINT_SHARD_FOREST);
		BiomeModifications.addFeature(BiomeSelectors.tag(HAS_MOUNTAIN_PEBBLES), step, PEBBLE_MOUNTAIN);
		BiomeModifications.addFeature(BiomeSelectors.tag(HAS_MOUNTAIN_PEBBLES), step, FLINT_SHARD_MOUNTAIN);
		BiomeModifications.addFeature(BiomeSelectors.tag(HAS_GROUND_STICKS), step, GROUND_STICK_UNDER_TREES);
	}

	private static ResourceKey<Feature> feature(String name) {
		return ResourceKey.create(Registries.FEATURE, Eam.id(name));
	}

	private static ResourceKey<PlacedFeature> placed(String name) {
		return ResourceKey.create(Registries.PLACED_FEATURE, Eam.id(name));
	}
}
