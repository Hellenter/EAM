package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.content.EamBlocks;
import io.github.hellenter.eam.content.EamWorldgen;
import io.github.hellenter.eam.content.ground.GroundItemBlock;
import io.github.hellenter.eam.content.ground.GroundItemLogic;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.BlockReplacement;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import net.minecraft.world.level.levelgen.feature.SimpleBlockFeature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.RandomizedIntStateProvider;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.OffsetPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.minecraft.world.level.levelgen.structure.templatesystem.HeightMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Pebbles, flint shards and sticks on the surface. In forests a pebble gets 0.53 tries per chunk on average
 * (8 tries, each kept 1 time in 15) and a flint shard 1 try in 20 chunks; in the mountains they lie only on
 * stone and gravel, with a flint shard 1 try in 10 chunks. Stony shores get 2.5 times the forest's pebbles
 * (1.33 tries per chunk) and one flint shard try for every 6 pebble tries. Sticks get 1 try per chunk
 * in any overworld biome and stay only where a tree trunk stands close by, on solid ground, never on water or ice.
 * <p>
 * Copper ore: 12 veins per chunk instead of vanilla's 16, at vanilla heights, each the size of an average diamond vein
 * (vanilla diamond veins are size 4, 8 and 12, about 6 on average) instead of 10, or 20 in dripstone caves.
 * <p>
 * Silver ore: copper's veins, heights and targets, 2.5 times fewer (4.8 veins per chunk: 24 tries, each kept 1 time in 5).
 * Mountains get a second, equal set of veins, so twice the silver.
 * <p>
 * Mithril ore: only below y -32, in deepslate (stone ore where stone is left), veins of size 6, half of the ore exposed
 * to air discarded like diamond. 2.8 veins per chunk (14 tries, each kept 1 time in 5), 60% fewer than the first 7.
 * Mountains and the swampy forest get 2.45 more (49 tries kept 1 in 20), 5.25 in all: 25% fewer than 7.
 */
public class EamWorldgenProvider extends FabricDynamicRegistryProvider {
	/** A stick needs a log at most this many blocks away sideways... */
	private static final int TREE_RADIUS = 3;
	/** ...and at most this many blocks above the ground it lies on, so a tree's own trunk counts but its crown alone does not. */
	private static final int TREE_TRUNK_HEIGHT = 4;
	private static final int COPPER_VEIN_SIZE = 6;
	private static final int COPPER_VEINS_PER_CHUNK = 12;
	private static final int SILVER_VEIN_TRIES = 24;
	private static final int SILVER_VEIN_RARITY = 5;
	private static final int MITHRIL_VEIN_SIZE = 6;
	private static final int MITHRIL_VEIN_TRIES = 14;
	private static final int MITHRIL_VEIN_RARITY = 5;
	private static final int MITHRIL_RICH_VEIN_TRIES = 49;
	private static final int MITHRIL_RICH_VEIN_RARITY = 20;
	private static final int MITHRIL_TOP = -32;
	private static final float MITHRIL_DISCARD_ON_AIR = 0.5F;
	private static final int ADAMANTIUM_VEIN_SIZE = 4;
	/** Most adamantium touching air is gone: it is found by digging, not by looking at cave walls. */
	private static final float ADAMANTIUM_DISCARD_ON_AIR = 0.7F;
	private static final int WILD_GARLIC_RARITY = 12;
	private static final int WILD_GARLIC_TRIES = 16;

	public EamWorldgenProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	public static void bootstrapFeatures(BootstrapContext<Feature> context) {
		context.register(EamWorldgen.PEBBLE, groundItem(EamBlocks.PEBBLE));
		context.register(EamWorldgen.FLINT_SHARD, groundItem(EamBlocks.FLINT_SHARD));
		context.register(EamWorldgen.GROUND_STICK, groundItem(EamBlocks.GROUND_STICK));

		// Same targets as vanilla copper: stone above y 0, deepslate below y 8.
		RuleTest stone = RuleTest.either(new TagMatchTest(BlockTags.HEIGHT_SPECIFIC_ORE_REPLACEABLES), HeightMatchTest.min(0),
				new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES));
		RuleTest deepslate = RuleTest.either(new TagMatchTest(BlockTags.HEIGHT_SPECIFIC_ORE_REPLACEABLES), HeightMatchTest.max(8),
				new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES));
		context.register(EamWorldgen.ORE_COPPER, new OreFeature(List.of(
				BlockReplacement.replace(stone, Blocks.COPPER_ORE.defaultBlockState()),
				BlockReplacement.replace(deepslate, Blocks.DEEPSLATE_COPPER_ORE.defaultBlockState())
		), COPPER_VEIN_SIZE));
		context.register(EamWorldgen.ORE_SILVER, new OreFeature(List.of(
				BlockReplacement.replace(stone, EamBlocks.SILVER_ORE.defaultBlockState()),
				BlockReplacement.replace(deepslate, EamBlocks.DEEPSLATE_SILVER_ORE.defaultBlockState())
		), COPPER_VEIN_SIZE));
		context.register(EamWorldgen.WILD_GARLIC, new SimpleBlockFeature(BlockStateProvider.of(EamBlocks.WILD_GARLIC)));
		mithrilFeature(context);
		SwampyForestWorldgen.bootstrapFeatures(context);
		UnderworldWorldgen.bootstrapFeatures(context);
	}

	private static void mithrilFeature(BootstrapContext<Feature> context) {
		context.register(EamWorldgen.ORE_MITHRIL, new OreFeature(List.of(
				BlockReplacement.replace(new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES), EamBlocks.MITHRIL_ORE.defaultBlockState()),
				BlockReplacement.replace(new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES), EamBlocks.DEEPSLATE_MITHRIL_ORE.defaultBlockState())
		), MITHRIL_VEIN_SIZE, MITHRIL_DISCARD_ON_AIR));
		context.register(EamWorldgen.ORE_ADAMANTIUM, new OreFeature(List.of(
				BlockReplacement.replace(new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES), EamBlocks.ADAMANTIUM_ORE.defaultBlockState()),
				BlockReplacement.replace(new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES), EamBlocks.DEEPSLATE_ADAMANTIUM_ORE.defaultBlockState())
		), ADAMANTIUM_VEIN_SIZE, ADAMANTIUM_DISCARD_ON_AIR));
	}

	/** Every EAM biome: the swampy forest and the underworld. */
	public static void bootstrapBiomes(BootstrapContext<Biome> context) {
		SwampyForestWorldgen.bootstrapBiomes(context);
		UnderworldWorldgen.bootstrapBiomes(context);
	}

	public static void bootstrapPlacedFeatures(BootstrapContext<PlacedFeature> context) {
		HolderGetter<Feature> features = context.lookup(Registries.FEATURE);
		var pebble = features.getOrThrow(EamWorldgen.PEBBLE);
		var flintShard = features.getOrThrow(EamWorldgen.FLINT_SHARD);
		var stick = features.getOrThrow(EamWorldgen.GROUND_STICK);
		var copper = features.getOrThrow(EamWorldgen.ORE_COPPER);
		var silver = features.getOrThrow(EamWorldgen.ORE_SILVER);

		PlacementUtils.register(context, EamWorldgen.PEBBLE_FOREST, pebble,
				onSurface(forestFloor(), CountPlacement.of(8), RarityFilter.onAverageOnceEvery(15)));
		PlacementUtils.register(context, EamWorldgen.FLINT_SHARD_FOREST, flintShard,
				onSurface(forestFloor(), RarityFilter.onAverageOnceEvery(20)));
		PlacementUtils.register(context, EamWorldgen.PEBBLE_MOUNTAIN, pebble,
				onSurface(stoneOrGravel(), CountPlacement.of(8), RarityFilter.onAverageOnceEvery(15)));
		PlacementUtils.register(context, EamWorldgen.FLINT_SHARD_MOUNTAIN, flintShard,
				onSurface(stoneOrGravel(), RarityFilter.onAverageOnceEvery(10)));
		PlacementUtils.register(context, EamWorldgen.PEBBLE_SHORE, pebble,
				onSurface(stoneOrGravel(), CountPlacement.of(4), RarityFilter.onAverageOnceEvery(3)));
		PlacementUtils.register(context, EamWorldgen.FLINT_SHARD_SHORE, flintShard,
				onSurface(stoneOrGravel(), CountPlacement.of(2), RarityFilter.onAverageOnceEvery(9)));
		PlacementUtils.register(context, EamWorldgen.GROUND_STICK_NEAR_TREES, stick, List.of(
				CountPlacement.of(1),
				InSquarePlacement.spread(),
				PlacementUtils.HEIGHTMAP_NO_LEAVES,
				BiomeFilter.biome(),
				BlockPredicateFilter.forPredicate(BlockPredicate.allOf(stickSpot(), solidDryGround(), nearTree()))));
		PlacementUtils.register(context, EamWorldgen.ORE_COPPER_PLACED, copper, List.of(
				CountPlacement.of(COPPER_VEINS_PER_CHUNK),
				InSquarePlacement.spread(),
				HeightRangePlacement.triangle(VerticalAnchor.absolute(-16), VerticalAnchor.absolute(112)),
				BiomeFilter.biome()));
		// A patch of up to a dozen plants once in 12 chunks on average, wherever the plant can root.
		PlacementUtils.register(context, EamWorldgen.WILD_GARLIC_PATCH, features.getOrThrow(EamWorldgen.WILD_GARLIC), List.of(
				RarityFilter.onAverageOnceEvery(WILD_GARLIC_RARITY),
				InSquarePlacement.spread(),
				PlacementUtils.HEIGHTMAP_NO_LEAVES,
				BiomeFilter.biome(),
				CountPlacement.of(WILD_GARLIC_TRIES),
				OffsetPlacement.ofTriangle(4, 2),
				BlockPredicateFilter.forPredicate(BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE,
						BlockPredicate.wouldSurvive(EamBlocks.WILD_GARLIC)))));
		PlacementUtils.register(context, EamWorldgen.ORE_SILVER_PLACED, silver, silverOre());
		PlacementUtils.register(context, EamWorldgen.ORE_SILVER_MOUNTAIN, silver, silverOre());
		var mithril = features.getOrThrow(EamWorldgen.ORE_MITHRIL);
		PlacementUtils.register(context, EamWorldgen.ORE_MITHRIL_PLACED, mithril, mithrilOre(MITHRIL_VEIN_TRIES, MITHRIL_VEIN_RARITY));
		PlacementUtils.register(context, EamWorldgen.ORE_MITHRIL_RICH, mithril, mithrilOre(MITHRIL_RICH_VEIN_TRIES, MITHRIL_RICH_VEIN_RARITY));
		SwampyForestWorldgen.bootstrapPlacedFeatures(context);
		UnderworldWorldgen.bootstrapPlacedFeatures(context);
	}

	/** Mithril below y -32: {@code tries} veins per chunk, each kept 1 time in {@code rarity}. */
	private static List<PlacementModifier> mithrilOre(int tries, int rarity) {
		return List.of(
				CountPlacement.of(tries),
				RarityFilter.onAverageOnceEvery(rarity),
				InSquarePlacement.spread(),
				HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(MITHRIL_TOP)),
				BiomeFilter.biome());
	}

	/** Copper's placement with 2.5 times fewer veins. */
	private static List<PlacementModifier> silverOre() {
		return List.of(
				CountPlacement.of(SILVER_VEIN_TRIES),
				RarityFilter.onAverageOnceEvery(SILVER_VEIN_RARITY),
				InSquarePlacement.spread(),
				HeightRangePlacement.triangle(VerticalAnchor.absolute(-16), VerticalAnchor.absolute(112)),
				BiomeFilter.biome());
	}

	private static Feature groundItem(Block block) {
		return new SimpleBlockFeature(new RandomizedIntStateProvider(
				BlockStateProvider.of(block), GroundItemBlock.POSITION, UniformInt.of(0, GroundItemLogic.POSITIONS - 1)));
	}

	/** {@code frequency} decides how many tries a chunk gets; each try lands on a random surface spot. */
	private static List<PlacementModifier> onSurface(BlockPredicate ground, PlacementModifier... frequency) {
		List<PlacementModifier> modifiers = new ArrayList<>(List.of(frequency));
		modifiers.add(InSquarePlacement.spread());
		modifiers.add(PlacementUtils.HEIGHTMAP_NO_LEAVES);
		modifiers.add(BiomeFilter.biome());
		modifiers.add(BlockPredicateFilter.forPredicate(BlockPredicate.allOf(freeSpot(), ground)));
		return modifiers;
	}

	/**
	 * Air, or ground cover the item lies on top of in its place: forests are carpeted with leaf litter,
	 * grass and ferns, which left almost no air spots for pebbles and sticks.
	 */
	private static BlockPredicate freeSpot() {
		return BlockPredicate.anyOf(
				BlockPredicate.ONLY_IN_AIR_PREDICATE,
				BlockPredicate.matchesBlocks(Blocks.LEAF_LITTER, Blocks.SHORT_GRASS, Blocks.FERN, Blocks.BUSH, Blocks.SHORT_DRY_GRASS)
		);
	}

	/** Dirt, grass, podzol, moss, mud, stone or gravel underneath. */
	private static BlockPredicate forestFloor() {
		return BlockPredicate.anyOf(
				BlockPredicate.matchesTag(Direction.DOWN, BlockTags.SUBSTRATE_OVERWORLD),
				stoneOrGravel()
		);
	}

	private static BlockPredicate stoneOrGravel() {
		return BlockPredicate.anyOf(
				BlockPredicate.matchesTag(Direction.DOWN, BlockTags.BASE_STONE_OVERWORLD),
				BlockPredicate.matchesBlocks(Direction.DOWN, Blocks.GRAVEL)
		);
	}

	/** Like {@link #freeSpot()}, plus snow layers so snowy forests get sticks too; the stick takes the cover's place. */
	private static BlockPredicate stickSpot() {
		return BlockPredicate.anyOf(freeSpot(), BlockPredicate.matchesBlocks(Blocks.SNOW));
	}

	/** Any block with a solid top underneath except ice; water has none, so sticks never float. */
	private static BlockPredicate solidDryGround() {
		return BlockPredicate.allOf(
				BlockPredicate.hasSturdyFace(Direction.DOWN, Direction.UP),
				BlockPredicate.not(BlockPredicate.matchesTag(Direction.DOWN, BlockTags.ICE)),
				BlockPredicate.not(BlockPredicate.matchesFluids(Fluids.WATER)));
	}

	/** A log within {@link #TREE_RADIUS} blocks sideways, from the stick's level up to {@link #TREE_TRUNK_HEIGHT} blocks. */
	private static BlockPredicate nearTree() {
		BlockPredicate noLogs = BlockPredicate.volumeMatch(
				new Vec3i(-TREE_RADIUS, 0, -TREE_RADIUS),
				new Vec3i(TREE_RADIUS, TREE_TRUNK_HEIGHT, TREE_RADIUS),
				BlockPredicate.not(BlockPredicate.matchesTag(BlockTags.LOGS)));
		return BlockPredicate.not(noLogs);
	}

	@Override
	protected void configure(HolderLookup.Provider registries, Entries entries) {
		var features = registries.lookupOrThrow(Registries.FEATURE);
		entries.add(features, EamWorldgen.PEBBLE);
		entries.add(features, EamWorldgen.FLINT_SHARD);
		entries.add(features, EamWorldgen.GROUND_STICK);
		entries.add(features, EamWorldgen.ORE_COPPER);
		entries.add(features, EamWorldgen.ORE_SILVER);
		entries.add(features, EamWorldgen.ORE_MITHRIL);
		entries.add(features, EamWorldgen.ORE_ADAMANTIUM);
		entries.add(features, EamWorldgen.WILD_GARLIC);
		var placedFeatures = registries.lookupOrThrow(Registries.PLACED_FEATURE);
		entries.add(placedFeatures, EamWorldgen.PEBBLE_FOREST);
		entries.add(placedFeatures, EamWorldgen.FLINT_SHARD_FOREST);
		entries.add(placedFeatures, EamWorldgen.PEBBLE_MOUNTAIN);
		entries.add(placedFeatures, EamWorldgen.FLINT_SHARD_MOUNTAIN);
		entries.add(placedFeatures, EamWorldgen.PEBBLE_SHORE);
		entries.add(placedFeatures, EamWorldgen.FLINT_SHARD_SHORE);
		entries.add(placedFeatures, EamWorldgen.GROUND_STICK_NEAR_TREES);
		entries.add(placedFeatures, EamWorldgen.WILD_GARLIC_PATCH);
		entries.add(placedFeatures, EamWorldgen.ORE_COPPER_PLACED);
		entries.add(placedFeatures, EamWorldgen.ORE_SILVER_PLACED);
		entries.add(placedFeatures, EamWorldgen.ORE_SILVER_MOUNTAIN);
		entries.add(placedFeatures, EamWorldgen.ORE_MITHRIL_PLACED);
		entries.add(placedFeatures, EamWorldgen.ORE_MITHRIL_RICH);
		SwampyForestWorldgen.configure(registries, entries);
		UnderworldWorldgen.configure(registries, entries);
	}

	@Override
	public String getName() {
		return "EAM worldgen";
	}
}
