package io.github.hellenter.eam.dimension;

import io.github.hellenter.eam.Eam;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

import java.util.List;

/** Keys of the underworld: the dimension, its type and terrain settings, its biomes and tags. */
public final class UnderworldDimension {
	public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION, Eam.id("underworld"));
	public static final ResourceKey<DimensionType> TYPE = ResourceKey.create(Registries.DIMENSION_TYPE, Eam.id("underworld"));
	public static final ResourceKey<NoiseGeneratorSettings> NOISE_SETTINGS = ResourceKey.create(Registries.NOISE_SETTINGS, Eam.id("underworld"));

	/** Warmest to coldest, the order the biomes border each other in. */
	public static final ResourceKey<Biome> LAVA_CAVES = biome("lava_caves");
	public static final ResourceKey<Biome> MUSHROOM_FOREST = biome("mushroom_forest");
	public static final ResourceKey<Biome> STONE_WASTES = biome("stone_wastes");
	public static final ResourceKey<Biome> LAKE_CAVES = biome("lake_caves");
	public static final ResourceKey<Biome> ICE_CAVES = biome("ice_caves");
	/** The lower level, under the deep bedrock. */
	public static final ResourceKey<Biome> DEPTHS = biome("underworld_depths");
	public static final List<ResourceKey<Biome>> BIOMES = List.of(LAVA_CAVES, MUSHROOM_FOREST, STONE_WASTES, LAKE_CAVES, ICE_CAVES, DEPTHS);

	/** Items that break deep bedrock. */
	public static final TagKey<Item> BREAKS_DEEP_BEDROCK = TagKey.create(Registries.ITEM, Eam.id("breaks_deep_bedrock"));

	private UnderworldDimension() {
	}

	public static boolean is(Level level) {
		return level.dimension() == LEVEL;
	}

	private static ResourceKey<Biome> biome(String name) {
		return ResourceKey.create(Registries.BIOME, Eam.id(name));
	}
}
