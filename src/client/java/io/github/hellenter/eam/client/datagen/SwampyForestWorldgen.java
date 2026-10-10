package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamBlocks;
import io.github.hellenter.eam.content.EamWorldgen;
import io.github.hellenter.eam.content.swamp.DenseShrubBlock;
import io.github.hellenter.eam.world.swamp.DuckweedMatFeature;
import io.github.hellenter.eam.world.swamp.FallenLogFeature;
import io.github.hellenter.eam.world.swamp.ShelfMushroomFeature;
import io.github.hellenter.eam.world.swamp.SwampPondFeature;
import io.github.hellenter.eam.world.swamp.SwampyForestBiomes;

import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;

import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.BlockStateProviders;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.biome.OverworldBiomes;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.data.worldgen.features.VegetationFeatures;
import net.minecraft.data.worldgen.placement.AquaticPlacements;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.data.worldgen.placement.TreePlacements;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ARGB;
import net.minecraft.util.random.WeightedList;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.attribute.BackgroundMusic;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.attribute.modifier.FloatModifier;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import net.minecraft.world.level.levelgen.feature.BlockColumnFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.RandomSelectorFeature;
import net.minecraft.world.level.levelgen.feature.SimpleBlockFeature;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.WeightedPlacedFeature;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.BlobFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.BushFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.OffsetPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.minecraft.world.level.levelgen.placement.SurfaceWaterDepthFilter;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.List;

/**
 * The swampy forest's three biomes and their features. All three share the bog flora (duckweed, cattails, swamp grass,
 * shrubs, fireweed, marsh marigold), duckweed carpets along every bank, ponds dug 1 to 3 blocks deep, and dead trunks standing on the shore and in the water.
 * <ul>
 * <li>Swampy forest: dense oak and birch forest, thickets of dense shrubs, a pond in every other chunk.</li>
 * <li>Swampy lakes: a chain of overlapping ponds, the deepest ones, with few trees, many dead trunks and duckweed mats.</li>
 * <li>Swampy hummocks: shallow water full of grassy tussocks, low birches and oak bushes.</li>
 * </ul>
 * Below y -42 each part gets as many extra mithril veins again as the overworld has there, so twice the mithril.
 */
final class SwampyForestWorldgen {
	/** Overworld mithril veins: 7 per chunk spread evenly over y -64..-32 (33 levels). Below y -42 that is 23 levels... */
	private static final int MITHRIL_DEEP_TOP = -42;
	/** ...so about 7 * 23 / 33 = 4.9 veins; the same again doubles the mithril there. */
	private static final int MITHRIL_DEEP_VEINS = 5;

	static final ResourceKey<Feature> POND_FOREST = feature("swamp_pond_forest");
	static final ResourceKey<Feature> POND_LAKES = feature("swamp_pond_lakes");
	static final ResourceKey<Feature> POND_HUMMOCKS = feature("swamp_pond_hummocks");
	static final ResourceKey<Feature> DEAD_TRUNK = feature("dead_trunk");
	static final ResourceKey<Feature> LOW_BIRCH = feature("swamp_low_birch");
	static final ResourceKey<Feature> OAK_BUSH = feature("swamp_oak_bush");
	static final ResourceKey<Feature> TREES_FOREST = feature("trees_swampy_forest");
	static final ResourceKey<Feature> TREES_LAKES = feature("trees_swampy_lakes");
	static final ResourceKey<Feature> TREES_HUMMOCKS = feature("trees_swampy_hummocks");
	static final ResourceKey<Feature> DUCKWEED_FOREST = feature("duckweed_mat_forest");
	static final ResourceKey<Feature> DUCKWEED_LAKES = feature("duckweed_mat_lakes");
	static final ResourceKey<Feature> DUCKWEED_HUMMOCKS = feature("duckweed_mat_hummocks");
	static final ResourceKey<Feature> DENSE_SHRUB = feature("dense_shrub");
	static final ResourceKey<Feature> FALLEN_HOLLOW_LOG = feature("fallen_hollow_log");
	static final ResourceKey<Feature> SHELF_MUSHROOMS = feature("shelf_mushrooms");
	static final ResourceKey<Feature> CATTAIL = feature("cattail");
	static final ResourceKey<Feature> SWAMP_GRASS = feature("swamp_grass");
	static final ResourceKey<Feature> TALL_SWAMP_GRASS = feature("tall_swamp_grass");
	static final ResourceKey<Feature> SWAMP_SHRUB = feature("swamp_shrub");
	static final ResourceKey<Feature> FIREWEED = feature("fireweed");
	static final ResourceKey<Feature> MARSH_MARIGOLD = feature("marsh_marigold");
	static final List<ResourceKey<Feature>> FEATURES = List.of(POND_FOREST, POND_LAKES, POND_HUMMOCKS, DEAD_TRUNK, LOW_BIRCH, OAK_BUSH,
			TREES_FOREST, TREES_LAKES, TREES_HUMMOCKS, DUCKWEED_FOREST, DUCKWEED_LAKES, DUCKWEED_HUMMOCKS, DENSE_SHRUB, FALLEN_HOLLOW_LOG, SHELF_MUSHROOMS, CATTAIL, SWAMP_GRASS, TALL_SWAMP_GRASS, SWAMP_SHRUB, FIREWEED, MARSH_MARIGOLD);

	/** One set of placed features per part of the swamp, each named after its part. */
	private enum Part {
		FOREST("swampy_forest"), LAKES("swampy_lakes"), HUMMOCKS("swampy_hummocks");

		final ResourceKey<PlacedFeature> pond;
		final ResourceKey<PlacedFeature> deadTrunks;
		final ResourceKey<PlacedFeature> trees;
		final ResourceKey<PlacedFeature> duckweed;
		final ResourceKey<PlacedFeature> cattails;
		final ResourceKey<PlacedFeature> grass;
		final ResourceKey<PlacedFeature> tallGrass;
		final ResourceKey<PlacedFeature> shrubs;
		final ResourceKey<PlacedFeature> denseShrubs;
		final ResourceKey<PlacedFeature> fallenLogs;
		final ResourceKey<PlacedFeature> shelfMushrooms;
		final ResourceKey<PlacedFeature> brownMushrooms;
		final ResourceKey<PlacedFeature> redMushrooms;
		final ResourceKey<PlacedFeature> fireweed;
		final ResourceKey<PlacedFeature> marigolds;

		Part(String name) {
			this.pond = placed("swamp_pond_" + name);
			this.deadTrunks = placed("dead_trunks_" + name);
			this.trees = placed("trees_" + name);
			this.duckweed = placed("duckweed_" + name);
			this.cattails = placed("cattails_" + name);
			this.grass = placed("swamp_grass_" + name);
			this.tallGrass = placed("tall_swamp_grass_" + name);
			this.shrubs = placed("swamp_shrubs_" + name);
			this.denseShrubs = placed("dense_shrubs_" + name);
			this.fallenLogs = placed("fallen_hollow_logs_" + name);
			this.shelfMushrooms = placed("shelf_mushrooms_" + name);
			this.brownMushrooms = placed("brown_mushrooms_" + name);
			this.redMushrooms = placed("red_mushrooms_" + name);
			this.fireweed = placed("fireweed_" + name);
			this.marigolds = placed("marsh_marigolds_" + name);
		}

		List<ResourceKey<PlacedFeature>> all() {
			return List.of(this.pond, this.deadTrunks, this.trees, this.duckweed, this.cattails, this.grass, this.tallGrass, this.shrubs,
					this.denseShrubs, this.fireweed, this.marigolds, this.fallenLogs, this.shelfMushrooms, this.brownMushrooms, this.redMushrooms);
		}
	}

	static final ResourceKey<PlacedFeature> ORE_MITHRIL_DEEP = placed("ore_mithril_swampy_forest");

	private SwampyForestWorldgen() {
	}

	static void bootstrapFeatures(BootstrapContext<Feature> context) {
		HolderGetter<BlockStateProvider> providers = context.lookup(Registries.BLOCK_STATE_PROVIDER);
		HolderGetter<PlacedFeature> placed = context.lookup(Registries.PLACED_FEATURE);
		HolderGetter<Feature> features = context.lookup(Registries.FEATURE);
		Holder<BlockStateProvider> soil = providers.getOrThrow(BlockStateProviders.SOIL_BENEATH_TREE);

		BlockStateProvider bottom = weighted(Blocks.MUD, 7, Blocks.CLAY, 2, Blocks.GRAVEL, 1);
		BlockStateProvider bank = weighted(Blocks.MUD, 3, Blocks.MOSS_BLOCK, 2, Blocks.COARSE_DIRT, 1);
		context.register(POND_FOREST, new SwampPondFeature(UniformInt.of(2, 4), 2, 0.0F, bottom, bank));
		context.register(POND_LAKES, new SwampPondFeature(UniformInt.of(3, 6), 3, 0.08F, bottom, bank));
		context.register(POND_HUMMOCKS, new SwampPondFeature(UniformInt.of(3, 5), 2, 0.35F, bottom, bank));

		// A dead trunk is one kind of log, bark on or bark peeled off, 2 to 6 blocks tall, on land or out of shallow water.
		List<WeightedPlacedFeature> trunks = new ArrayList<>();
		for (Block log : List.of(Blocks.BIRCH_LOG, Blocks.STRIPPED_OAK_LOG, Blocks.OAK_LOG)) {
			trunks.add(new WeightedPlacedFeature(PlacementUtils.inlinePlaced(deadTrunk(log)), 0.25F));
		}
		context.register(DEAD_TRUNK, new RandomSelectorFeature(trunks, PlacementUtils.inlinePlaced(deadTrunk(Blocks.STRIPPED_BIRCH_LOG))));

		context.register(LOW_BIRCH, new TreeFeature.Builder(BlockStateProvider.of(Blocks.BIRCH_LOG), new StraightTrunkPlacer(2, 1, 0),
				BlockStateProvider.of(Blocks.BIRCH_LEAVES), new BlobFoliagePlacer(ConstantInt.of(2), ConstantInt.of(0), 2),
				new TwoLayersFeatureSize(1, 0, 1), soil).ignoreVines().build());
		context.register(OAK_BUSH, new TreeFeature.Builder(BlockStateProvider.of(Blocks.OAK_LOG), new StraightTrunkPlacer(1, 0, 0),
				BlockStateProvider.of(Blocks.OAK_LEAVES), new BushFoliagePlacer(ConstantInt.of(2), ConstantInt.of(1), 2),
				new TwoLayersFeatureSize(0, 0, 0), soil).build());

		Holder<PlacedFeature> birch = tree(features, TreeFeatures.BIRCH, Blocks.BIRCH_SAPLING);
		Holder<PlacedFeature> oak = tree(features, TreeFeatures.OAK, Blocks.OAK_SAPLING);
		Holder<PlacedFeature> fancyOak = tree(features, TreeFeatures.FANCY_OAK, Blocks.OAK_SAPLING);
		Holder<PlacedFeature> swampOak = tree(features, TreeFeatures.SWAMP_OAK, Blocks.OAK_SAPLING);
		Holder<PlacedFeature> lowBirch = tree(features, LOW_BIRCH, Blocks.BIRCH_SAPLING);
		Holder<PlacedFeature> oakBush = tree(features, OAK_BUSH, Blocks.OAK_SAPLING);
		Holder<PlacedFeature> fallenBirch = placed.getOrThrow(TreePlacements.FALLEN_BIRCH_TREE);
		Holder<PlacedFeature> fallenOak = placed.getOrThrow(TreePlacements.FALLEN_OAK_TREE);
		context.register(TREES_FOREST, new RandomSelectorFeature(List.of(
				new WeightedPlacedFeature(fallenBirch, 0.02F),
				new WeightedPlacedFeature(fallenOak, 0.02F),
				new WeightedPlacedFeature(birch, 0.45F),
				new WeightedPlacedFeature(swampOak, 0.25F),
				new WeightedPlacedFeature(fancyOak, 0.08F)), oak));
		context.register(TREES_LAKES, new RandomSelectorFeature(List.of(
				new WeightedPlacedFeature(fallenBirch, 0.05F),
				new WeightedPlacedFeature(swampOak, 0.5F),
				new WeightedPlacedFeature(birch, 0.3F)), oak));
		context.register(TREES_HUMMOCKS, new RandomSelectorFeature(List.of(
				new WeightedPlacedFeature(lowBirch, 0.5F),
				new WeightedPlacedFeature(oakBush, 0.35F)), birch));

		// Duckweed mats reach further out over the open lakes than over forest ponds.
		context.register(DUCKWEED_FOREST, new DuckweedMatFeature(4));
		context.register(DUCKWEED_LAKES, new DuckweedMatFeature(6));
		context.register(DUCKWEED_HUMMOCKS, new DuckweedMatFeature(5));
		// A dense shrub, half of them with a second, all-leaves block on top.
		BlockState shrub = EamBlocks.DENSE_SHRUB.defaultBlockState();
		context.register(DENSE_SHRUB, new BlockColumnFeature(List.of(
				BlockColumnFeature.layer(ConstantInt.of(1), BlockStateProvider.of(shrub)),
				BlockColumnFeature.layer(UniformInt.of(0, 1), BlockStateProvider.of(shrub.setValue(DenseShrubBlock.TOP, true)))),
				Direction.UP, BlockPredicate.ONLY_IN_AIR_PREDICATE, false));
		// Fallen hollow oak and birch logs, on the ground or floating in the ponds.
		context.register(FALLEN_HOLLOW_LOG, new FallenLogFeature(HolderSet.direct(Block::builtInRegistryHolder, EamBlocks.HOLLOW_OAK_LOG,
				EamBlocks.HOLLOW_BIRCH_LOG), UniformInt.of(3, 6)));
		context.register(SHELF_MUSHROOMS, new ShelfMushroomFeature(24));
		context.register(CATTAIL, new SimpleBlockFeature(BlockStateProvider.of(EamBlocks.CATTAIL)));
		context.register(SWAMP_GRASS, new SimpleBlockFeature(weighted(EamBlocks.SWAMP_GRASS, 6, Blocks.SHORT_GRASS, 2, Blocks.FERN, 2)));
		context.register(TALL_SWAMP_GRASS, new SimpleBlockFeature(BlockStateProvider.of(EamBlocks.TALL_SWAMP_GRASS)));
		context.register(SWAMP_SHRUB, new SimpleBlockFeature(BlockStateProvider.of(EamBlocks.SWAMP_SHRUB)));
		context.register(FIREWEED, new SimpleBlockFeature(BlockStateProvider.of(EamBlocks.FIREWEED)));
		context.register(MARSH_MARIGOLD, new SimpleBlockFeature(BlockStateProvider.of(EamBlocks.MARSH_MARIGOLD)));
	}

	private static BlockColumnFeature deadTrunk(Block log) {
		return new BlockColumnFeature(List.of(BlockColumnFeature.layer(UniformInt.of(2, 6), BlockStateProvider.of(log))), Direction.UP,
				BlockPredicate.anyOf(BlockPredicate.ONLY_IN_AIR_PREDICATE, BlockPredicate.matchesFluids(Fluids.WATER)), false);
	}

	private static Holder<PlacedFeature> tree(HolderGetter<Feature> features, ResourceKey<Feature> tree, Block sapling) {
		return PlacementUtils.inlinePlaced(features.getOrThrow(tree), PlacementUtils.filteredByBlockSurvival(sapling));
	}

	static void bootstrapPlacedFeatures(BootstrapContext<PlacedFeature> context) {
		HolderGetter<Feature> features = context.lookup(Registries.FEATURE);
		// counts per chunk:  pond trunks trees cattails grass tall shrubs thickets fireweed marigolds fallen-logs shelves brown red
		placeAll(context, features, Part.FOREST, POND_FOREST, TREES_FOREST, DUCKWEED_FOREST,
				new int[] {-2, -2, 16, 2, 4, 1, 2, 5, -2, 1, -2, 1, 2, -2});
		placeAll(context, features, Part.LAKES, POND_LAKES, TREES_LAKES, DUCKWEED_LAKES,
				new int[] {3, 2, 3, 6, 3, 2, 1, 1, 1, 2, 2, 1, 1, -3});
		placeAll(context, features, Part.HUMMOCKS, POND_HUMMOCKS, TREES_HUMMOCKS, DUCKWEED_HUMMOCKS,
				new int[] {2, 1, 7, 3, 6, 7, 3, 3, -3, 1, 1, 1, 1, -3});

		PlacementUtils.register(context, ORE_MITHRIL_DEEP, features.getOrThrow(EamWorldgen.ORE_MITHRIL), List.of(
				CountPlacement.of(MITHRIL_DEEP_VEINS),
				InSquarePlacement.spread(),
				HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(MITHRIL_DEEP_TOP)),
				BiomeFilter.biome()));
	}

	/** A positive count is that many tries per chunk; a negative one, -n, is one try every n chunks. */
	private static void placeAll(BootstrapContext<PlacedFeature> context, HolderGetter<Feature> features, Part part,
			ResourceKey<Feature> pond, ResourceKey<Feature> trees, ResourceKey<Feature> duckweed, int[] counts) {
		PlacementUtils.register(context, part.pond, features.getOrThrow(pond),
				List.of(frequency(counts[0]), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_WORLD_SURFACE, BiomeFilter.biome()));
		PlacementUtils.register(context, part.deadTrunks, features.getOrThrow(DEAD_TRUNK), List.of(frequency(counts[1]),
				InSquarePlacement.spread(), SurfaceWaterDepthFilter.forMaxDepth(2), PlacementUtils.HEIGHTMAP_TOP_SOLID, BiomeFilter.biome(),
				BlockPredicateFilter.forPredicate(BlockPredicate.wouldSurvive(Blocks.OAK_SAPLING))));
		PlacementUtils.register(context, part.trees, features.getOrThrow(trees), List.of(PlacementUtils.countExtra(counts[2], 0.1F, 1),
				InSquarePlacement.spread(), SurfaceWaterDepthFilter.forMaxDepth(1), PlacementUtils.HEIGHTMAP_OCEAN_FLOOR, BiomeFilter.biome()));
		// Duckweed covers the water of the whole chunk at once, by distance from the banks.
		PlacementUtils.register(context, part.duckweed, features.getOrThrow(duckweed), List.of());
		// Cattails in water one block deep, or on dry ground right at the water's edge.
		PlacementUtils.register(context, part.cattails, features.getOrThrow(CATTAIL),
				patch(counts[3], PlacementUtils.HEIGHTMAP_TOP_SOLID, 24, 5, 2, BlockPredicate.anyOf(
						BlockPredicate.allOf(BlockPredicate.matchesFluids(Fluids.WATER), BlockPredicate.matchesBlocks(Direction.UP, Blocks.AIR)),
						BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE, nearWater()))));
		PlacementUtils.register(context, part.grass, features.getOrThrow(SWAMP_GRASS),
				patch(counts[4], PlacementUtils.HEIGHTMAP_WORLD_SURFACE, 32, 7, 3, BlockPredicate.ONLY_IN_AIR_PREDICATE));
		PlacementUtils.register(context, part.tallGrass, features.getOrThrow(TALL_SWAMP_GRASS),
				patch(counts[5], PlacementUtils.HEIGHTMAP_WORLD_SURFACE, 16, 6, 2, BlockPredicate.ONLY_IN_AIR_PREDICATE));
		PlacementUtils.register(context, part.shrubs, features.getOrThrow(SWAMP_SHRUB),
				patch(counts[6], PlacementUtils.HEIGHTMAP_WORLD_SURFACE, 8, 5, 2, BlockPredicate.ONLY_IN_AIR_PREDICATE));
		// Thickets of dense shrubs: many tries packed close, so they grow wall to wall.
		PlacementUtils.register(context, part.denseShrubs, features.getOrThrow(DENSE_SHRUB),
				patch(counts[7], PlacementUtils.HEIGHTMAP_WORLD_SURFACE, 40, 4, 2, BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE,
						BlockPredicate.wouldSurvive(EamBlocks.DENSE_SHRUB))));
		PlacementUtils.register(context, part.fallenLogs, features.getOrThrow(FALLEN_HOLLOW_LOG),
				List.of(frequency(counts[10]), InSquarePlacement.spread(), BiomeFilter.biome()));
		// Shelf mushrooms search the chunk's trunks themselves; the count is how many times.
		PlacementUtils.register(context, part.shelfMushrooms, features.getOrThrow(SHELF_MUSHROOMS), List.of(frequency(counts[11])));
		PlacementUtils.register(context, part.brownMushrooms, features.getOrThrow(VegetationFeatures.BROWN_MUSHROOM),
				patch(counts[12], PlacementUtils.HEIGHTMAP_WORLD_SURFACE, 12, 4, 2, BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE,
						BlockPredicate.wouldSurvive(Blocks.BROWN_MUSHROOM))));
		PlacementUtils.register(context, part.redMushrooms, features.getOrThrow(VegetationFeatures.RED_MUSHROOM),
				patch(counts[13], PlacementUtils.HEIGHTMAP_WORLD_SURFACE, 8, 4, 2, BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE,
						BlockPredicate.wouldSurvive(Blocks.RED_MUSHROOM))));
		PlacementUtils.register(context, part.fireweed, features.getOrThrow(FIREWEED),
				patch(counts[8], PlacementUtils.HEIGHTMAP_WORLD_SURFACE, 16, 5, 2, BlockPredicate.ONLY_IN_AIR_PREDICATE));
		PlacementUtils.register(context, part.marigolds, features.getOrThrow(MARSH_MARIGOLD),
				patch(counts[9], PlacementUtils.HEIGHTMAP_WORLD_SURFACE, 16, 5, 1,
						BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE, nearWater())));
	}

	private static PlacementModifier frequency(int count) {
		return count > 0 ? CountPlacement.of(count) : RarityFilter.onAverageOnceEvery(-count);
	}

	/** {@code count} patches per chunk, each with {@code tries} spots spread around its centre. */
	private static List<PlacementModifier> patch(int count, PlacementModifier heightmap, int tries, int spread, int ySpread, BlockPredicate spot) {
		return List.of(frequency(count), InSquarePlacement.spread(), heightmap, BiomeFilter.biome(), CountPlacement.of(tries),
				OffsetPlacement.ofTriangle(spread, ySpread), BlockPredicateFilter.forPredicate(spot));
	}

	/** Water on a side, one block down: the spot is on the bank of a pond. */
	private static BlockPredicate nearWater() {
		return BlockPredicate.anyOf(
				BlockPredicate.matchesFluids(new Vec3i(1, -1, 0), List.of(Fluids.WATER)),
				BlockPredicate.matchesFluids(new Vec3i(-1, -1, 0), List.of(Fluids.WATER)),
				BlockPredicate.matchesFluids(new Vec3i(0, -1, 1), List.of(Fluids.WATER)),
				BlockPredicate.matchesFluids(new Vec3i(0, -1, -1), List.of(Fluids.WATER)));
	}

	static void bootstrapBiomes(BootstrapContext<Biome> context) {
		HolderGetter<PlacedFeature> placed = context.lookup(Registries.PLACED_FEATURE);
		HolderGetter<WorldCarver> carvers = context.lookup(Registries.CARVER);
		// Lush, bright summer greens; the lakes a shade darker and bluer, the drier hummocks yellower.
		context.register(SwampyForestBiomes.SWAMPY_FOREST, biome(placed, carvers, Part.FOREST, 0x4F7F5C, 0x6DB33A, 0x58A02C));
		context.register(SwampyForestBiomes.SWAMPY_LAKES, biome(placed, carvers, Part.LAKES, 0x48745A, 0x64A83A, 0x4F962E));
		context.register(SwampyForestBiomes.SWAMPY_HUMMOCKS, biome(placed, carvers, Part.HUMMOCKS, 0x537D57, 0x86BA40, 0x6CAA32));
	}

	/** Forest temperature and rain, swamp music and murky water. */
	private static Biome biome(HolderGetter<PlacedFeature> placed, HolderGetter<WorldCarver> carvers, Part part, int water, int grass, int foliage) {
		BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(placed, carvers);
		BiomeDefaultFeatures.addFossilDecoration(generation);
		OverworldBiomes.globalOverworldGeneration(generation);
		generation.addFeature(GenerationStep.Decoration.LAKES, part.pond);
		BiomeDefaultFeatures.addDefaultOres(generation);
		BiomeDefaultFeatures.addSwampClayDisk(generation);
		generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, ORE_MITHRIL_DEEP);
		GenerationStep.Decoration vegetal = GenerationStep.Decoration.VEGETAL_DECORATION;
		generation.addFeature(vegetal, part.deadTrunks);
		generation.addFeature(vegetal, part.fallenLogs);
		generation.addFeature(vegetal, part.trees);
		generation.addFeature(vegetal, part.shelfMushrooms);
		generation.addFeature(vegetal, part.cattails);
		generation.addFeature(vegetal, part.tallGrass);
		generation.addFeature(vegetal, part.fireweed);
		generation.addFeature(vegetal, part.shrubs);
		generation.addFeature(vegetal, part.denseShrubs);
		generation.addFeature(vegetal, part.brownMushrooms);
		generation.addFeature(vegetal, part.redMushrooms);
		generation.addFeature(vegetal, part.grass);
		generation.addFeature(vegetal, part.marigolds);
		generation.addFeature(vegetal, part.duckweed);
		// Vanilla features keep the order they have in the vanilla swamp: every biome must agree on it.
		if (part == Part.FOREST) {
			generation.addFeature(vegetal, VegetationPlacements.PATCH_LARGE_FERN);
		} else {
			generation.addFeature(vegetal, VegetationPlacements.PATCH_WATERLILY);
		}
		generation.addFeature(vegetal, VegetationPlacements.BROWN_MUSHROOM_SWAMP);
		generation.addFeature(vegetal, VegetationPlacements.RED_MUSHROOM_SWAMP);
		if (part == Part.FOREST) {
			generation.addFeature(vegetal, VegetationPlacements.PATCH_LEAF_LITTER);
		}
		generation.addFeature(vegetal, VegetationPlacements.PATCH_PUMPKIN);
		generation.addFeature(vegetal, VegetationPlacements.PATCH_FIREFLY_BUSH_NEAR_WATER_SWAMP);
		if (part != Part.FOREST) {
			generation.addFeature(vegetal, AquaticPlacements.SEAGRASS_SWAMP);
		}

		MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder();
		BiomeDefaultFeatures.farmAnimals(mobs);
		BiomeDefaultFeatures.swampSpawns(mobs, 70);

		return OverworldBiomes.baseBiome(0.7F, 0.8F)
				.setAttribute(EnvironmentAttributes.WATER_FOG_COLOR, ARGB.vector3fFromRGB24(0x232317))
				.modifyAttribute(EnvironmentAttributes.WATER_FOG_END_DISTANCE, FloatModifier.MULTIPLY, 0.85F)
				.setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(SoundEvents.MUSIC_BIOME_SWAMP))
				.specialEffects(new BiomeSpecialEffects.Builder()
						.waterColor(water)
						.grassColorOverride(grass)
						.foliageColorOverride(foliage)
						.build())
				.mobSpawnSettings(mobs.build())
				.generationSettings(generation.build())
				.build();
	}

	static void configure(HolderLookup.Provider registries, FabricDynamicRegistryProvider.Entries entries) {
		var features = registries.lookupOrThrow(Registries.FEATURE);
		for (ResourceKey<Feature> key : FEATURES) {
			entries.add(features, key);
		}
		var placedFeatures = registries.lookupOrThrow(Registries.PLACED_FEATURE);
		for (Part part : Part.values()) {
			for (ResourceKey<PlacedFeature> key : part.all()) {
				entries.add(placedFeatures, key);
			}
		}
		entries.add(placedFeatures, ORE_MITHRIL_DEEP);
		var biomes = registries.lookupOrThrow(Registries.BIOME);
		for (ResourceKey<Biome> key : SwampyForestBiomes.ALL) {
			entries.add(biomes, key);
		}
	}

	private static BlockStateProvider weighted(Block a, int wa, Block b, int wb, Block c, int wc) {
		return new WeightedStateProvider(WeightedList.<BlockState>builder()
				.add(a.defaultBlockState(), wa).add(b.defaultBlockState(), wb).add(c.defaultBlockState(), wc));
	}

	private static ResourceKey<Feature> feature(String name) {
		return ResourceKey.create(Registries.FEATURE, Eam.id(name));
	}

	private static ResourceKey<PlacedFeature> placed(String name) {
		return ResourceKey.create(Registries.PLACED_FEATURE, Eam.id(name));
	}
}
