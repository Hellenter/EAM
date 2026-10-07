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

	/** Pebbles scattered in forests and mountains. */
	public static final ResourceKey<PlacedFeature> PEBBLE_SCATTERED = placed("pebble_scattered");
	/** Pebbles next to surface gravel, in any overworld biome. */
	public static final ResourceKey<PlacedFeature> PEBBLE_NEAR_GRAVEL = placed("pebble_near_gravel");
	public static final ResourceKey<PlacedFeature> FLINT_SHARD_SCATTERED = placed("flint_shard_scattered");
	public static final ResourceKey<PlacedFeature> FLINT_SHARD_NEAR_GRAVEL = placed("flint_shard_near_gravel");

	/** Biomes with scattered pebbles and flint shards: forests and mountains. */
	public static final TagKey<Biome> HAS_SCATTERED_PEBBLES = TagKey.create(Registries.BIOME, Eam.id("has_scattered_pebbles"));
	/** Biomes where pebbles and flint shards lie next to surface gravel. */
	public static final TagKey<Biome> HAS_PEBBLES_NEAR_GRAVEL = TagKey.create(Registries.BIOME, Eam.id("has_pebbles_near_gravel"));

	private EamWorldgen() {
	}

	public static void init() {
		GenerationStep.Decoration step = GenerationStep.Decoration.VEGETAL_DECORATION;
		BiomeModifications.addFeature(BiomeSelectors.tag(HAS_SCATTERED_PEBBLES), step, PEBBLE_SCATTERED);
		BiomeModifications.addFeature(BiomeSelectors.tag(HAS_SCATTERED_PEBBLES), step, FLINT_SHARD_SCATTERED);
		BiomeModifications.addFeature(BiomeSelectors.tag(HAS_PEBBLES_NEAR_GRAVEL), step, PEBBLE_NEAR_GRAVEL);
		BiomeModifications.addFeature(BiomeSelectors.tag(HAS_PEBBLES_NEAR_GRAVEL), step, FLINT_SHARD_NEAR_GRAVEL);
	}

	private static ResourceKey<Feature> feature(String name) {
		return ResourceKey.create(Registries.FEATURE, Eam.id(name));
	}

	private static ResourceKey<PlacedFeature> placed(String name) {
		return ResourceKey.create(Registries.PLACED_FEATURE, Eam.id(name));
	}
}
