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
	public static final ResourceKey<Feature> ORE_SILVER = feature("ore_silver");
	public static final ResourceKey<Feature> ORE_MITHRIL = feature("ore_mithril");
	/** Small adamantium veins; placed only in the underworld's lower level (see UnderworldWorldgen). */
	public static final ResourceKey<Feature> ORE_ADAMANTIUM = feature("ore_adamantium");
	public static final ResourceKey<Feature> WILD_GARLIC = feature("wild_garlic");

	/** Pebbles on the forest floor. */
	public static final ResourceKey<PlacedFeature> PEBBLE_FOREST = placed("pebble_forest");
	public static final ResourceKey<PlacedFeature> FLINT_SHARD_FOREST = placed("flint_shard_forest");
	/** Pebbles on stone and gravel in the mountains. */
	public static final ResourceKey<PlacedFeature> PEBBLE_MOUNTAIN = placed("pebble_mountain");
	public static final ResourceKey<PlacedFeature> FLINT_SHARD_MOUNTAIN = placed("flint_shard_mountain");
	/** Pebbles on stone and gravel along stony shores, more than anywhere else. */
	public static final ResourceKey<PlacedFeature> PEBBLE_SHORE = placed("pebble_shore");
	public static final ResourceKey<PlacedFeature> FLINT_SHARD_SHORE = placed("flint_shard_shore");
	/** Sticks on the ground next to trees, in any biome that has them. */
	public static final ResourceKey<PlacedFeature> GROUND_STICK_NEAR_TREES = placed("ground_stick_near_trees");
	public static final ResourceKey<PlacedFeature> ORE_COPPER_PLACED = placed("ore_copper");
	/** Small patches of wild garlic in plains and forests. */
	public static final ResourceKey<PlacedFeature> WILD_GARLIC_PATCH = placed("wild_garlic_patch");
	/** Silver veins in every overworld biome, 2.5 times fewer than copper. */
	public static final ResourceKey<PlacedFeature> ORE_SILVER_PLACED = placed("ore_silver");
	/** As many silver veins again in the mountains, so they have twice the silver. */
	public static final ResourceKey<PlacedFeature> ORE_SILVER_MOUNTAIN = placed("ore_silver_mountain");

	/** Mithril veins below y -32 in every overworld biome, much fewer than diamond. */
	public static final ResourceKey<PlacedFeature> ORE_MITHRIL_PLACED = placed("ore_mithril");
	/** Extra mithril veins in the mountains and the swampy forest, which keep more of it. */
	public static final ResourceKey<PlacedFeature> ORE_MITHRIL_RICH = placed("ore_mithril_rich");

	public static final TagKey<Biome> HAS_FOREST_PEBBLES = TagKey.create(Registries.BIOME, Eam.id("has_forest_pebbles"));
	public static final TagKey<Biome> HAS_MOUNTAIN_PEBBLES = TagKey.create(Registries.BIOME, Eam.id("has_mountain_pebbles"));
	public static final TagKey<Biome> HAS_SHORE_PEBBLES = TagKey.create(Registries.BIOME, Eam.id("has_shore_pebbles"));
	public static final TagKey<Biome> HAS_SILVER_ORE = TagKey.create(Registries.BIOME, Eam.id("has_silver_ore"));
	public static final TagKey<Biome> HAS_MOUNTAIN_SILVER_ORE = TagKey.create(Registries.BIOME, Eam.id("has_mountain_silver_ore"));
	public static final TagKey<Biome> HAS_MITHRIL_ORE = TagKey.create(Registries.BIOME, Eam.id("has_mithril_ore"));
	public static final TagKey<Biome> HAS_RICH_MITHRIL_ORE = TagKey.create(Registries.BIOME, Eam.id("has_rich_mithril_ore"));
	public static final TagKey<Biome> HAS_GROUND_STICKS = TagKey.create(Registries.BIOME, Eam.id("has_ground_sticks"));
	public static final TagKey<Biome> HAS_WILD_GARLIC = TagKey.create(Registries.BIOME, Eam.id("has_wild_garlic"));

	private EamWorldgen() {
	}

	public static void init() {
		GenerationStep.Decoration step = GenerationStep.Decoration.VEGETAL_DECORATION;
		BiomeModifications.addFeature(BiomeSelectors.tag(HAS_FOREST_PEBBLES), step, PEBBLE_FOREST);
		BiomeModifications.addFeature(BiomeSelectors.tag(HAS_FOREST_PEBBLES), step, FLINT_SHARD_FOREST);
		BiomeModifications.addFeature(BiomeSelectors.tag(HAS_MOUNTAIN_PEBBLES), step, PEBBLE_MOUNTAIN);
		BiomeModifications.addFeature(BiomeSelectors.tag(HAS_MOUNTAIN_PEBBLES), step, FLINT_SHARD_MOUNTAIN);
		BiomeModifications.addFeature(BiomeSelectors.tag(HAS_SHORE_PEBBLES), step, PEBBLE_SHORE);
		BiomeModifications.addFeature(BiomeSelectors.tag(HAS_SHORE_PEBBLES), step, FLINT_SHARD_SHORE);
		BiomeModifications.addFeature(BiomeSelectors.tag(HAS_GROUND_STICKS), step, GROUND_STICK_NEAR_TREES);
		BiomeModifications.addFeature(BiomeSelectors.tag(HAS_WILD_GARLIC), step, WILD_GARLIC_PATCH);
		GenerationStep.Decoration ores = GenerationStep.Decoration.UNDERGROUND_ORES;
		BiomeModifications.addFeature(BiomeSelectors.tag(HAS_SILVER_ORE), ores, ORE_SILVER_PLACED);
		BiomeModifications.addFeature(BiomeSelectors.tag(HAS_MOUNTAIN_SILVER_ORE), ores, ORE_SILVER_MOUNTAIN);
		BiomeModifications.addFeature(BiomeSelectors.tag(HAS_MITHRIL_ORE), ores, ORE_MITHRIL_PLACED);
		BiomeModifications.addFeature(BiomeSelectors.tag(HAS_RICH_MITHRIL_ORE), ores, ORE_MITHRIL_RICH);
	}

	private static ResourceKey<Feature> feature(String name) {
		return ResourceKey.create(Registries.FEATURE, Eam.id(name));
	}

	private static ResourceKey<PlacedFeature> placed(String name) {
		return ResourceKey.create(Registries.PLACED_FEATURE, Eam.id(name));
	}
}
