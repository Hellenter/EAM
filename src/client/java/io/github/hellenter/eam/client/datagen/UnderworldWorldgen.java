package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamBlocks;
import io.github.hellenter.eam.content.EamParticles;
import io.github.hellenter.eam.content.EamSounds;
import io.github.hellenter.eam.content.EamWorldgen;
import io.github.hellenter.eam.dimension.UnderworldDimension;
import io.github.hellenter.eam.dimension.UnderworldLogic;
import io.github.hellenter.eam.dimension.feature.CaveBoulderFeature;
import io.github.hellenter.eam.dimension.feature.CavePillarFeature;
import io.github.hellenter.eam.dimension.feature.CaveStreamFeature;
import io.github.hellenter.eam.dimension.feature.GiantMushroomFeature;
import io.github.hellenter.eam.dimension.feature.GiantMushroomFeature.Shape;
import io.github.hellenter.eam.dimension.feature.IceSpikeCaveFeature;
import io.github.hellenter.eam.dimension.feature.IceSpireFeature;
import io.github.hellenter.eam.mobs.entity.draugr.DraugrPackFeature;
import io.github.hellenter.eam.dimension.feature.UnderworldFluidsFeature;
import io.github.hellenter.eam.dimension.feature.UnderworldLakeFeature;
import io.github.hellenter.eam.world.darkness.DarknessLogic;
import io.github.hellenter.eam.world.darkness.DarknessModule;

import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;

import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BlockStateProviders;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.AquaticFeatures;
import net.minecraft.data.worldgen.features.CaveFeatures;
import net.minecraft.data.worldgen.features.MiscOverworldFeatures;
import net.minecraft.data.worldgen.features.OreFeatures;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.data.worldgen.features.VegetationFeatures;
import net.minecraft.data.worldgen.material.VanillaMaterialConditions;
import net.minecraft.data.worldgen.material.VanillaMaterialRules;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ARGB;
import net.minecraft.util.CubicSpline;
import net.minecraft.util.random.WeightedList;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.util.valueproviders.WeightedListInt;
import net.minecraft.world.attribute.AmbientMoodSettings;
import net.minecraft.world.attribute.AmbientParticle;
import net.minecraft.world.attribute.AmbientAdditionsSettings;
import net.minecraft.world.attribute.AmbientSounds;
import net.minecraft.world.attribute.BackgroundMusic;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.attribute.modifier.FloatModifier;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseRouterData;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunctions;
import net.minecraft.world.level.levelgen.densityfunction.op.SplineFunction;
import net.minecraft.world.level.levelgen.feature.BlockColumnFeature;
import net.minecraft.world.level.levelgen.feature.FallenTreeFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.SpruceFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.treedecorators.AttachedToLogsDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.ShelfMushroomDecorator;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.RandomizedIntStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.feature.SimpleBlockFeature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.material.MaterialRules;
import net.minecraft.world.level.levelgen.material.condition.MaterialCondition;
import net.minecraft.world.level.levelgen.material.rule.MaterialRule;
import net.minecraft.world.level.levelgen.placement.CaveSurface;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.EnvironmentScanPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.OffsetPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

/**
 * The underworld's datagen: its dimension type, terrain (noise settings, density functions, material rules), its
 * biomes and their features. The level stem itself is the hand-written {@code data/eam/dimension/underworld.json}, which
 * lists the biomes by temperature.
 * <p>
 * Terrain: the nether's 3D noise in both levels, each with its own floor and ceiling slides, and solid rock between
 * them where the material rules lay the deep bedrock. The temperature that picks the biome also opens the caves up (the
 * mushroom forest most); the ice caves are one huge hall instead, floor to roof, and the frozen caves and the underground
 * forest are low caves stacked in many levels. A second noise, the humidity, splits the band between the stone wastes and
 * the underground forest into lake caves and river caves. The aquifer floods only the lower
 * level (a lava sea); upper lakes are dug by {@link UnderworldLakeFeature}, lowest floor first, and
 * {@link UnderworldFluidsFeature} turns them to lava in the hot biomes and freezes them in the ice caves.
 */
final class UnderworldWorldgen {
	static final ResourceKey<NormalNoise> TEMPERATURE_NOISE = ResourceKey.create(Registries.NOISE, Eam.id("underworld/temperature"));
	static final ResourceKey<DensityFunction> TEMPERATURE = ResourceKey.create(Registries.DENSITY_FUNCTION, Eam.id("underworld/temperature"));
	static final ResourceKey<NormalNoise> HUMIDITY_NOISE = ResourceKey.create(Registries.NOISE, Eam.id("underworld/humidity"));
	static final ResourceKey<DensityFunction> HUMIDITY = ResourceKey.create(Registries.DENSITY_FUNCTION, Eam.id("underworld/humidity"));
	static final ResourceKey<MaterialRule> MATERIAL_RULE = ResourceKey.create(Registries.MATERIAL_RULE, Eam.id("underworld"));
	/** Hot biomes whose lakes are lava, and cold ones whose lakes freeze. */
	static final TagKey<Biome> LAVA_LAKES = TagKey.create(Registries.BIOME, Eam.id("underworld_lava_lakes"));
	static final TagKey<Biome> FROZEN_LAKES = TagKey.create(Registries.BIOME, Eam.id("underworld_frozen_lakes"));
	static final TagKey<Biome> IS_UNDERWORLD = TagKey.create(Registries.BIOME, Eam.id("is_underworld"));

	/**
	 * Temperature edges between the biomes' bands, warm to cold: each is the cold edge of the biome it is named after.
	 * Keep in step with the hand-written bands in data/eam/dimension/underworld.json. The ice caves take the widest share:
	 * the boss monument will stand there. The stone wastes have two bands, one on each side of the lake and river caves.
	 */
	static final float LAVA_EDGE = 0.36F;
	static final float BURNT_EDGE = 0.28F;
	static final float MUSHROOM_EDGE = 0.19F;
	static final float WARM_STONE_EDGE = 0.11F;
	static final float FOREST_EDGE = 0.03F;
	static final float LAKE_EDGE = -0.055F;
	static final float STONE_EDGE = -0.13F;
	static final float FROZEN_EDGE = -0.2F;
	static final float ICE_EDGE = FROZEN_EDGE;
	/** Temperature at the middle of each biome's band. */
	static final float LAVA_T = (LAVA_EDGE + 1.0F) / 2.0F;
	static final float BURNT_T = (BURNT_EDGE + LAVA_EDGE) / 2.0F;
	static final float MUSHROOM_T = (MUSHROOM_EDGE + BURNT_EDGE) / 2.0F;
	static final float WARM_STONE_T = (WARM_STONE_EDGE + MUSHROOM_EDGE) / 2.0F;
	static final float FOREST_T = (FOREST_EDGE + WARM_STONE_EDGE) / 2.0F;
	static final float LAKE_T = (LAKE_EDGE + FOREST_EDGE) / 2.0F;
	static final float STONE_T = (STONE_EDGE + LAKE_EDGE) / 2.0F;
	static final float FROZEN_T = (FROZEN_EDGE + STONE_EDGE) / 2.0F;
	static final float ICE_T = (ICE_EDGE - 1.0F) / 2.0F;

	private static final int UPPER_BOTTOM = UnderworldLogic.BAND_TOP + 2;
	private static final int UPPER_TOP = UnderworldLogic.HEIGHT - 6;
	/** Where the ice caves' hall has its floor and its roof. */
	private static final int ICE_FLOOR = 64;
	private static final int ICE_ROOF = 172;
	/** The lava caves turn to solid rock above this height, so they are lower than the other biomes' halls. */
	private static final int LAVA_ROOF = 128;
	/**
	 * The layered caves: a level every LAYER_PERIOD blocks of height, its density from LAYER_SLAB in the rock between
	 * levels to LAYER_OPEN in the middle of one, rising and falling by up to about LAYER_WAVE / 2 blocks, and roughened by
	 * LAYER_ROUGHNESS times the nether noise.
	 */
	private static final int LAYER_PERIOD = 13;
	/** The underground forest has fewer, taller and more open levels than the frozen caves, solid rock from FOREST_ROOF up. */
	private static final int FOREST_LAYER_PERIOD = 17;
	private static final float FOREST_LAYER_OPEN = -1.0F;
	private static final int FOREST_ROOF = 124;
	private static final float LAYER_SLAB = 0.5F;
	private static final float LAYER_OPEN = -0.5F;
	private static final float LAYER_WAVE = 10.0F;
	private static final float LAYER_ROUGHNESS = 0.45F;

	static final ResourceKey<Feature> FLUIDS = feature("underworld_fluids");
	static final ResourceKey<Feature> GIANT_BROWN_MUSHROOM = feature("giant_brown_mushroom");
	static final ResourceKey<Feature> GIANT_RED_MUSHROOM = feature("giant_red_mushroom");
	static final ResourceKey<Feature> ICE_SPIKE = feature("underworld_ice_spike");
	static final ResourceKey<Feature> COBWEB = feature("underworld_cobweb");
	static final ResourceKey<Feature> LAKE = feature("underworld_lake");
	static final ResourceKey<Feature> LAVA_LAKE = feature("underworld_lava_lake");
	static final ResourceKey<Feature> LAVA_PILLAR = feature("underworld_lava_pillar");
	static final ResourceKey<Feature> ICE_SPIRE = feature("underworld_ice_spire");
	static final ResourceKey<Feature> GIANT_CHANTERELLE = feature("giant_chanterelle");
	static final ResourceKey<Feature> GIANT_CHAMPIGNON = feature("giant_champignon");
	static final ResourceKey<Feature> DRAUGR_PACK = feature("underworld_draugr_pack");
	static final ResourceKey<Feature> STREAM = feature("underworld_stream");
	static final ResourceKey<Feature> RIVER = feature("underworld_river");
	static final ResourceKey<Feature> SMALL_ICE_SPIRE = feature("underworld_small_ice_spire");
	static final ResourceKey<Feature> SMALL_ICE_SPIKE = feature("underworld_small_ice_spike");
	static final ResourceKey<Feature> SMALL_SPRUCE = feature("underworld_small_spruce");
	static final ResourceKey<Feature> FOREST_PLANTS = feature("underground_forest_plants");
	static final ResourceKey<Feature> GLOWSHROOM_VINES = feature("glowshroom_vines");
	static final ResourceKey<Feature> SHORE_PLANTS = feature("underworld_shore_plants");
	static final ResourceKey<Feature> BURNT_BROWN_MUSHROOM = feature("burnt_brown_mushroom");
	static final ResourceKey<Feature> BURNT_RED_MUSHROOM = feature("burnt_red_mushroom");
	static final ResourceKey<Feature> SMOLDERING_MUSHROOM = feature("smoldering_mushroom");
	// Small decorations, each rare so they dot a biome without cluttering it.
	static final ResourceKey<Feature> FALLEN_SPRUCE = feature("underworld_fallen_spruce");
	static final ResourceKey<Feature> FALLEN_POPLAR = feature("underworld_fallen_poplar");
	static final ResourceKey<Feature> FALLEN_FROZEN_SPRUCE = feature("underworld_fallen_frozen_spruce");
	static final ResourceKey<Feature> FALLEN_MUSHROOM_STEM = feature("underworld_fallen_mushroom_stem");
	static final ResourceKey<Feature> FALLEN_BURNT_STEM = feature("underworld_fallen_burnt_stem");
	static final ResourceKey<Feature> OLD_BONES = feature("underworld_old_bones");
	static final ResourceKey<Feature> MOSSY_BOULDER = feature("underworld_mossy_boulder");
	static final ResourceKey<Feature> ROCK_PILE = feature("underworld_rock_pile");
	static final ResourceKey<Feature> ICE_BOULDER = feature("underworld_ice_boulder");
	static final ResourceKey<Feature> BIG_ICE_BOULDER = feature("underworld_big_ice_boulder");
	static final ResourceKey<Feature> SCORCHED_BOULDER = feature("underworld_scorched_boulder");
	static final ResourceKey<Feature> BASALT_BOULDER = feature("underworld_basalt_boulder");
	static final List<ResourceKey<Feature>> FEATURES = List.of(FLUIDS, GIANT_BROWN_MUSHROOM, GIANT_RED_MUSHROOM, ICE_SPIKE, COBWEB, LAKE, LAVA_LAKE,
			LAVA_PILLAR, ICE_SPIRE, GIANT_CHANTERELLE, GIANT_CHAMPIGNON, DRAUGR_PACK, STREAM, RIVER, SMALL_ICE_SPIRE, SMALL_ICE_SPIKE, SMALL_SPRUCE,
			FOREST_PLANTS, GLOWSHROOM_VINES, SHORE_PLANTS, BURNT_BROWN_MUSHROOM, BURNT_RED_MUSHROOM, SMOLDERING_MUSHROOM, FALLEN_SPRUCE, FALLEN_POPLAR,
			FALLEN_FROZEN_SPRUCE, FALLEN_MUSHROOM_STEM, FALLEN_BURNT_STEM, OLD_BONES, MOSSY_BOULDER, ROCK_PILE, ICE_BOULDER, BIG_ICE_BOULDER,
			SCORCHED_BOULDER, BASALT_BOULDER);

	/**
	 * Every placed feature of the underworld, in the one order all its biomes share (a biome takes a subset), so
	 * feature order never cycles. Values are the decoration step.
	 */
	private static final Map<ResourceKey<PlacedFeature>, GenerationStep.Decoration> ORDER = new LinkedHashMap<>();
	static final ResourceKey<PlacedFeature> P_LAKES_FEW = placed("underworld/lakes_few", GenerationStep.Decoration.LAKES);
	static final ResourceKey<PlacedFeature> P_LAKES = placed("underworld/lakes", GenerationStep.Decoration.LAKES);
	static final ResourceKey<PlacedFeature> P_LAKES_MANY = placed("underworld/lakes_many", GenerationStep.Decoration.LAKES);
	static final ResourceKey<PlacedFeature> P_LAVA_LAKES = placed("underworld/lava_lakes", GenerationStep.Decoration.LAKES);
	/** Streams and rivers before the fluids, which would freeze or burn them in the cold and hot biomes. */
	static final ResourceKey<PlacedFeature> P_STREAMS = placed("underworld/streams", GenerationStep.Decoration.LAKES);
	static final ResourceKey<PlacedFeature> P_RIVERS = placed("underworld/rivers", GenerationStep.Decoration.LAKES);
	static final ResourceKey<PlacedFeature> P_FLUIDS = placed("underworld/fluids", GenerationStep.Decoration.LAKES);
	static final ResourceKey<PlacedFeature> P_ORE_IRON = placed("underworld/ore_iron", GenerationStep.Decoration.UNDERGROUND_ORES);
	static final ResourceKey<PlacedFeature> P_ORE_GOLD = placed("underworld/ore_gold", GenerationStep.Decoration.UNDERGROUND_ORES);
	static final ResourceKey<PlacedFeature> P_ORE_COPPER = placed("underworld/ore_copper", GenerationStep.Decoration.UNDERGROUND_ORES);
	static final ResourceKey<PlacedFeature> P_ORE_SILVER = placed("underworld/ore_silver", GenerationStep.Decoration.UNDERGROUND_ORES);
	static final ResourceKey<PlacedFeature> P_ORE_MITHRIL = placed("underworld/ore_mithril", GenerationStep.Decoration.UNDERGROUND_ORES);
	static final ResourceKey<PlacedFeature> P_ORE_MITHRIL_DEPTHS = placed("underworld/ore_mithril_depths", GenerationStep.Decoration.UNDERGROUND_ORES);
	static final ResourceKey<PlacedFeature> P_ORE_ADAMANTIUM = placed("underworld/ore_adamantium", GenerationStep.Decoration.UNDERGROUND_ORES);
	static final ResourceKey<PlacedFeature> P_ORE_DIAMOND = placed("underworld/ore_diamond", GenerationStep.Decoration.UNDERGROUND_ORES);
	static final ResourceKey<PlacedFeature> P_ORE_REDSTONE = placed("underworld/ore_redstone", GenerationStep.Decoration.UNDERGROUND_ORES);
	static final ResourceKey<PlacedFeature> P_ORE_LAPIS = placed("underworld/ore_lapis", GenerationStep.Decoration.UNDERGROUND_ORES);
	// Biome extras on top of the common ores: gold and redstone in the hot biomes, the rare ores and lapis in the ice caves.
	static final ResourceKey<PlacedFeature> P_ORE_GOLD_HOT = placed("underworld/ore_gold_hot", GenerationStep.Decoration.UNDERGROUND_ORES);
	static final ResourceKey<PlacedFeature> P_ORE_REDSTONE_HOT = placed("underworld/ore_redstone_hot", GenerationStep.Decoration.UNDERGROUND_ORES);
	static final ResourceKey<PlacedFeature> P_ORE_DIAMOND_ICE = placed("underworld/ore_diamond_ice", GenerationStep.Decoration.UNDERGROUND_ORES);
	static final ResourceKey<PlacedFeature> P_ORE_SILVER_ICE = placed("underworld/ore_silver_ice", GenerationStep.Decoration.UNDERGROUND_ORES);
	static final ResourceKey<PlacedFeature> P_ORE_MITHRIL_ICE = placed("underworld/ore_mithril_ice", GenerationStep.Decoration.UNDERGROUND_ORES);
	static final ResourceKey<PlacedFeature> P_ORE_LAPIS_ICE = placed("underworld/ore_lapis_ice", GenerationStep.Decoration.UNDERGROUND_ORES);
	static final ResourceKey<PlacedFeature> P_ORE_GRAVEL = placed("underworld/ore_gravel", GenerationStep.Decoration.UNDERGROUND_ORES);
	static final ResourceKey<PlacedFeature> P_ORE_MAGMA = placed("underworld/ore_magma", GenerationStep.Decoration.UNDERGROUND_ORES);
	static final ResourceKey<PlacedFeature> P_SPRING_WATER = placed("underworld/spring_water", GenerationStep.Decoration.FLUID_SPRINGS);
	static final ResourceKey<PlacedFeature> P_SPRING_WATER_MANY = placed("underworld/spring_water_many", GenerationStep.Decoration.FLUID_SPRINGS);
	static final ResourceKey<PlacedFeature> P_SPRING_LAVA = placed("underworld/spring_lava", GenerationStep.Decoration.FLUID_SPRINGS);
	static final ResourceKey<PlacedFeature> P_SPRING_LAVA_MANY = placed("underworld/spring_lava_many", GenerationStep.Decoration.FLUID_SPRINGS);
	static final ResourceKey<PlacedFeature> P_LAVA_PILLAR = placed("underworld/lava_pillar", GenerationStep.Decoration.LOCAL_MODIFICATIONS);
	static final ResourceKey<PlacedFeature> P_LAVA_PILLAR_DEPTHS = placed("underworld/lava_pillar_depths", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_ICE_SPIRE = placed("underworld/ice_spire", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_ICE_SPIKE = placed("underworld/ice_spike", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_GIANT_BROWN = placed("underworld/giant_brown_mushroom", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_GIANT_RED = placed("underworld/giant_red_mushroom", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_GIANT_CHANTERELLE = placed("underworld/giant_chanterelle", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_GIANT_CHAMPIGNON = placed("underworld/giant_champignon", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_HUGE_BROWN = placed("underworld/huge_brown_mushroom", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_HUGE_RED = placed("underworld/huge_red_mushroom", GenerationStep.Decoration.VEGETAL_DECORATION);
	/**
	 * Decorations after the big mushrooms and before the small plants, which then grow around them: fallen trunks with
	 * mushrooms, boulders, old bones. Each on its biome's own floor blocks only, so never on a mushroom cap or a frozen lake.
	 */
	static final ResourceKey<PlacedFeature> P_FALLEN_SPRUCE = placed("underworld/fallen_spruce", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_FALLEN_POPLAR = placed("underworld/fallen_poplar", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_FALLEN_FROZEN_SPRUCE = placed("underworld/fallen_frozen_spruce", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_FALLEN_MUSHROOM_STEM = placed("underworld/fallen_mushroom_stem", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_FALLEN_BURNT_STEM = placed("underworld/fallen_burnt_stem", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_OLD_BONES = placed("underworld/old_bones", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_MOSSY_BOULDER_FOREST = placed("underworld/mossy_boulder_forest", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_MOSSY_BOULDER_MUSHROOM = placed("underworld/mossy_boulder_mushroom", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_MOSSY_BOULDER_LAKE = placed("underworld/mossy_boulder_lake", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_ROCK_PILE = placed("underworld/rock_pile", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_ICE_BOULDER = placed("underworld/ice_boulder", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_BIG_ICE_BOULDER = placed("underworld/big_ice_boulder", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_SCORCHED_BOULDER = placed("underworld/scorched_boulder", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_BASALT_BOULDER = placed("underworld/basalt_boulder", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_BROWN_MUSHROOMS = placed("underworld/brown_mushrooms", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_RED_MUSHROOMS = placed("underworld/red_mushrooms", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_BROWN_MUSHROOMS_FEW = placed("underworld/brown_mushrooms_few", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_CAVE_VINES = placed("underworld/cave_vines", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_COBWEB = placed("underworld/cobweb", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_GLOW_LICHEN = placed("underworld/glow_lichen", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_GLOW_LICHEN_MANY = placed("underworld/glow_lichen_many", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_SMALL_ICE_SPIRE = placed("underworld/small_ice_spire", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_SMALL_ICE_SPIKE = placed("underworld/small_ice_spike", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_BURNT_BROWN = placed("underworld/burnt_brown_mushroom", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_BURNT_RED = placed("underworld/burnt_red_mushroom", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_SMOLDERING = placed("underworld/smoldering_mushroom", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_DEAD_BUSHES = placed("underworld/dead_bushes", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_SPRUCE = placed("underworld/spruce", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_SMALL_SPRUCE = placed("underworld/small_spruce", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_FOREST_MOSS = placed("underworld/forest_moss", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_FOREST_PLANTS = placed("underworld/forest_plants", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_GLOWSHROOM_VINES = placed("underworld/glowshroom_vines", GenerationStep.Decoration.VEGETAL_DECORATION);
	/** The lake and river caves' small plants: moss with azaleas, orchids, ferns, dripleaves, cane by the water. */
	static final ResourceKey<PlacedFeature> P_MOSS_PATCH = placed("underworld/moss_patch", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_DRIPLEAF_CLAY = placed("underworld/dripleaf_clay", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_ORCHIDS = placed("underworld/orchids", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_FERNS = placed("underworld/ferns", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_SHORE_PLANTS = placed("underworld/shore_plants", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_SUGAR_CANE = placed("underworld/sugar_cane", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_SPORE_BLOSSOM = placed("underworld/spore_blossom", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_KELP = placed("underworld/kelp", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_SEAGRASS = placed("underworld/seagrass", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_LILY_PADS = placed("underworld/lily_pads", GenerationStep.Decoration.VEGETAL_DECORATION);
	/** Last, so the draugr lie in a floor that has its ice spires already. */
	static final ResourceKey<PlacedFeature> P_DRAUGR_PACK = placed("underworld/draugr_pack", GenerationStep.Decoration.VEGETAL_DECORATION);

	private UnderworldWorldgen() {
	}

	// ---- Noise and density functions ----

	static void bootstrapNoises(BootstrapContext<NormalNoise> context) {
		// Regions: most bands of temperature are one or two hundred blocks across, the ice caves widest.
		context.register(TEMPERATURE_NOISE, NormalNoise.createParity(-8, 1.0, 1.0, 0.5));
		// Smaller patches: which of the lake and river caves a stretch of that band is.
		context.register(HUMIDITY_NOISE, NormalNoise.createParity(-7, 1.0, 1.0));
	}

	static void bootstrapDensityFunctions(BootstrapContext<DensityFunction> context) {
		HolderGetter<DensityFunction> functions = context.lookup(Registries.DENSITY_FUNCTION);
		HolderGetter<NormalNoise> noises = context.lookup(Registries.NOISE);
		context.register(TEMPERATURE, DensityFunctions.shiftedNoise2d(
				NoiseRouterData.getFunction(functions, NoiseRouterData.SHIFT_X), NoiseRouterData.getFunction(functions, NoiseRouterData.SHIFT_Z),
				0.13, noises.getOrThrow(TEMPERATURE_NOISE)));
		context.register(HUMIDITY, DensityFunctions.shiftedNoise2d(
				NoiseRouterData.getFunction(functions, NoiseRouterData.SHIFT_X), NoiseRouterData.getFunction(functions, NoiseRouterData.SHIFT_Z),
				0.2, noises.getOrThrow(HUMIDITY_NOISE)));
	}

	/**
	 * Layered caves: rock slabs and open floors (density {@code open} in their middle) taking turns every {@code period}
	 * blocks of the wavy {@code height}, roughened by LAYER_ROUGHNESS times the nether noise, rock below the upper level.
	 */
	private static DensityFunction layered(SplineFunction.Coordinate height, int period, float open, DensityFunction base) {
		CubicSpline.Builder<SplineFunction.Coordinate> levels = CubicSpline.builder(height);
		for (int y = UPPER_BOTTOM - period; y < UnderworldLogic.HEIGHT + period; y += period) {
			levels.addPoint(y, LAYER_SLAB);
			levels.addPoint(y + period / 2.0F, open);
		}
		DensityFunction layers = DensityFunctions.add(DensityFunctions.spline(levels.build()),
				DensityFunctions.mul(DensityFunctions.constant(LAYER_ROUGHNESS), base));
		return NoiseRouterData.slide(layers, 0, UnderworldLogic.HEIGHT, 22, 5, 0.9375F, UPPER_BOTTOM, UPPER_BOTTOM + 13, 2.5F);
	}

	static void bootstrapNoiseSettings(BootstrapContext<NoiseGeneratorSettings> context) {
		HolderGetter<DensityFunction> functions = context.lookup(Registries.DENSITY_FUNCTION);
		HolderGetter<NormalNoise> noises = context.lookup(Registries.NOISE);
		DensityFunction temperature = NoiseRouterData.getFunction(functions, TEMPERATURE);
		SplineFunction.Coordinate t = new SplineFunction.Coordinate(temperature);
		DensityFunction base = NoiseRouterData.getFunction(functions, NoiseRouterData.BASE_3D_NOISE_NETHER);

		// How much more open (negative) the caves are, by biome: the mushroom forest has the tallest halls.
		DensityFunction openness = DensityFunctions.spline(CubicSpline.builder(t)
				.addPoint(-1.0F, -0.12F)
				.addPoint(ICE_T, -0.12F)
				.addPoint(FROZEN_T, 0.0F)
				.addPoint(STONE_T, 0.0F)
				.addPoint(LAKE_T, -0.12F)
				.addPoint(FOREST_T, -0.04F)
				.addPoint(WARM_STONE_T, 0.0F)
				.addPoint(MUSHROOM_T, -0.34F)
				.addPoint(BURNT_T, -0.24F)
				.addPoint(LAVA_T, -0.08F)
				.addPoint(1.0F, -0.08F)
				.build());
		// Upper level: rock below y 53 rising to open noise by y 66, and back to rock from y 170 to the roof.
		DensityFunction caves = NoiseRouterData.slide(DensityFunctions.add(base, openness), 0, UnderworldLogic.HEIGHT,
				22, 5, 0.9375F, UPPER_BOTTOM, UPPER_BOTTOM + 13, 2.5F);
		// The ice caves are one hall as tall as the level can hold: a rough floor near y 64, a roof near y 172, and the
		// ice spires growing between them.
		DensityFunction roughness = DensityFunctions.add(
				DensityFunctions.mul(DensityFunctions.constant(0.35F), DensityFunctions.noise(noises.getOrThrow(Noises.ICE), 1.0, 0.0)),
				DensityFunctions.mul(DensityFunctions.constant(0.12F), base));
		DensityFunction hall = DensityFunctions.add(DensityFunctions.max(
						DensityFunctions.yClampedGradient(UPPER_BOTTOM + 2, ICE_FLOOR + 10, 1.0F, -1.0F),
						DensityFunctions.yClampedGradient(ICE_ROOF - 10, UPPER_TOP - 2, -1.0F, 1.0F)),
				roughness);
		DensityFunction iceWeight = DensityFunctions.spline(CubicSpline.builder(t)
				.addPoint(ICE_EDGE - 0.08F, 1.0F)
				.addPoint(ICE_EDGE + 0.02F, 0.0F)
				.build());
		// The lava caves are lower: rock closes in from y 100 and is solid by the roof height.
		DensityFunction lavaWeight = DensityFunctions.spline(CubicSpline.builder(t)
				.addPoint(LAVA_EDGE - 0.03F, 0.0F)
				.addPoint(LAVA_EDGE + 0.02F, 1.0F)
				.build());
		DensityFunction lavaRoof = DensityFunctions.mul(lavaWeight, DensityFunctions.yClampedGradient(LAVA_ROOF - 28, LAVA_ROOF, 0.0F, 1.5F));
		caves = DensityFunctions.add(caves, lavaRoof);
		// The frozen caves and the underground forest are low caves in many levels: rock slabs and open floors take turns
		// every LAYER_PERIOD blocks of height, the levels rise and fall with a broad noise, and the nether noise breaks
		// through the slabs here and there so the levels run into each other.
		DensityFunction wave = DensityFunctions.mul(DensityFunctions.constant(LAYER_WAVE),
				DensityFunctions.noise(noises.getOrThrow(Noises.CAVE_LAYER), 4.0, 0.0));
		SplineFunction.Coordinate height = new SplineFunction.Coordinate(DensityFunctions.add(
				DensityFunctions.yClampedGradient(0, UnderworldLogic.HEIGHT, 0.0F, UnderworldLogic.HEIGHT), wave));
		DensityFunction frozenWeight = DensityFunctions.spline(CubicSpline.builder(t)
				.addPoint(ICE_EDGE + 0.01F, 0.0F)
				.addPoint(ICE_EDGE + 0.04F, 1.0F)
				.addPoint(STONE_EDGE - 0.02F, 1.0F)
				.addPoint(STONE_EDGE + 0.02F, 0.0F)
				.build());
		caves = DensityFunctions.lerp(frozenWeight, caves, layered(height, LAYER_PERIOD, LAYER_OPEN, base));
		// The forest's levels are taller and stop lower: solid rock closes in over the top one.
		DensityFunction forestWeight = DensityFunctions.spline(CubicSpline.builder(t)
				.addPoint(FOREST_EDGE - 0.03F, 0.0F)
				.addPoint(FOREST_EDGE + 0.01F, 1.0F)
				.addPoint(WARM_STONE_EDGE - 0.02F, 1.0F)
				.addPoint(WARM_STONE_EDGE + 0.02F, 0.0F)
				.build());
		DensityFunction forest = DensityFunctions.add(layered(height, FOREST_LAYER_PERIOD, FOREST_LAYER_OPEN, base),
				DensityFunctions.yClampedGradient(FOREST_ROOF - 12, FOREST_ROOF, 0.0F, 1.5F));
		caves = DensityFunctions.lerp(forestWeight, caves, forest);
		DensityFunction upper = DensityFunctions.lerp(iceWeight, caves, hall);
		// Lower level: wide, lower halls between the floor bedrock and the band.
		DensityFunction lower = NoiseRouterData.slide(DensityFunctions.add(base, DensityFunctions.constant(-0.14F)), 0, UnderworldLogic.HEIGHT,
				UnderworldLogic.HEIGHT - 36, UnderworldLogic.HEIGHT - (UnderworldLogic.BAND_BOTTOM - 1), 2.5F, 4, 10, 2.5F);
		DensityFunction finalDensity = DensityFunctions.add(NoiseRouterData.postProcess(DensityFunctions.min(upper, lower), 4, 8),
				DensityFunctions.beardifier());

		// Depth is 1 in the lower level and 0 in the upper one, so the lower level is always the depths biome.
		DensityFunction depth = DensityFunctions.yClampedGradient(UnderworldLogic.LEVEL_SPLIT - 2, UnderworldLogic.LEVEL_SPLIT + 2, 1.0F, 0.0F);
		NoiseRouter router = new NoiseRouter(temperature, NoiseRouterData.getFunction(functions, HUMIDITY), DensityFunctions.zero(),
				DensityFunctions.zero(), depth,
				DensityFunctions.zero(), DensityFunctions.zero(), finalDensity);

		DensityFunction floodNoise = DensityFunctions.mul(DensityFunctions.constant(0.3F),
				DensityFunctions.noise(noises.getOrThrow(Noises.AQUIFER_FLUID_LEVEL_FLOODEDNESS), 1.0, 0.67));
		// The depths flood into a lava sea; the upper level stays dry (at 0.4 and below a cave holds no fluid), its lakes
		// are dug by features with thick walls instead of the aquifer's thin ones.
		DensityFunction flooded = DensityFunctions.lerp(
				DensityFunctions.yClampedGradient(UnderworldLogic.BAND_BOTTOM - 2, UnderworldLogic.BAND_TOP, 0.0F, 1.0F),
				DensityFunctions.add(DensityFunctions.constant(0.5F), floodNoise),
				DensityFunctions.constant(-1.0F));
		// Above 0.8 a cave would take the global fluid, which here is none: keep the wettest spots just below it.
		Aquifer.Config aquifers = new Aquifer.Config(
				DensityFunctions.noise(noises.getOrThrow(Noises.AQUIFER_BARRIER), 1.0, 0.5),
				DensityFunctions.clamp(flooded, -1.0F, 0.78F),
				DensityFunctions.noise(noises.getOrThrow(Noises.AQUIFER_FLUID_LEVEL_SPREAD), 1.0, 0.7142857142857143),
				DensityFunctions.zero(),
				DensityFunctions.zero(),
				// The whole dimension counts as deep underground.
				DensityFunctions.constant(1000.0F));

		context.register(UnderworldDimension.NOISE_SETTINGS, new NoiseGeneratorSettings(
				NoiseSettings.create(UnderworldLogic.MIN_Y, UnderworldLogic.HEIGHT),
				Blocks.STONE.defaultBlockState(),
				Blocks.WATER.defaultBlockState(),
				router,
				context.lookup(Registries.MATERIAL_RULE).getOrThrow(MATERIAL_RULE),
				List.of(),
				UnderworldLogic.MIN_Y,
				false,
				Optional.of(aquifers),
				false,
				NoiseGeneratorSettings.DebugFunctions.EMPTY));
	}

	// ---- Material rules ----

	static void bootstrapMaterialRules(BootstrapContext<MaterialRule> context) {
		HolderGetter<MaterialRule> rules = context.lookup(Registries.MATERIAL_RULE);
		HolderGetter<MaterialCondition> conditions = context.lookup(Registries.MATERIAL_CONDITION);
		HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
		MaterialCondition onFloor = MaterialRules.getCondition(conditions, VanillaMaterialConditions.ON_FLOOR);
		MaterialCondition underFloor = MaterialRules.getCondition(conditions, VanillaMaterialConditions.UNDER_FLOOR);
		MaterialCondition onCeiling = MaterialRules.getCondition(conditions, VanillaMaterialConditions.ON_CEILING);
		MaterialCondition dry = MaterialRules.getCondition(conditions, VanillaMaterialConditions.NOT_UNDERWATER);

		MaterialRule deepBedrock = MaterialRules.ifTrue(
				MaterialRules.verticalGradient("eam:deep_bedrock_top", VerticalAnchor.absolute(UnderworldLogic.BAND_CORE_TOP),
						VerticalAnchor.absolute(UnderworldLogic.BAND_TOP + 1)),
				MaterialRules.ifTrue(MaterialRules.not(MaterialRules.verticalGradient("eam:deep_bedrock_bottom",
						VerticalAnchor.absolute(UnderworldLogic.BAND_BOTTOM - 1), VerticalAnchor.absolute(UnderworldLogic.BAND_CORE_BOTTOM))),
						state(EamBlocks.DEEP_BEDROCK)));
		MaterialCondition lowerLevel = MaterialRules.not(MaterialRules.yBlockCheck(VerticalAnchor.absolute(UnderworldLogic.LEVEL_SPLIT), 0));
		MaterialRule deepslate = MaterialRules.ifTrue(MaterialRules.verticalGradient("eam:underworld_deepslate",
				VerticalAnchor.absolute(UnderworldLogic.BAND_BOTTOM - 6), VerticalAnchor.absolute(UnderworldLogic.BAND_BOTTOM - 1)),
				state(Blocks.DEEPSLATE));

		MaterialRule depths = MaterialRules.ifTrue(lowerLevel, MaterialRules.sequence(
				MaterialRules.ifTrue(onFloor, MaterialRules.sequence(
						MaterialRules.ifTrue(MaterialRules.noiseCondition3d(Noises.NETHER_STATE_SELECTOR, 0.35), state(Blocks.MAGMA_BLOCK)),
						MaterialRules.ifTrue(MaterialRules.noiseCondition3d(Noises.PATCH, 0.1), state(Blocks.BASALT)),
						MaterialRules.ifTrue(MaterialRules.noiseCondition3d(Noises.PATCH, -1.0, -0.25), state(Blocks.BLACKSTONE)))),
				MaterialRules.ifTrue(onCeiling, MaterialRules.ifTrue(MaterialRules.noiseCondition3d(Noises.PATCH, 0.2), state(Blocks.BASALT))),
				deepslate));

		MaterialRule mushroom = MaterialRules.ifTrue(MaterialRules.isBiome(biomes, UnderworldDimension.MUSHROOM_FOREST), MaterialRules.sequence(
				MaterialRules.ifTrue(onFloor, MaterialRules.ifTrue(dry, state(Blocks.MYCELIUM))),
				MaterialRules.ifTrue(underFloor, state(Blocks.DIRT))));
		MaterialRule lava = MaterialRules.ifTrue(MaterialRules.isBiome(biomes, UnderworldDimension.LAVA_CAVES), MaterialRules.sequence(
				MaterialRules.ifTrue(underFloor, MaterialRules.sequence(
						MaterialRules.ifTrue(MaterialRules.noiseCondition3d(Noises.PATCH, 0.15), state(Blocks.BASALT)),
						MaterialRules.ifTrue(MaterialRules.noiseCondition3d(Noises.PATCH, -1.0, -0.2), state(Blocks.BLACKSTONE)))),
				MaterialRules.ifTrue(onCeiling, MaterialRules.ifTrue(MaterialRules.noiseCondition3d(Noises.NETHER_STATE_SELECTOR, 0.0),
						state(Blocks.BASALT)))));
		MaterialRule ice = MaterialRules.ifTrue(MaterialRules.isBiome(biomes, UnderworldDimension.ICE_CAVES), MaterialRules.sequence(
				// Powder snow pits, read from a 2D noise so each is a straight shaft: three blocks deep at the rim, five in the middle.
				MaterialRules.ifTrue(dry, MaterialRules.ifTrue(MaterialRules.stoneDepthCheck(4, false, CaveSurface.FLOOR),
						MaterialRules.ifTrue(MaterialRules.noiseCondition2d(Noises.POWDER_SNOW, 0.48, 0.55), state(Blocks.POWDER_SNOW)))),
				MaterialRules.ifTrue(dry, MaterialRules.ifTrue(MaterialRules.stoneDepthCheck(2, false, CaveSurface.FLOOR),
						MaterialRules.ifTrue(MaterialRules.noiseCondition2d(Noises.POWDER_SNOW, 0.45, 0.58), state(Blocks.POWDER_SNOW)))),
				MaterialRules.ifTrue(onFloor, MaterialRules.ifTrue(dry, MaterialRules.sequence(
						MaterialRules.ifTrue(MaterialRules.noiseCondition3d(Noises.PACKED_ICE, 0.0, 0.25), state(Blocks.PACKED_ICE)),
						state(Blocks.SNOW_BLOCK)))),
				MaterialRules.ifTrue(underFloor, MaterialRules.ifTrue(MaterialRules.noiseCondition3d(Noises.ICE, -0.2, 0.2), state(Blocks.PACKED_ICE))),
				MaterialRules.ifTrue(onCeiling, MaterialRules.ifTrue(MaterialRules.noiseCondition3d(Noises.PACKED_ICE, -0.3, 0.3),
						state(Blocks.PACKED_ICE)))));
		// Moss on the floors of the lake caves; the lake beds are clay and stone, laid by the lake feature.
		MaterialRule lakes = MaterialRules.ifTrue(MaterialRules.isBiome(biomes, UnderworldDimension.LAKE_CAVES),
				MaterialRules.ifTrue(onFloor, MaterialRules.ifTrue(MaterialRules.noiseCondition3d(Noises.PATCH, 0.05), state(Blocks.MOSS_BLOCK))));
		// Frozen caves: floors of packed ice and snow with stone between, ice patches on the ceilings.
		MaterialRule frozen = MaterialRules.ifTrue(MaterialRules.isBiome(biomes, UnderworldDimension.FROZEN_CAVES), MaterialRules.sequence(
				MaterialRules.ifTrue(onFloor, MaterialRules.ifTrue(dry, MaterialRules.sequence(
						MaterialRules.ifTrue(MaterialRules.noiseCondition3d(Noises.PACKED_ICE, -0.1, 0.3), state(Blocks.PACKED_ICE)),
						MaterialRules.ifTrue(MaterialRules.noiseCondition3d(Noises.POWDER_SNOW, -0.25, 0.25), state(Blocks.SNOW_BLOCK)),
						MaterialRules.ifTrue(MaterialRules.noiseCondition3d(Noises.ICE, 0.55), state(Blocks.BLUE_ICE))))),
				MaterialRules.ifTrue(onCeiling, MaterialRules.ifTrue(MaterialRules.noiseCondition3d(Noises.PACKED_ICE, -0.2, 0.2),
						state(Blocks.PACKED_ICE)))));
		// River caves: moss like the lake caves, gravel and clay where it is wetter.
		MaterialRule rivers = MaterialRules.ifTrue(MaterialRules.isBiome(biomes, UnderworldDimension.RIVER_CAVES),
				MaterialRules.ifTrue(onFloor, MaterialRules.sequence(
						MaterialRules.ifTrue(MaterialRules.noiseCondition3d(Noises.PATCH, 0.0), state(Blocks.MOSS_BLOCK)),
						MaterialRules.ifTrue(MaterialRules.noiseCondition3d(Noises.GRAVEL, 0.35), state(Blocks.GRAVEL)),
						MaterialRules.ifTrue(MaterialRules.noiseCondition3d(Noises.CLAY_BANDS_OFFSET, 0.4), state(Blocks.CLAY)))));
		// Underground forest: grass, moss and podzol over dirt, moss hanging on the ceilings.
		MaterialRule forest = MaterialRules.ifTrue(MaterialRules.isBiome(biomes, UnderworldDimension.UNDERGROUND_FOREST), MaterialRules.sequence(
				MaterialRules.ifTrue(onFloor, MaterialRules.ifTrue(dry, MaterialRules.sequence(
						MaterialRules.ifTrue(MaterialRules.noiseCondition3d(Noises.PATCH, 0.1), state(Blocks.MOSS_BLOCK)),
						MaterialRules.ifTrue(MaterialRules.noiseCondition3d(Noises.PATCH, -1.0, -0.3), state(Blocks.PODZOL)),
						state(Blocks.GRASS_BLOCK)))),
				MaterialRules.ifTrue(underFloor, state(Blocks.DIRT)),
				MaterialRules.ifTrue(onCeiling, MaterialRules.ifTrue(MaterialRules.noiseCondition3d(Noises.PATCH, 0.15), state(Blocks.MOSS_BLOCK)))));
		// Burnt mushroom forest: scorched ground, coarse dirt with basalt and blackstone, embers of magma.
		MaterialRule burnt = MaterialRules.ifTrue(MaterialRules.isBiome(biomes, UnderworldDimension.BURNT_MUSHROOM_FOREST), MaterialRules.sequence(
				MaterialRules.ifTrue(onFloor, MaterialRules.ifTrue(dry, MaterialRules.sequence(
						MaterialRules.ifTrue(MaterialRules.noiseCondition3d(Noises.NETHER_STATE_SELECTOR, 0.5), state(Blocks.MAGMA_BLOCK)),
						MaterialRules.ifTrue(MaterialRules.noiseCondition3d(Noises.PATCH, 0.3), state(Blocks.BASALT)),
						MaterialRules.ifTrue(MaterialRules.noiseCondition3d(Noises.PATCH, -1.0, -0.35), state(Blocks.BLACKSTONE)),
						state(Blocks.COARSE_DIRT)))),
				MaterialRules.ifTrue(underFloor, state(Blocks.COARSE_DIRT)),
				MaterialRules.ifTrue(onCeiling, MaterialRules.ifTrue(MaterialRules.noiseCondition3d(Noises.NETHER_STATE_SELECTOR, 0.1),
						state(Blocks.BASALT)))));
		MaterialRule wastes = MaterialRules.ifTrue(MaterialRules.isBiome(biomes, UnderworldDimension.STONE_WASTES),
				MaterialRules.ifTrue(onFloor, MaterialRules.sequence(
						MaterialRules.ifTrue(MaterialRules.noiseCondition3d(Noises.GRAVEL, 0.3), state(Blocks.GRAVEL)),
						MaterialRules.ifTrue(MaterialRules.noiseCondition3d(Noises.PATCH, 0.5), state(Blocks.TUFF)))));

		context.register(MATERIAL_RULE, MaterialRules.sequence(
				MaterialRules.getRule(rules, VanillaMaterialRules.BEDROCK_FLOOR),
				MaterialRules.getRule(rules, VanillaMaterialRules.BEDROCK_ROOF),
				deepBedrock,
				depths,
				mushroom,
				lava,
				ice,
				lakes,
				frozen,
				rivers,
				forest,
				burnt,
				wastes));
	}

	private static MaterialRule state(Block block) {
		return MaterialRules.state(block.defaultBlockState());
	}

	// ---- Dimension type ----

	static void bootstrapDimensionTypes(BootstrapContext<DimensionType> context) {
		HolderGetter<Block> blocks = context.lookup(Registries.BLOCK);
		BedRule noSleep = new BedRule(BedRule.Rule.NEVER, BedRule.Rule.ALWAYS, false, false,
				Optional.of(net.minecraft.network.chat.Component.translatable("block.eam.bed.underworld")));
		context.register(UnderworldDimension.TYPE, new DimensionType(
				true,
				false,
				true,
				false,
				8.0,
				UnderworldLogic.MIN_Y,
				UnderworldLogic.HEIGHT,
				UnderworldLogic.HEIGHT,
				blocks.getOrThrow(BlockTags.INFINIBURN_OVERWORLD),
				0.05F,
				new DimensionType.MonsterSettings(UniformInt.of(0, 7), 0),
				DimensionType.Skybox.NONE,
				CardinalLighting.Type.DEFAULT,
				EnvironmentAttributeMap.builder()
						.set(EnvironmentAttributes.FOG_START_DISTANCE, 8.0F)
						.set(EnvironmentAttributes.FOG_END_DISTANCE, 88.0F)
						.set(EnvironmentAttributes.FOG_COLOR, ARGB.vector3fFromRGB24(0x2B2B2B))
						.set(EnvironmentAttributes.SKY_LIGHT_LEVEL, 0.0F)
						.set(EnvironmentAttributes.SKY_LIGHT_FACTOR, 0.0F)
						// Dark: a third of what it was, and the brightness setting capped like the overworld's (see DarknessLogic).
						.set(EnvironmentAttributes.AMBIENT_LIGHT_COLOR, ARGB.vector3fFromRGB24(0x08080A))
						.set(DarknessModule.MAX_BRIGHTNESS, DarknessLogic.MAX_BRIGHTNESS)
						.set(EnvironmentAttributes.AMBIENT_SOUNDS, AmbientSounds.LEGACY_CAVE_SETTINGS)
						.set(EnvironmentAttributes.BED_RULE, noSleep)
						.set(EnvironmentAttributes.STRAW_BED_RULE, noSleep)
						.set(EnvironmentAttributes.CAN_START_RAID, false)
						.build(),
				HolderSet.empty(),
				Optional.empty()));
	}

	// ---- Features ----

	static void bootstrapFeatures(BootstrapContext<Feature> context) {
		context.register(FLUIDS, new UnderworldFluidsFeature(LAVA_LAKES, FROZEN_LAKES, 0.35F, 0.2F, 0.08F));
		// Mushrooms a little bigger than vanilla's huge ones and shaped like them, plus chanterelles and champignons.
		BlockState stem = Blocks.MUSHROOM_STEM.defaultBlockState();
		BlockState shroomlight = Blocks.SHROOMLIGHT.defaultBlockState();
		context.register(GIANT_BROWN_MUSHROOM, new GiantMushroomFeature(Blocks.BROWN_MUSHROOM_BLOCK.defaultBlockState(), stem,
				shroomlight, Shape.BROWN, UniformInt.of(5, 10), UniformInt.of(3, 5), 9, 0.03F));
		context.register(GIANT_RED_MUSHROOM, new GiantMushroomFeature(Blocks.RED_MUSHROOM_BLOCK.defaultBlockState(), stem,
				shroomlight, Shape.RED, UniformInt.of(5, 9), UniformInt.of(2, 3), 64, 0.03F));
		context.register(GIANT_CHANTERELLE, new GiantMushroomFeature(EamBlocks.CHANTERELLE_CAP.defaultBlockState(),
				EamBlocks.CHANTERELLE_STEM.defaultBlockState(), shroomlight, Shape.CHANTERELLE, UniformInt.of(3, 7), UniformInt.of(2, 5), 6, 0.0F));
		context.register(GIANT_CHAMPIGNON, new GiantMushroomFeature(EamBlocks.CHAMPIGNON_CAP.defaultBlockState(), stem,
				EamBlocks.GLOWING_CHAMPIGNON_CAP.defaultBlockState(), Shape.CHAMPIGNON, UniformInt.of(4, 8), UniformInt.of(3, 6), 7, 0.22F));
		context.register(ICE_SPIKE, new IceSpikeCaveFeature(UniformInt.of(5, 26), UniformInt.of(1, 4), 0.0F, 48));
		context.register(ICE_SPIRE, new IceSpireFeature(UniformInt.of(5, 22), UniformInt.of(1, 3), UniformInt.of(30, 110), UniformInt.of(3, 6),
				0.12F));
		context.register(LAKE, new UnderworldLakeFeature(UniformInt.of(3, 6), UniformInt.of(2, 4), 0.3F, 0.6F, 2, UPPER_BOTTOM, UPPER_TOP - 8,
				LAVA_LAKES));
		// Big lava lakes of the lava caves, now and then fed by a lavafall from another one on a ledge above.
		context.register(LAVA_LAKE, new UnderworldLakeFeature(UniformInt.of(7, 10), UniformInt.of(3, 5), 0.25F, 0.3F, 1, UPPER_BOTTOM,
				UPPER_TOP - 8, LAVA_LAKES));
		context.register(LAVA_PILLAR, new CavePillarFeature(Blocks.BASALT.defaultBlockState(), Blocks.BLACKSTONE.defaultBlockState(),
				UniformInt.of(1, 3), 0.2F, 96));
		context.register(COBWEB, new SimpleBlockFeature(BlockStateProvider.of(Blocks.COBWEB)));
		// About thirty draugr frozen into the floor, two to four of them waiting.
		context.register(DRAUGR_PACK, new DraugrPackFeature(UniformInt.of(24, 34), UniformInt.of(2, 4), 20, 0.25F));
		// Streams in the underground forest, wider and deeper rivers in the river caves.
		context.register(STREAM, new CaveStreamFeature(UniformInt.of(0, 1), ConstantInt.of(1), UniformInt.of(12, 28), Blocks.CLAY.defaultBlockState()));
		context.register(RIVER, new CaveStreamFeature(UniformInt.of(1, 2), UniformInt.of(1, 2), UniformInt.of(20, 40), Blocks.CLAY.defaultBlockState()));
		// The frozen caves have now and then a small ice spike and a short icicle.
		context.register(SMALL_ICE_SPIRE, new IceSpireFeature(UniformInt.of(3, 8), UniformInt.of(1, 2), UniformInt.of(9, 14), UniformInt.of(2, 3),
				0.08F));
		context.register(SMALL_ICE_SPIKE, new IceSpikeCaveFeature(UniformInt.of(2, 7), UniformInt.of(1, 2), 0.3F, 24));
		// Spruces low enough for the forest's caves; vanilla's taller ones stand where a cave is high enough.
		HolderGetter<BlockStateProvider> providers = context.lookup(Registries.BLOCK_STATE_PROVIDER);
		context.register(SMALL_SPRUCE, new TreeFeature.Builder(BlockStateProvider.of(Blocks.SPRUCE_LOG), new StraightTrunkPlacer(3, 2, 0),
				BlockStateProvider.of(Blocks.SPRUCE_LEAVES), new SpruceFoliagePlacer(UniformInt.of(1, 2), UniformInt.of(0, 1), UniformInt.of(1, 2)),
				new TwoLayersFeatureSize(2, 0, 2), providers.getOrThrow(BlockStateProviders.SOIL_BENEATH_TREE)).ignoreVines().build());
		context.register(FOREST_PLANTS, new SimpleBlockFeature(new WeightedStateProvider(WeightedList.<BlockState>builder()
				.add(Blocks.FERN.defaultBlockState(), 8)
				.add(Blocks.SHORT_GRASS.defaultBlockState(), 5)
				.add(Blocks.LARGE_FERN.defaultBlockState(), 2)
				.add(Blocks.MOSS_CARPET.defaultBlockState(), 4)
				.add(Blocks.SWEET_BERRY_BUSH.defaultBlockState(), 1)
				.add(Blocks.BROWN_MUSHROOM.defaultBlockState(), 1))));
		// Glowing fungus threads from the ceilings: mostly short, some long enough to reach far down. The tip starts old, as
		// vanilla's vines do, so they barely grow on after worldgen.
		context.register(GLOWSHROOM_VINES, new BlockColumnFeature(List.of(
				BlockColumnFeature.layer(new WeightedListInt(WeightedList.<IntProvider>builder()
						.add(UniformInt.of(0, 3), 6).add(UniformInt.of(2, 7), 3).add(UniformInt.of(5, 12), 1).build()),
						BlockStateProvider.of(EamBlocks.GLOWSHROOM_VINES_PLANT)),
				BlockColumnFeature.layer(ConstantInt.of(1), new RandomizedIntStateProvider(BlockStateProvider.of(EamBlocks.GLOWSHROOM_VINES),
						GrowingPlantHeadBlock.AGE, UniformInt.of(17, 25)))),
				Direction.DOWN, BlockPredicate.ONLY_IN_AIR_PREDICATE, true));
		// Plants on the banks of the lake and river caves.
		context.register(SHORE_PLANTS, new SimpleBlockFeature(new WeightedStateProvider(WeightedList.<BlockState>builder()
				.add(Blocks.BLUE_ORCHID.defaultBlockState(), 5)
				.add(Blocks.FERN.defaultBlockState(), 4)
				.add(Blocks.SHORT_GRASS.defaultBlockState(), 4)
				.add(Blocks.TALL_GRASS.defaultBlockState(), 2)
				.add(Blocks.AZALEA.defaultBlockState(), 2)
				.add(Blocks.FLOWERING_AZALEA.defaultBlockState(), 1)
				.add(Blocks.MOSS_CARPET.defaultBlockState(), 3))));
		// Giant mushrooms burnt black: some hang a smouldering cap block under the cap, the champignon-like ones smoulder in
		// patches of the cap itself.
		BlockState burntCap = EamBlocks.BURNT_MUSHROOM_CAP.defaultBlockState();
		BlockState burntStem = EamBlocks.BURNT_MUSHROOM_STEM.defaultBlockState();
		BlockState smoldering = EamBlocks.SMOLDERING_MUSHROOM_CAP.defaultBlockState();
		context.register(BURNT_BROWN_MUSHROOM, new GiantMushroomFeature(burntCap, burntStem, smoldering, Shape.BROWN, UniformInt.of(4, 9),
				UniformInt.of(3, 5), 9, 0.05F));
		context.register(BURNT_RED_MUSHROOM, new GiantMushroomFeature(burntCap, burntStem, smoldering, Shape.RED, UniformInt.of(4, 8),
				UniformInt.of(2, 3), 64, 0.05F));
		context.register(SMOLDERING_MUSHROOM, new GiantMushroomFeature(burntCap, burntStem, smoldering, Shape.CHAMPIGNON, UniformInt.of(4, 7),
				UniformInt.of(3, 5), 7, 0.18F));

		// Fallen trunks: vanilla's fallen trees (a stump, a gap, the trunk lying along the floor) in cave woods and stones.
		Holder<BlockStateProvider> smallMushrooms = Holder.direct(weighted(Blocks.BROWN_MUSHROOM, 2, Blocks.RED_MUSHROOM, 1));
		Holder<BlockStateProvider> mossCarpet = BlockStateProvider.holderOf(Blocks.MOSS_CARPET);
		Holder<BlockStateProvider> snow = BlockStateProvider.holderOf(Blocks.SNOW);
		context.register(FALLEN_SPRUCE, FallenTreeFeature.builder(BlockStateProvider.of(Blocks.SPRUCE_LOG), UniformInt.of(5, 8))
				.logDecorator(new ShelfMushroomDecorator(0.8F))
				.logDecorator(new AttachedToLogsDecorator(0.15F, smallMushrooms, List.of(Direction.UP)))
				.logDecorator(new AttachedToLogsDecorator(0.3F, mossCarpet, List.of(Direction.UP)))
				.stumpDecorator(new AttachedToLogsDecorator(0.5F, mossCarpet, List.of(Direction.UP)))
				.build());
		context.register(FALLEN_POPLAR, FallenTreeFeature.builder(BlockStateProvider.of(Blocks.POPLAR_LOG), UniformInt.of(4, 7))
				.logDecorator(new ShelfMushroomDecorator(0.6F))
				.logDecorator(new AttachedToLogsDecorator(0.35F, mossCarpet, List.of(Direction.UP)))
				.build());
		context.register(FALLEN_FROZEN_SPRUCE, FallenTreeFeature.builder(weighted(Blocks.SPRUCE_LOG, 2, Blocks.STRIPPED_SPRUCE_LOG, 1), UniformInt.of(4, 7))
				.logDecorator(new AttachedToLogsDecorator(0.6F, snow, List.of(Direction.UP)))
				.stumpDecorator(new AttachedToLogsDecorator(1.0F, snow, List.of(Direction.UP)))
				.build());
		// A giant mushroom's stem that fell over, little mushrooms and shelf fungus growing on it.
		context.register(FALLEN_MUSHROOM_STEM, FallenTreeFeature.builder(BlockStateProvider.of(Blocks.MUSHROOM_STEM), UniformInt.of(5, 8))
				.logDecorator(new ShelfMushroomDecorator(0.7F))
				.logDecorator(new AttachedToLogsDecorator(0.3F, smallMushrooms, List.of(Direction.UP)))
				.build());
		// A charred stem of the burnt forest, here and there still smouldering.
		context.register(FALLEN_BURNT_STEM, FallenTreeFeature.builder(weighted(EamBlocks.BURNT_MUSHROOM_STEM, 8, EamBlocks.SMOLDERING_MUSHROOM_CAP, 1),
				UniformInt.of(4, 7)).build());
		// Bones of something big that died in the wastes long ago: a rib standing up and the spine lying beside it.
		context.register(OLD_BONES, FallenTreeFeature.builder(BlockStateProvider.of(Blocks.BONE_BLOCK), UniformInt.of(4, 6)).build());

		// Boulders, each block picked on its own, so the stones mix.
		context.register(MOSSY_BOULDER, new CaveBoulderFeature(Holder.direct(weighted(Blocks.MOSSY_COBBLESTONE, 3, Blocks.COBBLESTONE, 2)),
				UniformInt.of(2, 3), UniformInt.of(1, 2), Optional.of(mossCarpet), 0.25F));
		context.register(ROCK_PILE, new CaveBoulderFeature(Holder.direct(new WeightedStateProvider(WeightedList.<BlockState>builder()
				.add(Blocks.COBBLESTONE.defaultBlockState(), 4)
				.add(Blocks.STONE.defaultBlockState(), 3)
				.add(Blocks.ANDESITE.defaultBlockState(), 2)
				.add(Blocks.TUFF.defaultBlockState(), 2)
				.add(Blocks.GRAVEL.defaultBlockState(), 1))), UniformInt.of(1, 3), ConstantInt.of(1), Optional.empty(), 0.0F));
		context.register(ICE_BOULDER, new CaveBoulderFeature(Holder.direct(weighted(Blocks.PACKED_ICE, 4, Blocks.SNOW_BLOCK, 2, Blocks.BLUE_ICE, 1)),
				UniformInt.of(2, 3), UniformInt.of(1, 2), Optional.of(snow), 0.6F));
		context.register(BIG_ICE_BOULDER, new CaveBoulderFeature(Holder.direct(weighted(Blocks.PACKED_ICE, 5, Blocks.BLUE_ICE, 2, Blocks.SNOW_BLOCK, 1)),
				UniformInt.of(3, 4), UniformInt.of(2, 3), Optional.of(snow), 0.6F));
		context.register(SCORCHED_BOULDER, new CaveBoulderFeature(Holder.direct(weighted(Blocks.BLACKSTONE, 5, Blocks.COBBLESTONE, 3, Blocks.COAL_ORE, 1)),
				UniformInt.of(2, 3), UniformInt.of(1, 2), Optional.empty(), 0.0F));
		context.register(BASALT_BOULDER, new CaveBoulderFeature(Holder.direct(weighted(Blocks.BASALT, 4, Blocks.BLACKSTONE, 3, Blocks.MAGMA_BLOCK, 1)),
				UniformInt.of(2, 3), UniformInt.of(1, 2), Optional.empty(), 0.0F));
	}

	static void bootstrapPlacedFeatures(BootstrapContext<PlacedFeature> context) {
		HolderGetter<Feature> features = context.lookup(Registries.FEATURE);
		PlacementModifier upper = HeightRangePlacement.uniform(VerticalAnchor.absolute(UPPER_BOTTOM), VerticalAnchor.absolute(UPPER_TOP));
		PlacementModifier lower = HeightRangePlacement.uniform(VerticalAnchor.absolute(5), VerticalAnchor.absolute(UnderworldLogic.BAND_BOTTOM));
		PlacementModifier all = HeightRangePlacement.uniform(VerticalAnchor.absolute(5), VerticalAnchor.absolute(UPPER_TOP));

		// Lakes: the feature finds the lowest floor of its column itself; the fixed height only picks the upper level's biome.
		PlacementModifier upperBiome = HeightRangePlacement.uniform(VerticalAnchor.absolute(UPPER_BOTTOM + 40), VerticalAnchor.absolute(UPPER_BOTTOM + 40));
		PlacementUtils.register(context, P_LAKES_FEW, features.getOrThrow(LAKE), List.of(RarityFilter.onAverageOnceEvery(3),
				InSquarePlacement.spread(), upperBiome, BiomeFilter.biome()));
		PlacementUtils.register(context, P_LAKES, features.getOrThrow(LAKE), List.of(CountPlacement.of(UniformInt.of(1, 2)),
				InSquarePlacement.spread(), upperBiome, BiomeFilter.biome()));
		PlacementUtils.register(context, P_LAKES_MANY, features.getOrThrow(LAKE), List.of(CountPlacement.of(UniformInt.of(3, 5)),
				InSquarePlacement.spread(), upperBiome, BiomeFilter.biome()));
		PlacementUtils.register(context, P_LAVA_LAKES, features.getOrThrow(LAVA_LAKE), List.of(RarityFilter.onAverageOnceEvery(2),
				InSquarePlacement.spread(), upperBiome, BiomeFilter.biome()));
		onFloor(context, P_STREAMS, features.getOrThrow(STREAM), CountPlacement.of(UniformInt.of(1, 3)));
		onFloor(context, P_RIVERS, features.getOrThrow(RIVER), CountPlacement.of(UniformInt.of(2, 4)));
		PlacementUtils.register(context, P_FLUIDS, features.getOrThrow(FLUIDS), List.of());
		// Ores: the underworld is where the rare ores are. Diamond, silver and mithril are richer than in the overworld,
		// the common ores (iron, gold, copper, redstone, lapis) scarcer. Copper, silver, redstone and lapis only in the
		// upper level; mithril is most common under the deep bedrock, and adamantium is found only there. The hot biomes (lava caves, mushroom forest) add
		// gold and redstone; the ice caves add diamond, silver, mithril and lapis.
		ore(context, P_ORE_IRON, features.getOrThrow(OreFeatures.ORE_IRON), 9, all);
		ore(context, P_ORE_GOLD, features.getOrThrow(OreFeatures.ORE_GOLD), 3, all);
		ore(context, P_ORE_COPPER, features.getOrThrow(EamWorldgen.ORE_COPPER), 6, upper);
		ore(context, P_ORE_REDSTONE, features.getOrThrow(OreFeatures.ORE_REDSTONE), 2, upper);
		ore(context, P_ORE_LAPIS, features.getOrThrow(OreFeatures.ORE_LAPIS), 1, upper);
		ore(context, P_ORE_DIAMOND, features.getOrThrow(OreFeatures.ORE_DIAMOND_SMALL), 4, all);
		ore(context, P_ORE_SILVER, features.getOrThrow(EamWorldgen.ORE_SILVER), 9, upper);
		ore(context, P_ORE_MITHRIL, features.getOrThrow(EamWorldgen.ORE_MITHRIL), 2, upper);
		ore(context, P_ORE_MITHRIL_DEPTHS, features.getOrThrow(EamWorldgen.ORE_MITHRIL), 6, lower);
		// Adamantium only in the depths, one small vein per chunk on average (3 tries, each kept 1 time in 3).
		PlacementUtils.register(context, P_ORE_ADAMANTIUM, features.getOrThrow(EamWorldgen.ORE_ADAMANTIUM), List.of(CountPlacement.of(3),
				RarityFilter.onAverageOnceEvery(3), InSquarePlacement.spread(), lower, BiomeFilter.biome()));
		ore(context, P_ORE_GOLD_HOT, features.getOrThrow(OreFeatures.ORE_GOLD), 7, upper);
		ore(context, P_ORE_REDSTONE_HOT, features.getOrThrow(OreFeatures.ORE_REDSTONE), 6, upper);
		ore(context, P_ORE_DIAMOND_ICE, features.getOrThrow(OreFeatures.ORE_DIAMOND_SMALL), 4, upper);
		ore(context, P_ORE_SILVER_ICE, features.getOrThrow(EamWorldgen.ORE_SILVER), 8, upper);
		ore(context, P_ORE_MITHRIL_ICE, features.getOrThrow(EamWorldgen.ORE_MITHRIL), 2, upper);
		ore(context, P_ORE_LAPIS_ICE, features.getOrThrow(OreFeatures.ORE_LAPIS), 4, upper);
		ore(context, P_ORE_GRAVEL, features.getOrThrow(OreFeatures.ORE_GRAVEL), 10, upper);
		ore(context, P_ORE_MAGMA, features.getOrThrow(OreFeatures.ORE_MAGMA), 6, all);

		// Springs make the waterfalls and lavafalls; most of them never find a wall with one opening and do nothing.
		ore(context, P_SPRING_WATER, features.getOrThrow(MiscOverworldFeatures.SPRING_WATER), 12, upper);
		ore(context, P_SPRING_WATER_MANY, features.getOrThrow(MiscOverworldFeatures.SPRING_WATER), 20, upper);
		ore(context, P_SPRING_LAVA, features.getOrThrow(MiscOverworldFeatures.SPRING_LAVA_OVERWORLD), 6, all);
		ore(context, P_SPRING_LAVA_MANY, features.getOrThrow(MiscOverworldFeatures.SPRING_LAVA_OVERWORLD), 40, all);

		// Pillars stand anywhere in the open and reach from floor (or lava bed) to ceiling. In the upper caves they rise
		// before the mines are dug, so a mine tunnel or its adit cuts through a pillar instead of being walled up by one.
		PlacementUtils.register(context, P_LAVA_PILLAR, features.getOrThrow(LAVA_PILLAR),
				List.of(RarityFilter.onAverageOnceEvery(2), InSquarePlacement.spread(), upper, BiomeFilter.biome()));
		PlacementUtils.register(context, P_LAVA_PILLAR_DEPTHS, features.getOrThrow(LAVA_PILLAR),
				List.of(CountPlacement.of(UniformInt.of(1, 2)), InSquarePlacement.spread(), lower, BiomeFilter.biome()));
		PlacementUtils.register(context, P_ICE_SPIRE, features.getOrThrow(ICE_SPIRE), List.of(CountPlacement.of(UniformInt.of(3, 7)),
				InSquarePlacement.spread(), HeightRangePlacement.uniform(VerticalAnchor.absolute(ICE_FLOOR - 4), VerticalAnchor.absolute(ICE_FLOOR + 28)),
				EnvironmentScanPlacement.scanningFor(Direction.DOWN, BlockPredicate.solid(), BlockPredicate.ONLY_IN_AIR_PREDICATE, 32),
				OffsetPlacement.vertical(ConstantInt.of(1)), BiomeFilter.biome()));
		// A pack every forty chunks or so of the ice caves, on the floor of the great hall.
		PlacementUtils.register(context, P_DRAUGR_PACK, features.getOrThrow(DRAUGR_PACK), List.of(RarityFilter.onAverageOnceEvery(40),
				InSquarePlacement.spread(), HeightRangePlacement.uniform(VerticalAnchor.absolute(ICE_FLOOR - 4), VerticalAnchor.absolute(ICE_FLOOR + 28)),
				EnvironmentScanPlacement.scanningFor(Direction.DOWN, BlockPredicate.solid(), BlockPredicate.ONLY_IN_AIR_PREDICATE, 32),
				OffsetPlacement.vertical(ConstantInt.of(1)), BiomeFilter.biome()));
		PlacementUtils.register(context, P_ICE_SPIKE, features.getOrThrow(ICE_SPIKE),
				List.of(CountPlacement.of(UniformInt.of(4, 9)), InSquarePlacement.spread(), upper, BiomeFilter.biome()));
		onFloor(context, P_GIANT_BROWN, features.getOrThrow(GIANT_BROWN_MUSHROOM), CountPlacement.of(UniformInt.of(2, 5)));
		onFloor(context, P_GIANT_RED, features.getOrThrow(GIANT_RED_MUSHROOM), CountPlacement.of(UniformInt.of(2, 4)));
		onFloor(context, P_GIANT_CHANTERELLE, features.getOrThrow(GIANT_CHANTERELLE), CountPlacement.of(UniformInt.of(1, 3)));
		onFloor(context, P_GIANT_CHAMPIGNON, features.getOrThrow(GIANT_CHAMPIGNON), CountPlacement.of(UniformInt.of(1, 3)));
		onFloor(context, P_HUGE_BROWN, features.getOrThrow(TreeFeatures.HUGE_BROWN_MUSHROOM), CountPlacement.of(3));
		onFloor(context, P_HUGE_RED, features.getOrThrow(TreeFeatures.HUGE_RED_MUSHROOM), CountPlacement.of(2));
		// Decorations: rare on purpose, a few per biome band, each only on the floor its biome is made of. The many-level
		// biomes (the forest, the frozen caves) get more tries for trunks: most land in rock or on a floor too uneven for one.
		decoration(context, P_FALLEN_SPRUCE, features.getOrThrow(FALLEN_SPRUCE), 2, 1, Blocks.GRASS_BLOCK, Blocks.PODZOL, Blocks.MOSS_BLOCK,
				Blocks.DIRT);
		decoration(context, P_MOSSY_BOULDER_FOREST, features.getOrThrow(MOSSY_BOULDER), 1, 6, Blocks.GRASS_BLOCK, Blocks.PODZOL,
				Blocks.MOSS_BLOCK, Blocks.STONE);
		decoration(context, P_FALLEN_FROZEN_SPRUCE, features.getOrThrow(FALLEN_FROZEN_SPRUCE), 1, 1, Blocks.SNOW_BLOCK, Blocks.PACKED_ICE,
				Blocks.STONE);
		decoration(context, P_ICE_BOULDER, features.getOrThrow(ICE_BOULDER), 1, 2, Blocks.SNOW_BLOCK, Blocks.PACKED_ICE,
				Blocks.BLUE_ICE, Blocks.STONE);
		decoration(context, P_FALLEN_POPLAR, features.getOrThrow(FALLEN_POPLAR), 1, 2, Blocks.MOSS_BLOCK, Blocks.CLAY,
				Blocks.GRAVEL, Blocks.STONE);
		decoration(context, P_MOSSY_BOULDER_LAKE, features.getOrThrow(MOSSY_BOULDER), 1, 4, Blocks.MOSS_BLOCK, Blocks.STONE,
				Blocks.GRAVEL);
		decoration(context, P_FALLEN_MUSHROOM_STEM, features.getOrThrow(FALLEN_MUSHROOM_STEM), 1, 5, Blocks.MYCELIUM,
				Blocks.DIRT, Blocks.STONE);
		decoration(context, P_MOSSY_BOULDER_MUSHROOM, features.getOrThrow(MOSSY_BOULDER), 1, 3, Blocks.MYCELIUM, Blocks.DIRT,
				Blocks.STONE);
		decoration(context, P_FALLEN_BURNT_STEM, features.getOrThrow(FALLEN_BURNT_STEM), 1, 4, Blocks.COARSE_DIRT,
				Blocks.BASALT, Blocks.BLACKSTONE);
		decoration(context, P_SCORCHED_BOULDER, features.getOrThrow(SCORCHED_BOULDER), 1, 5, Blocks.COARSE_DIRT,
				Blocks.BASALT, Blocks.BLACKSTONE);
		decoration(context, P_BASALT_BOULDER, features.getOrThrow(BASALT_BOULDER), 1, 4, Blocks.BASALT, Blocks.BLACKSTONE,
				Blocks.STONE);
		decoration(context, P_ROCK_PILE, features.getOrThrow(ROCK_PILE), 1, 4, Blocks.STONE, Blocks.GRAVEL, Blocks.TUFF);
		decoration(context, P_OLD_BONES, features.getOrThrow(OLD_BONES), 1, 3, Blocks.STONE, Blocks.GRAVEL, Blocks.TUFF);
		// The ice caves' hall is far taller than the scan: look for its floor from just above it, like the spires do.
		PlacementUtils.register(context, P_BIG_ICE_BOULDER, features.getOrThrow(BIG_ICE_BOULDER), List.of(RarityFilter.onAverageOnceEvery(3),
				InSquarePlacement.spread(), HeightRangePlacement.uniform(VerticalAnchor.absolute(ICE_FLOOR - 4), VerticalAnchor.absolute(ICE_FLOOR + 28)),
				EnvironmentScanPlacement.scanningFor(Direction.DOWN, BlockPredicate.solid(), BlockPredicate.ONLY_IN_AIR_PREDICATE, 32),
				OffsetPlacement.vertical(ConstantInt.of(1)), floorOf(Blocks.SNOW_BLOCK, Blocks.PACKED_ICE), BiomeFilter.biome()));
		patch(context, P_BROWN_MUSHROOMS, features.getOrThrow(VegetationFeatures.BROWN_MUSHROOM), 8, Blocks.BROWN_MUSHROOM);
		patch(context, P_RED_MUSHROOMS, features.getOrThrow(VegetationFeatures.RED_MUSHROOM), 5, Blocks.RED_MUSHROOM);
		patch(context, P_BROWN_MUSHROOMS_FEW, features.getOrThrow(VegetationFeatures.BROWN_MUSHROOM), 2, Blocks.BROWN_MUSHROOM);
		PlacementUtils.register(context, P_CAVE_VINES, features.getOrThrow(CaveFeatures.CAVE_VINE), List.of(CountPlacement.of(60),
				InSquarePlacement.spread(), upper, EnvironmentScanPlacement.scanningFor(Direction.UP, BlockPredicate.hasSturdyFace(Direction.DOWN),
						BlockPredicate.ONLY_IN_AIR_PREDICATE, 16), OffsetPlacement.vertical(ConstantInt.of(-1)), BiomeFilter.biome()));
		PlacementUtils.register(context, P_COBWEB, features.getOrThrow(COBWEB), List.of(CountPlacement.of(10), InSquarePlacement.spread(),
				all, EnvironmentScanPlacement.scanningFor(Direction.UP, BlockPredicate.solid(), BlockPredicate.ONLY_IN_AIR_PREDICATE, 12),
				OffsetPlacement.vertical(ConstantInt.of(-1)), BiomeFilter.biome()));
		PlacementUtils.register(context, P_GLOW_LICHEN, features.getOrThrow(CaveFeatures.GLOW_LICHEN),
				List.of(CountPlacement.of(UniformInt.of(30, 50)), InSquarePlacement.spread(), all, BiomeFilter.biome()));
		PlacementUtils.register(context, P_GLOW_LICHEN_MANY, features.getOrThrow(CaveFeatures.GLOW_LICHEN),
				List.of(CountPlacement.of(UniformInt.of(90, 130)), InSquarePlacement.spread(), all, BiomeFilter.biome()));

		// Frozen caves: a small ice spike every other chunk or so, a few short icicles.
		PlacementUtils.register(context, P_SMALL_ICE_SPIRE, features.getOrThrow(SMALL_ICE_SPIRE), List.of(RarityFilter.onAverageOnceEvery(2),
				InSquarePlacement.spread(), upper, EnvironmentScanPlacement.scanningFor(Direction.DOWN, BlockPredicate.solid(),
						BlockPredicate.ONLY_IN_AIR_PREDICATE, 32), OffsetPlacement.vertical(ConstantInt.of(1)), BiomeFilter.biome()));
		PlacementUtils.register(context, P_SMALL_ICE_SPIKE, features.getOrThrow(SMALL_ICE_SPIKE),
				List.of(CountPlacement.of(UniformInt.of(1, 3)), InSquarePlacement.spread(), upper, BiomeFilter.biome()));

		// Burnt mushroom forest.
		onFloor(context, P_BURNT_BROWN, features.getOrThrow(BURNT_BROWN_MUSHROOM), CountPlacement.of(UniformInt.of(3, 6)));
		onFloor(context, P_BURNT_RED, features.getOrThrow(BURNT_RED_MUSHROOM), CountPlacement.of(UniformInt.of(2, 4)));
		onFloor(context, P_SMOLDERING, features.getOrThrow(SMOLDERING_MUSHROOM), CountPlacement.of(UniformInt.of(2, 3)));
		patch(context, P_DEAD_BUSHES, features.getOrThrow(VegetationFeatures.DEAD_BUSH), 3, Blocks.DEAD_BUSH);

		// Underground forest: spruces wherever a cave is high enough, ferns and grass, streams, and glowing vines.
		onFloor(context, P_SPRUCE, features.getOrThrow(TreeFeatures.SPRUCE), CountPlacement.of(UniformInt.of(4, 7)),
				PlacementUtils.filteredByBlockSurvival(Blocks.SPRUCE_SAPLING));
		onFloor(context, P_SMALL_SPRUCE, features.getOrThrow(SMALL_SPRUCE), CountPlacement.of(UniformInt.of(8, 12)),
				PlacementUtils.filteredByBlockSurvival(Blocks.SPRUCE_SAPLING));
		onFloor(context, P_FOREST_MOSS, features.getOrThrow(CaveFeatures.MOSS_PATCH), CountPlacement.of(UniformInt.of(2, 4)));
		patch(context, P_FOREST_PLANTS, features.getOrThrow(FOREST_PLANTS), 10, Blocks.FERN);
		PlacementUtils.register(context, P_GLOWSHROOM_VINES, features.getOrThrow(GLOWSHROOM_VINES), List.of(CountPlacement.of(90),
				InSquarePlacement.spread(), upper, EnvironmentScanPlacement.scanningFor(Direction.UP, BlockPredicate.hasSturdyFace(Direction.DOWN),
						BlockPredicate.ONLY_IN_AIR_PREDICATE, 16), OffsetPlacement.vertical(ConstantInt.of(-1)), BiomeFilter.biome()));

		// Lake and river caves: moss with azaleas, clay pools with dripleaves, orchids and ferns, cane and lily pads by
		// the water, kelp and seagrass in it, spore blossoms on the ceilings.
		onFloor(context, P_MOSS_PATCH, features.getOrThrow(CaveFeatures.MOSS_PATCH), CountPlacement.of(UniformInt.of(4, 8)));
		onFloor(context, P_DRIPLEAF_CLAY, features.getOrThrow(CaveFeatures.LUSH_CAVES_CLAY), CountPlacement.of(UniformInt.of(1, 3)));
		patch(context, P_ORCHIDS, features.getOrThrow(VegetationFeatures.FLOWER_SWAMP), 3, Blocks.BLUE_ORCHID);
		patch(context, P_FERNS, features.getOrThrow(VegetationFeatures.TAIGA_GRASS), 3, Blocks.FERN);
		patch(context, P_SHORE_PLANTS, features.getOrThrow(SHORE_PLANTS), 4, Blocks.FERN);
		patch(context, P_SUGAR_CANE, features.getOrThrow(VegetationFeatures.SUGAR_CANE), 8, Blocks.SUGAR_CANE);
		PlacementUtils.register(context, P_SPORE_BLOSSOM, features.getOrThrow(CaveFeatures.SPORE_BLOSSOM), List.of(CountPlacement.of(6),
				InSquarePlacement.spread(), upper, EnvironmentScanPlacement.scanningFor(Direction.UP, BlockPredicate.solid(),
						BlockPredicate.ONLY_IN_AIR_PREDICATE, 16), OffsetPlacement.vertical(ConstantInt.of(-1)), BiomeFilter.biome()));
		inWater(context, P_KELP, features.getOrThrow(AquaticFeatures.KELP), 80);
		inWater(context, P_SEAGRASS, features.getOrThrow(AquaticFeatures.SEAGRASS_MID), 50);
		PlacementUtils.register(context, P_LILY_PADS, features.getOrThrow(VegetationFeatures.WATERLILY), List.of(CountPlacement.of(30),
				InSquarePlacement.spread(), upper, EnvironmentScanPlacement.scanningFor(Direction.DOWN,
						BlockPredicate.matchesBlocks(Direction.DOWN, Blocks.WATER), BlockPredicate.ONLY_IN_AIR_PREDICATE, 32),
				BlockPredicateFilter.forPredicate(BlockPredicate.wouldSurvive(Blocks.LILY_PAD)), BiomeFilter.biome()));
	}

	private static void ore(BootstrapContext<PlacedFeature> context, ResourceKey<PlacedFeature> key, Holder<Feature> feature, int count,
			PlacementModifier height) {
		PlacementUtils.register(context, key, feature, List.of(CountPlacement.of(count), InSquarePlacement.spread(), height, BiomeFilter.biome()));
	}

	/**
	 * At the bottom of a cave lake in the upper level: down from a random spot through the air to the water, then down
	 * through it to the bed, one block above it.
	 */
	private static void inWater(BootstrapContext<PlacedFeature> context, ResourceKey<PlacedFeature> key, Holder<Feature> feature, int count) {
		PlacementUtils.register(context, key, feature, List.of(CountPlacement.of(count), InSquarePlacement.spread(),
				HeightRangePlacement.uniform(VerticalAnchor.absolute(UPPER_BOTTOM), VerticalAnchor.absolute(UPPER_TOP)),
				EnvironmentScanPlacement.scanningFor(Direction.DOWN, BlockPredicate.matchesBlocks(Blocks.WATER), BlockPredicate.ONLY_IN_AIR_PREDICATE, 32),
				EnvironmentScanPlacement.scanningFor(Direction.DOWN, BlockPredicate.solid(), BlockPredicate.matchesBlocks(Blocks.WATER), 8),
				OffsetPlacement.vertical(ConstantInt.of(1)), BiomeFilter.biome()));
	}

	/** On the floor of a cave in the upper level: down from a random spot onto solid ground, one block above it. */
	private static void onFloor(BootstrapContext<PlacedFeature> context, ResourceKey<PlacedFeature> key, Holder<Feature> feature,
			PlacementModifier count, PlacementModifier... filters) {
		List<PlacementModifier> modifiers = new ArrayList<>(List.of(count, InSquarePlacement.spread(),
				HeightRangePlacement.uniform(VerticalAnchor.absolute(UPPER_BOTTOM), VerticalAnchor.absolute(UPPER_TOP)),
				EnvironmentScanPlacement.scanningFor(Direction.DOWN, BlockPredicate.solid(), BlockPredicate.ONLY_IN_AIR_PREDICATE, 32),
				OffsetPlacement.vertical(ConstantInt.of(1))));
		modifiers.addAll(List.of(filters));
		modifiers.add(BiomeFilter.biome());
		PlacementUtils.register(context, key, feature, modifiers);
	}

	/**
	 * {@code tries} of a decoration per chunk, each kept once in {@code rarity}, on a cave floor made of one of
	 * {@code floor}. The rarity is drawn after the spot: drawn first, the same random numbers would pick from fewer heights.
	 */
	private static void decoration(BootstrapContext<PlacedFeature> context, ResourceKey<PlacedFeature> key, Holder<Feature> feature, int tries,
			int rarity, Block... floor) {
		onFloor(context, key, feature, CountPlacement.of(tries), floorOf(floor), RarityFilter.onAverageOnceEvery(rarity));
	}

	private static PlacementModifier floorOf(Block... floor) {
		return BlockPredicateFilter.forPredicate(BlockPredicate.matchesBlocks(Direction.DOWN, floor));
	}

	private static WeightedStateProvider weighted(Block a, int weightA, Block b, int weightB) {
		return new WeightedStateProvider(WeightedList.<BlockState>builder().add(a.defaultBlockState(), weightA).add(b.defaultBlockState(), weightB));
	}

	private static WeightedStateProvider weighted(Block a, int weightA, Block b, int weightB, Block c, int weightC) {
		return new WeightedStateProvider(WeightedList.<BlockState>builder().add(a.defaultBlockState(), weightA).add(b.defaultBlockState(), weightB)
				.add(c.defaultBlockState(), weightC));
	}

	/** {@code count} patches of a small plant on cave floors, 12 tries around each. */
	private static void patch(BootstrapContext<PlacedFeature> context, ResourceKey<PlacedFeature> key, Holder<Feature> feature, int count,
			Block plant) {
		PlacementUtils.register(context, key, feature, List.of(CountPlacement.of(count), InSquarePlacement.spread(),
				HeightRangePlacement.uniform(VerticalAnchor.absolute(UPPER_BOTTOM), VerticalAnchor.absolute(UPPER_TOP)),
				EnvironmentScanPlacement.scanningFor(Direction.DOWN, BlockPredicate.solid(), BlockPredicate.ONLY_IN_AIR_PREDICATE, 32),
				OffsetPlacement.vertical(ConstantInt.of(1)), BiomeFilter.biome(), CountPlacement.of(12), OffsetPlacement.ofTriangle(4, 2),
				BlockPredicateFilter.forPredicate(BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE, BlockPredicate.wouldSurvive(plant)))));
	}

	// ---- Biomes ----

	static void bootstrapBiomes(BootstrapContext<Biome> context) {
		HolderGetter<PlacedFeature> placed = context.lookup(Registries.PLACED_FEATURE);
		HolderGetter<WorldCarver> carvers = context.lookup(Registries.CARVER);
		List<ResourceKey<PlacedFeature>> common = List.of(P_FLUIDS, P_ORE_IRON, P_ORE_GOLD, P_ORE_COPPER, P_ORE_REDSTONE, P_ORE_LAPIS,
				P_ORE_DIAMOND, P_ORE_SILVER, P_ORE_MITHRIL);

		context.register(UnderworldDimension.STONE_WASTES, biome(placed, carvers, 0.5F, 0x34322F, 1.0F, 0x3F4A55,
				SoundEvents.MUSIC_BIOME_DRIPSTONE_CAVES, ambience(EamSounds.UNDERWORLD_STONE_WASTES_LOOP, EamSounds.UNDERWORLD_STONE_WASTES_ADDITIONS),
				AmbientParticle.of(EamParticles.UNDERWORLD_DUST, 0.004F),
				features(common, P_LAKES_FEW, P_ORE_GRAVEL, P_SPRING_WATER, P_SPRING_LAVA, P_ROCK_PILE, P_OLD_BONES, P_BROWN_MUSHROOMS_FEW,
						P_COBWEB, P_GLOW_LICHEN),
				spawns()));
		context.register(UnderworldDimension.MUSHROOM_FOREST, biome(placed, carvers, 0.9F, 0x3A2A48, 0.75F, 0x3D57D6,
				SoundEvents.MUSIC_BIOME_LUSH_CAVES, ambience(EamSounds.UNDERWORLD_MUSHROOM_FOREST_LOOP, EamSounds.UNDERWORLD_MUSHROOM_FOREST_ADDITIONS),
				AmbientParticle.of(EamParticles.UNDERWORLD_SPORE, 0.012F),
				features(common, P_ORE_GOLD_HOT, P_ORE_REDSTONE_HOT, P_LAKES_FEW, P_SPRING_WATER, P_GIANT_BROWN, P_GIANT_RED, P_GIANT_CHANTERELLE, P_GIANT_CHAMPIGNON, P_HUGE_BROWN,
						P_HUGE_RED, P_FALLEN_MUSHROOM_STEM, P_MOSSY_BOULDER_MUSHROOM, P_BROWN_MUSHROOMS,
						P_RED_MUSHROOMS, P_GLOW_LICHEN),
				spawns()));
		List<ResourceKey<PlacedFeature>> lakePlants = List.of(P_FALLEN_POPLAR, P_MOSSY_BOULDER_LAKE, P_MOSS_PATCH, P_DRIPLEAF_CLAY, P_ORCHIDS, P_FERNS, P_SHORE_PLANTS, P_SUGAR_CANE,
				P_SPORE_BLOSSOM, P_KELP, P_SEAGRASS, P_LILY_PADS);
		context.register(UnderworldDimension.LAKE_CAVES, biome(placed, carvers, 0.6F, 0x1A3238, 0.8F, 0x2E6E8E,
				SoundEvents.MUSIC_BIOME_LUSH_CAVES, ambience(null, EamSounds.UNDERWORLD_LAKE_CAVES_ADDITIONS),
				AmbientParticle.of(ParticleTypes.DRIPPING_WATER, 0.006F),
				features(features(common, P_LAKES_MANY, P_SPRING_WATER_MANY, P_CAVE_VINES, P_GLOW_LICHEN_MANY), lakePlants),
				spawns()));
		context.register(UnderworldDimension.RIVER_CAVES, biome(placed, carvers, 0.6F, 0x1A2E3C, 0.85F, 0x2E7A8E,
				SoundEvents.MUSIC_BIOME_LUSH_CAVES, ambience(EamSounds.UNDERWORLD_RIVER_CAVES_LOOP, EamSounds.UNDERWORLD_LAKE_CAVES_ADDITIONS),
				AmbientParticle.of(ParticleTypes.DRIPPING_WATER, 0.006F),
				features(features(common, P_LAKES_FEW, P_RIVERS, P_ORE_GRAVEL, P_SPRING_WATER_MANY, P_CAVE_VINES, P_GLOW_LICHEN_MANY), lakePlants),
				spawns()));
		context.register(UnderworldDimension.FROZEN_CAVES, biome(placed, carvers, -0.5F, 0x34465A, 0.8F, 0x3938C9,
				SoundEvents.MUSIC_BIOME_FROZEN_PEAKS, ambience(EamSounds.UNDERWORLD_FROZEN_CAVES_LOOP, EamSounds.UNDERWORLD_ICE_ADDITIONS),
				AmbientParticle.of(EamParticles.UNDERWORLD_SNOW, 0.004F),
				features(common, P_ORE_DIAMOND_ICE, P_ORE_SILVER_ICE, P_ORE_MITHRIL_ICE, P_ORE_LAPIS_ICE, P_LAKES_MANY, P_ORE_GRAVEL, P_SMALL_ICE_SPIRE,
						P_SMALL_ICE_SPIKE, P_FALLEN_FROZEN_SPRUCE, P_ICE_BOULDER, P_COBWEB, P_GLOW_LICHEN),
				spawns()));
		context.register(UnderworldDimension.UNDERGROUND_FOREST, biome(placed, carvers, 0.3F, 0x122E28, 0.9F, 0x2E8E80,
				SoundEvents.MUSIC_BIOME_OLD_GROWTH_TAIGA, ambience(EamSounds.UNDERWORLD_FOREST_LOOP, EamSounds.UNDERWORLD_FOREST_ADDITIONS),
				AmbientParticle.of(ParticleTypes.GLOW, 0.004F),
				features(common, P_STREAMS, P_FALLEN_SPRUCE, P_MOSSY_BOULDER_FOREST, P_SPRUCE, P_SMALL_SPRUCE, P_FOREST_MOSS, P_FOREST_PLANTS, P_BROWN_MUSHROOMS_FEW,
						P_GLOWSHROOM_VINES, P_GLOW_LICHEN),
				spawns()));
		context.register(UnderworldDimension.BURNT_MUSHROOM_FOREST, biome(placed, carvers, 1.5F, 0x3A2E28, 0.6F, 0x3F76E4,
				SoundEvents.MUSIC_BIOME_BASALT_DELTAS, ambience(EamSounds.UNDERWORLD_BURNT_FOREST_LOOP, EamSounds.UNDERWORLD_BURNT_FOREST_ADDITIONS),
				List.of(new AmbientParticle(EamParticles.UNDERWORLD_ASH, 0.02F), new AmbientParticle(EamParticles.UNDERWORLD_EMBER, 0.0015F)),
				features(common, P_ORE_GOLD_HOT, P_ORE_REDSTONE_HOT, P_LAKES_FEW, P_ORE_MAGMA, P_SPRING_LAVA, P_BURNT_BROWN, P_BURNT_RED,
						P_SMOLDERING, P_FALLEN_BURNT_STEM, P_SCORCHED_BOULDER, P_DEAD_BUSHES),
				spawns()));
		context.register(UnderworldDimension.LAVA_CAVES, biome(placed, carvers, 2.0F, 0x4A1E10, 0.65F, 0x3F76E4,
				SoundEvents.MUSIC_BIOME_BASALT_DELTAS, ambience(EamSounds.UNDERWORLD_LAVA_CAVES_LOOP, EamSounds.UNDERWORLD_LAVA_CAVES_ADDITIONS),
				List.of(new AmbientParticle(ParticleTypes.WHITE_ASH, 0.015F), new AmbientParticle(EamParticles.UNDERWORLD_EMBER, 0.006F)),
				features(common, P_ORE_GOLD_HOT, P_ORE_REDSTONE_HOT, P_LAKES, P_LAVA_LAKES, P_ORE_MAGMA, P_SPRING_LAVA_MANY, P_LAVA_PILLAR, P_BASALT_BOULDER),
				spawns()));
		context.register(UnderworldDimension.ICE_CAVES, biome(placed, carvers, -0.7F, 0x3C5068, 0.9F, 0x3938C9,
				SoundEvents.MUSIC_BIOME_FROZEN_PEAKS, ambience(EamSounds.UNDERWORLD_ICE_CAVES_LOOP, EamSounds.UNDERWORLD_ICE_ADDITIONS),
				AmbientParticle.of(EamParticles.UNDERWORLD_SNOW, 0.008F),
				features(common, P_ORE_DIAMOND_ICE, P_ORE_SILVER_ICE, P_ORE_MITHRIL_ICE, P_ORE_LAPIS_ICE, P_LAKES_FEW, P_ICE_SPIRE, P_ICE_SPIKE, P_BIG_ICE_BOULDER,
						P_GLOW_LICHEN, P_DRAUGR_PACK),
				spawns()));
		context.register(UnderworldDimension.DEPTHS, biome(placed, carvers, 2.0F, 0x2A1410, 0.7F, 0x3F76E4,
				SoundEvents.MUSIC_BIOME_DEEP_DARK, ambience(EamSounds.UNDERWORLD_DEPTHS_LOOP, EamSounds.UNDERWORLD_DEPTHS_ADDITIONS),
				List.of(new AmbientParticle(ParticleTypes.ASH, 0.01F), new AmbientParticle(EamParticles.UNDERWORLD_EMBER, 0.002F)),
				features(List.of(P_FLUIDS, P_ORE_IRON, P_ORE_GOLD, P_ORE_DIAMOND), P_ORE_MITHRIL_DEPTHS, P_ORE_ADAMANTIUM, P_ORE_MAGMA, P_SPRING_LAVA_MANY, P_LAVA_PILLAR_DEPTHS,
						P_GLOW_LICHEN),
				spawns()));
	}

	@SafeVarargs
	private static List<ResourceKey<PlacedFeature>> features(List<ResourceKey<PlacedFeature>> common, ResourceKey<PlacedFeature>... more) {
		return features(common, List.of(more));
	}

	private static List<ResourceKey<PlacedFeature>> features(List<ResourceKey<PlacedFeature>> common, List<ResourceKey<PlacedFeature>> more) {
		List<ResourceKey<PlacedFeature>> list = new ArrayList<>(common);
		list.addAll(more);
		return list;
	}

	/**
	 * No vanilla monsters spawn in the underworld: its monsters are our own (the Lost, vampire bats, draugr, ...), added
	 * by their modules through biome tags. Only plain bats stay.
	 */
	private static MobSpawnSettings spawns() {
		MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder();
		mobs.addSpawn(EntityTypes.BAT, 10, 8, 8);
		return mobs.build();
	}

	/**
	 * A biome's sound: its own loop (none for {@code null}) and rare additions over it, both under the shared cave mood.
	 * Additions roll every tick, so 0.008 is one about every six seconds on average.
	 */
	private static AmbientSounds ambience(@Nullable SoundEvent loop, SoundEvent additions) {
		return new AmbientSounds(Optional.ofNullable(loop).map(BuiltInRegistries.SOUND_EVENT::wrapAsHolder),
				Optional.of(new AmbientMoodSettings(SoundEvents.AMBIENT_CAVE, 6000, 8, 2.0)),
				List.of(new AmbientAdditionsSettings(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(additions), 0.008)));
	}

	/**
	 * @param fogReach how far one sees in the biome, as a share of the dimension's fog distance: ash, spores, heat and
	 *                 damp air close it in
	 */
	private static Biome biome(HolderGetter<PlacedFeature> placed, HolderGetter<WorldCarver> carvers, float temperature, int fog, float fogReach,
			int water, Holder<SoundEvent> music, AmbientSounds sounds, List<AmbientParticle> particles, List<ResourceKey<PlacedFeature>> features,
			MobSpawnSettings mobs) {
		BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(placed, carvers);
		// Add in the shared order, so no two biomes ever disagree on it.
		for (Map.Entry<ResourceKey<PlacedFeature>, GenerationStep.Decoration> entry : ORDER.entrySet()) {
			if (features.contains(entry.getKey())) {
				generation.addFeature(entry.getValue(), entry.getKey());
			}
		}
		Biome.BiomeBuilder builder = new Biome.BiomeBuilder()
				.hasPrecipitation(false)
				.temperature(temperature)
				.downfall(0.0F)
				.setAttribute(EnvironmentAttributes.FOG_COLOR, ARGB.vector3fFromRGB24(fog))
				.setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(music))
				.setAttribute(EnvironmentAttributes.AMBIENT_SOUNDS, sounds)
				.specialEffects(new BiomeSpecialEffects.Builder().waterColor(water).build())
				.mobSpawnSettings(mobs)
				.generationSettings(generation.build());
		if (!particles.isEmpty()) {
			builder.setAttribute(EnvironmentAttributes.AMBIENT_PARTICLES, particles);
		}
		if (fogReach != 1.0F) {
			builder.modifyAttribute(EnvironmentAttributes.FOG_END_DISTANCE, FloatModifier.MULTIPLY, fogReach);
		}
		return builder.build();
	}

	// ---- Output ----

	static void configure(HolderLookup.Provider registries, FabricDynamicRegistryProvider.Entries entries) {
		entries.add(registries.lookupOrThrow(Registries.NOISE), TEMPERATURE_NOISE);
		entries.add(registries.lookupOrThrow(Registries.DENSITY_FUNCTION), TEMPERATURE);
		entries.add(registries.lookupOrThrow(Registries.NOISE), HUMIDITY_NOISE);
		entries.add(registries.lookupOrThrow(Registries.DENSITY_FUNCTION), HUMIDITY);
		entries.add(registries.lookupOrThrow(Registries.MATERIAL_RULE), MATERIAL_RULE);
		entries.add(registries.lookupOrThrow(Registries.NOISE_SETTINGS), UnderworldDimension.NOISE_SETTINGS);
		entries.add(registries.lookupOrThrow(Registries.DIMENSION_TYPE), UnderworldDimension.TYPE);
		var features = registries.lookupOrThrow(Registries.FEATURE);
		for (ResourceKey<Feature> key : FEATURES) {
			entries.add(features, key);
		}
		var placedFeatures = registries.lookupOrThrow(Registries.PLACED_FEATURE);
		for (ResourceKey<PlacedFeature> key : ORDER.keySet()) {
			entries.add(placedFeatures, key);
		}
		var biomes = registries.lookupOrThrow(Registries.BIOME);
		for (ResourceKey<Biome> key : UnderworldDimension.BIOMES) {
			entries.add(biomes, key);
		}
	}

	private static ResourceKey<Feature> feature(String name) {
		return ResourceKey.create(Registries.FEATURE, Eam.id(name));
	}

	private static ResourceKey<PlacedFeature> placed(String name, GenerationStep.Decoration step) {
		ResourceKey<PlacedFeature> key = ResourceKey.create(Registries.PLACED_FEATURE, Eam.id(name));
		ORDER.put(key, step);
		return key;
	}
}
