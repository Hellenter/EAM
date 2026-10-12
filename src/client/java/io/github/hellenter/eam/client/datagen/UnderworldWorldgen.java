package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamBlocks;
import io.github.hellenter.eam.content.EamWorldgen;
import io.github.hellenter.eam.dimension.UnderworldDimension;
import io.github.hellenter.eam.dimension.UnderworldLogic;
import io.github.hellenter.eam.dimension.feature.CavePillarFeature;
import io.github.hellenter.eam.dimension.feature.GiantMushroomFeature;
import io.github.hellenter.eam.dimension.feature.GiantMushroomFeature.Shape;
import io.github.hellenter.eam.dimension.feature.IceSpikeCaveFeature;
import io.github.hellenter.eam.dimension.feature.IceSpireFeature;
import io.github.hellenter.eam.mobs.entity.draugr.DraugrPackFeature;
import io.github.hellenter.eam.dimension.feature.UnderworldFluidsFeature;
import io.github.hellenter.eam.dimension.feature.UnderworldLakeFeature;

import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;

import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.CaveFeatures;
import net.minecraft.data.worldgen.features.MiscOverworldFeatures;
import net.minecraft.data.worldgen.features.OreFeatures;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.data.worldgen.features.VegetationFeatures;
import net.minecraft.data.worldgen.material.VanillaMaterialConditions;
import net.minecraft.data.worldgen.material.VanillaMaterialRules;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ARGB;
import net.minecraft.util.CubicSpline;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.attribute.AmbientMoodSettings;
import net.minecraft.world.attribute.AmbientParticle;
import net.minecraft.world.attribute.AmbientSounds;
import net.minecraft.world.attribute.BackgroundMusic;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
import net.minecraft.world.level.levelgen.feature.Feature;
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

/**
 * The underworld's datagen: its dimension type, terrain (noise settings, density functions, material rules), the six
 * biomes and their features. The level stem itself is the hand-written {@code data/eam/dimension/underworld.json}, which
 * lists the biomes by temperature.
 * <p>
 * Terrain: the nether's 3D noise in both levels, each with its own floor and ceiling slides, and solid rock between
 * them where the material rules lay the deep bedrock. The temperature that picks the biome also opens the caves up (the
 * mushroom forest most); the ice caves are one huge hall instead, floor to roof. The aquifer floods only the lower
 * level (a lava sea); upper lakes are dug by {@link UnderworldLakeFeature}, lowest floor first, and
 * {@link UnderworldFluidsFeature} turns them to lava in the hot biomes and freezes them in the ice caves.
 */
final class UnderworldWorldgen {
	static final ResourceKey<NormalNoise> TEMPERATURE_NOISE = ResourceKey.create(Registries.NOISE, Eam.id("underworld/temperature"));
	static final ResourceKey<DensityFunction> TEMPERATURE = ResourceKey.create(Registries.DENSITY_FUNCTION, Eam.id("underworld/temperature"));
	static final ResourceKey<MaterialRule> MATERIAL_RULE = ResourceKey.create(Registries.MATERIAL_RULE, Eam.id("underworld"));
	/** Hot biomes whose lakes are lava, and cold ones whose lakes freeze. */
	static final TagKey<Biome> LAVA_LAKES = TagKey.create(Registries.BIOME, Eam.id("underworld_lava_lakes"));
	static final TagKey<Biome> FROZEN_LAKES = TagKey.create(Registries.BIOME, Eam.id("underworld_frozen_lakes"));
	static final TagKey<Biome> IS_UNDERWORLD = TagKey.create(Registries.BIOME, Eam.id("is_underworld"));

	/**
	 * Temperature edges between the biomes' bands, warm to cold. Keep in step with the hand-written bands in
	 * data/eam/dimension/underworld.json. The ice caves take the widest share: the boss monument will stand there.
	 */
	static final float LAVA_EDGE = 0.32F;
	static final float MUSHROOM_EDGE = 0.13F;
	static final float LAKE_EDGE = -0.04F;
	static final float ICE_EDGE = -0.2F;
	/** Temperature at the middle of each biome's band. */
	static final float LAVA_T = (LAVA_EDGE + 1.0F) / 2.0F;
	static final float MUSHROOM_T = (MUSHROOM_EDGE + LAVA_EDGE) / 2.0F;
	static final float STONE_T = (LAKE_EDGE + MUSHROOM_EDGE) / 2.0F;
	static final float LAKE_T = (ICE_EDGE + LAKE_EDGE) / 2.0F;
	static final float ICE_T = (ICE_EDGE - 1.0F) / 2.0F;

	private static final int UPPER_BOTTOM = UnderworldLogic.BAND_TOP + 2;
	private static final int UPPER_TOP = UnderworldLogic.HEIGHT - 6;
	/** Where the ice caves' hall has its floor and its roof. */
	private static final int ICE_FLOOR = 64;
	private static final int ICE_ROOF = 172;
	/** The lava caves turn to solid rock above this height, so they are lower than the other biomes' halls. */
	private static final int LAVA_ROOF = 128;

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
	static final List<ResourceKey<Feature>> FEATURES = List.of(FLUIDS, GIANT_BROWN_MUSHROOM, GIANT_RED_MUSHROOM, ICE_SPIKE, COBWEB, LAKE, LAVA_LAKE,
			LAVA_PILLAR, ICE_SPIRE, GIANT_CHANTERELLE, GIANT_CHAMPIGNON, DRAUGR_PACK);

	/**
	 * Every placed feature of the underworld, in the one order all its biomes share (a biome takes a subset), so
	 * feature order never cycles. Values are the decoration step.
	 */
	private static final Map<ResourceKey<PlacedFeature>, GenerationStep.Decoration> ORDER = new LinkedHashMap<>();
	static final ResourceKey<PlacedFeature> P_LAKES_FEW = placed("underworld/lakes_few", GenerationStep.Decoration.LAKES);
	static final ResourceKey<PlacedFeature> P_LAKES = placed("underworld/lakes", GenerationStep.Decoration.LAKES);
	static final ResourceKey<PlacedFeature> P_LAKES_MANY = placed("underworld/lakes_many", GenerationStep.Decoration.LAKES);
	static final ResourceKey<PlacedFeature> P_LAVA_LAKES = placed("underworld/lava_lakes", GenerationStep.Decoration.LAKES);
	static final ResourceKey<PlacedFeature> P_FLUIDS = placed("underworld/fluids", GenerationStep.Decoration.LAKES);
	static final ResourceKey<PlacedFeature> P_ORE_IRON = placed("underworld/ore_iron", GenerationStep.Decoration.UNDERGROUND_ORES);
	static final ResourceKey<PlacedFeature> P_ORE_GOLD = placed("underworld/ore_gold", GenerationStep.Decoration.UNDERGROUND_ORES);
	static final ResourceKey<PlacedFeature> P_ORE_COPPER = placed("underworld/ore_copper", GenerationStep.Decoration.UNDERGROUND_ORES);
	static final ResourceKey<PlacedFeature> P_ORE_SILVER = placed("underworld/ore_silver", GenerationStep.Decoration.UNDERGROUND_ORES);
	static final ResourceKey<PlacedFeature> P_ORE_MITHRIL = placed("underworld/ore_mithril", GenerationStep.Decoration.UNDERGROUND_ORES);
	static final ResourceKey<PlacedFeature> P_ORE_MITHRIL_DEPTHS = placed("underworld/ore_mithril_depths", GenerationStep.Decoration.UNDERGROUND_ORES);
	static final ResourceKey<PlacedFeature> P_ORE_GRAVEL = placed("underworld/ore_gravel", GenerationStep.Decoration.UNDERGROUND_ORES);
	static final ResourceKey<PlacedFeature> P_ORE_MAGMA = placed("underworld/ore_magma", GenerationStep.Decoration.UNDERGROUND_ORES);
	static final ResourceKey<PlacedFeature> P_SPRING_WATER = placed("underworld/spring_water", GenerationStep.Decoration.FLUID_SPRINGS);
	static final ResourceKey<PlacedFeature> P_SPRING_WATER_MANY = placed("underworld/spring_water_many", GenerationStep.Decoration.FLUID_SPRINGS);
	static final ResourceKey<PlacedFeature> P_SPRING_LAVA = placed("underworld/spring_lava", GenerationStep.Decoration.FLUID_SPRINGS);
	static final ResourceKey<PlacedFeature> P_SPRING_LAVA_MANY = placed("underworld/spring_lava_many", GenerationStep.Decoration.FLUID_SPRINGS);
	static final ResourceKey<PlacedFeature> P_LAVA_PILLAR = placed("underworld/lava_pillar", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_LAVA_PILLAR_DEPTHS = placed("underworld/lava_pillar_depths", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_ICE_SPIRE = placed("underworld/ice_spire", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_ICE_SPIKE = placed("underworld/ice_spike", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_GIANT_BROWN = placed("underworld/giant_brown_mushroom", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_GIANT_RED = placed("underworld/giant_red_mushroom", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_GIANT_CHANTERELLE = placed("underworld/giant_chanterelle", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_GIANT_CHAMPIGNON = placed("underworld/giant_champignon", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_HUGE_BROWN = placed("underworld/huge_brown_mushroom", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_HUGE_RED = placed("underworld/huge_red_mushroom", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_BROWN_MUSHROOMS = placed("underworld/brown_mushrooms", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_RED_MUSHROOMS = placed("underworld/red_mushrooms", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_BROWN_MUSHROOMS_FEW = placed("underworld/brown_mushrooms_few", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_CAVE_VINES = placed("underworld/cave_vines", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_COBWEB = placed("underworld/cobweb", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_GLOW_LICHEN = placed("underworld/glow_lichen", GenerationStep.Decoration.VEGETAL_DECORATION);
	static final ResourceKey<PlacedFeature> P_GLOW_LICHEN_MANY = placed("underworld/glow_lichen_many", GenerationStep.Decoration.VEGETAL_DECORATION);
	/** Last, so the draugr lie in a floor that has its ice spires already. */
	static final ResourceKey<PlacedFeature> P_DRAUGR_PACK = placed("underworld/draugr_pack", GenerationStep.Decoration.VEGETAL_DECORATION);

	private UnderworldWorldgen() {
	}

	// ---- Noise and density functions ----

	static void bootstrapNoises(BootstrapContext<NormalNoise> context) {
		// Big regions: one band of temperature is many hundreds of blocks across.
		context.register(TEMPERATURE_NOISE, NormalNoise.createParity(-8, 1.0, 1.0, 0.5));
	}

	static void bootstrapDensityFunctions(BootstrapContext<DensityFunction> context) {
		HolderGetter<DensityFunction> functions = context.lookup(Registries.DENSITY_FUNCTION);
		HolderGetter<NormalNoise> noises = context.lookup(Registries.NOISE);
		context.register(TEMPERATURE, DensityFunctions.shiftedNoise2d(
				NoiseRouterData.getFunction(functions, NoiseRouterData.SHIFT_X), NoiseRouterData.getFunction(functions, NoiseRouterData.SHIFT_Z),
				0.25, noises.getOrThrow(TEMPERATURE_NOISE)));
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
				.addPoint(LAKE_T, -0.12F)
				.addPoint(STONE_T, 0.0F)
				.addPoint(MUSHROOM_T, -0.34F)
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
		DensityFunction upper = DensityFunctions.lerp(iceWeight, caves, hall);
		// Lower level: wide, lower halls between the floor bedrock and the band.
		DensityFunction lower = NoiseRouterData.slide(DensityFunctions.add(base, DensityFunctions.constant(-0.14F)), 0, UnderworldLogic.HEIGHT,
				UnderworldLogic.HEIGHT - 36, UnderworldLogic.HEIGHT - (UnderworldLogic.BAND_BOTTOM - 1), 2.5F, 4, 10, 2.5F);
		DensityFunction finalDensity = DensityFunctions.add(NoiseRouterData.postProcess(DensityFunctions.min(upper, lower), 4, 8),
				DensityFunctions.beardifier());

		// Depth is 1 in the lower level and 0 in the upper one, so the lower level is always the depths biome.
		DensityFunction depth = DensityFunctions.yClampedGradient(UnderworldLogic.LEVEL_SPLIT - 2, UnderworldLogic.LEVEL_SPLIT + 2, 1.0F, 0.0F);
		NoiseRouter router = new NoiseRouter(temperature, DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), depth,
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
						.set(EnvironmentAttributes.AMBIENT_LIGHT_COLOR, ARGB.vector3fFromRGB24(0x1A1A1E))
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
		PlacementUtils.register(context, P_FLUIDS, features.getOrThrow(FLUIDS), List.of());
		// Ores: MITE's underworld is richer than the overworld. Iron and gold everywhere, copper and silver in the upper
		// level, mithril rare above the deep bedrock and common under it.
		ore(context, P_ORE_IRON, features.getOrThrow(OreFeatures.ORE_IRON), 14, all);
		ore(context, P_ORE_GOLD, features.getOrThrow(OreFeatures.ORE_GOLD), 6, all);
		ore(context, P_ORE_COPPER, features.getOrThrow(EamWorldgen.ORE_COPPER), 10, upper);
		ore(context, P_ORE_SILVER, features.getOrThrow(EamWorldgen.ORE_SILVER), 5, upper);
		PlacementUtils.register(context, P_ORE_MITHRIL, features.getOrThrow(EamWorldgen.ORE_MITHRIL),
				List.of(RarityFilter.onAverageOnceEvery(2), InSquarePlacement.spread(), upper, BiomeFilter.biome()));
		ore(context, P_ORE_MITHRIL_DEPTHS, features.getOrThrow(EamWorldgen.ORE_MITHRIL), 4, lower);
		ore(context, P_ORE_GRAVEL, features.getOrThrow(OreFeatures.ORE_GRAVEL), 10, upper);
		ore(context, P_ORE_MAGMA, features.getOrThrow(OreFeatures.ORE_MAGMA), 6, all);

		// Springs make the waterfalls and lavafalls; most of them never find a wall with one opening and do nothing.
		ore(context, P_SPRING_WATER, features.getOrThrow(MiscOverworldFeatures.SPRING_WATER), 12, upper);
		ore(context, P_SPRING_WATER_MANY, features.getOrThrow(MiscOverworldFeatures.SPRING_WATER), 20, upper);
		ore(context, P_SPRING_LAVA, features.getOrThrow(MiscOverworldFeatures.SPRING_LAVA_OVERWORLD), 6, all);
		ore(context, P_SPRING_LAVA_MANY, features.getOrThrow(MiscOverworldFeatures.SPRING_LAVA_OVERWORLD), 40, all);

		// Pillars stand anywhere in the open and reach from floor (or lava bed) to ceiling.
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
	}

	private static void ore(BootstrapContext<PlacedFeature> context, ResourceKey<PlacedFeature> key, Holder<Feature> feature, int count,
			PlacementModifier height) {
		PlacementUtils.register(context, key, feature, List.of(CountPlacement.of(count), InSquarePlacement.spread(), height, BiomeFilter.biome()));
	}

	/** On the floor of a cave in the upper level: down from a random spot onto solid ground, one block above it. */
	private static void onFloor(BootstrapContext<PlacedFeature> context, ResourceKey<PlacedFeature> key, Holder<Feature> feature,
			PlacementModifier count) {
		PlacementUtils.register(context, key, feature, List.of(count, InSquarePlacement.spread(),
				HeightRangePlacement.uniform(VerticalAnchor.absolute(UPPER_BOTTOM), VerticalAnchor.absolute(UPPER_TOP)),
				EnvironmentScanPlacement.scanningFor(Direction.DOWN, BlockPredicate.solid(), BlockPredicate.ONLY_IN_AIR_PREDICATE, 32),
				OffsetPlacement.vertical(ConstantInt.of(1)), BiomeFilter.biome()));
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
		List<ResourceKey<PlacedFeature>> common = List.of(P_FLUIDS, P_ORE_IRON, P_ORE_GOLD, P_ORE_COPPER, P_ORE_SILVER, P_ORE_MITHRIL);

		context.register(UnderworldDimension.STONE_WASTES, biome(placed, carvers, 0.5F, 0x2E2E2E, 0x3F4A55,
				SoundEvents.MUSIC_BIOME_DRIPSTONE_CAVES, List.of(),
				features(common, P_LAKES_FEW, P_ORE_GRAVEL, P_SPRING_WATER, P_SPRING_LAVA, P_BROWN_MUSHROOMS_FEW, P_COBWEB, P_GLOW_LICHEN),
				spawns(EntityTypes.SKELETON, 100, EntityTypes.ZOMBIE, 95, EntityTypes.SPIDER, 60, EntityTypes.CAVE_SPIDER, 40,
						EntityTypes.CREEPER, 50, EntityTypes.ENDERMAN, 8, EntityTypes.WITCH, 5)));
		context.register(UnderworldDimension.MUSHROOM_FOREST, biome(placed, carvers, 0.9F, 0x2D2433, 0x3D57D6,
				SoundEvents.MUSIC_BIOME_LUSH_CAVES, AmbientParticle.of(ParticleTypes.MYCELIUM, 0.02F),
				features(common, P_LAKES_FEW, P_SPRING_WATER, P_GIANT_BROWN, P_GIANT_RED, P_GIANT_CHANTERELLE, P_GIANT_CHAMPIGNON, P_HUGE_BROWN,
						P_HUGE_RED, P_BROWN_MUSHROOMS,
						P_RED_MUSHROOMS, P_GLOW_LICHEN),
				spawns(EntityTypes.ZOMBIE, 90, EntityTypes.SKELETON, 80, EntityTypes.SPIDER, 80, EntityTypes.CREEPER, 60,
						EntityTypes.WITCH, 10, EntityTypes.ENDERMAN, 5)));
		context.register(UnderworldDimension.LAKE_CAVES, biome(placed, carvers, 0.6F, 0x1C2A2E, 0x2E6E8E,
				SoundEvents.MUSIC_BIOME_LUSH_CAVES, AmbientParticle.of(ParticleTypes.DRIPPING_WATER, 0.006F),
				features(common, P_LAKES_MANY, P_SPRING_WATER_MANY, P_CAVE_VINES, P_GLOW_LICHEN_MANY),
				spawns(EntityTypes.ZOMBIE, 90, EntityTypes.SKELETON, 90, EntityTypes.CREEPER, 40, EntityTypes.SPIDER, 40,
						EntityTypes.CAVE_SPIDER, 20, EntityTypes.WITCH, 5)));
		context.register(UnderworldDimension.LAVA_CAVES, biome(placed, carvers, 2.0F, 0x3A1A10, 0x3F76E4,
				SoundEvents.MUSIC_BIOME_BASALT_DELTAS, AmbientParticle.of(ParticleTypes.WHITE_ASH, 0.03F),
				features(common, P_LAKES, P_LAVA_LAKES, P_ORE_MAGMA, P_SPRING_LAVA_MANY, P_LAVA_PILLAR),
				spawns(EntityTypes.MAGMA_CUBE, 60, EntityTypes.SKELETON, 80, EntityTypes.ZOMBIE, 60, EntityTypes.CAVE_SPIDER, 30,
						EntityTypes.CREEPER, 20)));
		context.register(UnderworldDimension.ICE_CAVES, biome(placed, carvers, -0.7F, 0x2D3A4A, 0x3938C9,
				SoundEvents.MUSIC_BIOME_FROZEN_PEAKS, AmbientParticle.of(ParticleTypes.SNOWFLAKE, 0.004F),
				features(common, P_LAKES_FEW, P_ICE_SPIRE, P_ICE_SPIKE, P_GLOW_LICHEN, P_DRAUGR_PACK),
				spawns(EntityTypes.SKELETON, 100, EntityTypes.ZOMBIE, 80, EntityTypes.SPIDER, 40, EntityTypes.CREEPER, 40)));
		context.register(UnderworldDimension.DEPTHS, biome(placed, carvers, 2.0F, 0x2A1410, 0x3F76E4,
				SoundEvents.MUSIC_BIOME_DEEP_DARK, AmbientParticle.of(ParticleTypes.ASH, 0.01F),
				features(List.of(P_FLUIDS, P_ORE_IRON, P_ORE_GOLD), P_ORE_MITHRIL_DEPTHS, P_ORE_MAGMA, P_SPRING_LAVA_MANY, P_LAVA_PILLAR_DEPTHS,
						P_GLOW_LICHEN),
				spawns(EntityTypes.MAGMA_CUBE, 80, EntityTypes.SKELETON, 60, EntityTypes.CAVE_SPIDER, 60, EntityTypes.ZOMBIE, 40)));
	}

	@SafeVarargs
	private static List<ResourceKey<PlacedFeature>> features(List<ResourceKey<PlacedFeature>> common, ResourceKey<PlacedFeature>... more) {
		List<ResourceKey<PlacedFeature>> list = new ArrayList<>(common);
		list.addAll(List.of(more));
		return list;
	}

	private static MobSpawnSettings spawns(Object... typesAndWeights) {
		MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder();
		for (int i = 0; i < typesAndWeights.length; i += 2) {
			var type = (net.minecraft.world.entity.EntityType<?>) typesAndWeights[i];
			int weight = (Integer) typesAndWeights[i + 1];
			int group = type == EntityTypes.ENDERMAN || type == EntityTypes.WITCH ? 1 : 4;
			mobs.addSpawn(type, weight, 1, group);
		}
		mobs.addSpawn(EntityTypes.BAT, 10, 8, 8);
		return mobs.build();
	}

	private static Biome biome(HolderGetter<PlacedFeature> placed, HolderGetter<WorldCarver> carvers, float temperature, int fog, int water,
			Holder<net.minecraft.sounds.SoundEvent> music, List<AmbientParticle> particles, List<ResourceKey<PlacedFeature>> features,
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
				.setAttribute(EnvironmentAttributes.AMBIENT_SOUNDS, new AmbientSounds(Optional.empty(),
						Optional.of(new AmbientMoodSettings(SoundEvents.AMBIENT_CAVE, 6000, 8, 2.0)), List.of()))
				.specialEffects(new BiomeSpecialEffects.Builder().waterColor(water).build())
				.mobSpawnSettings(mobs)
				.generationSettings(generation.build());
		if (!particles.isEmpty()) {
			builder.setAttribute(EnvironmentAttributes.AMBIENT_PARTICLES, particles);
		}
		return builder.build();
	}

	// ---- Output ----

	static void configure(HolderLookup.Provider registries, FabricDynamicRegistryProvider.Entries entries) {
		entries.add(registries.lookupOrThrow(Registries.NOISE), TEMPERATURE_NOISE);
		entries.add(registries.lookupOrThrow(Registries.DENSITY_FUNCTION), TEMPERATURE);
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
