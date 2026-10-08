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
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.minecraft.world.level.levelgen.structure.templatesystem.HeightMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Pebbles, flint shards and sticks on the surface. In forests a pebble gets 0.8 tries per chunk on average
 * (4 tries, each kept 1 time in 5) and a flint shard 16 times fewer; in the mountains they lie only on stone
 * and gravel, with flint shards 8 times rarer. Sticks lie under tree crowns, 3 tries per chunk in any
 * overworld biome.
 * <p>
 * Copper ore: 12 veins per chunk instead of vanilla's 16, at vanilla heights, each the size of an average diamond vein
 * (vanilla diamond veins are size 4, 8 and 12, about 6 on average) instead of 10, or 20 in dripstone caves.
 */
public class EamWorldgenProvider extends FabricDynamicRegistryProvider {
	/** How far above the ground a tree crown counts for a stick under it. */
	private static final int CROWN_HEIGHT = 12;
	private static final int COPPER_VEIN_SIZE = 6;
	private static final int COPPER_VEINS_PER_CHUNK = 12;

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
	}

	public static void bootstrapPlacedFeatures(BootstrapContext<PlacedFeature> context) {
		HolderGetter<Feature> features = context.lookup(Registries.FEATURE);
		var pebble = features.getOrThrow(EamWorldgen.PEBBLE);
		var flintShard = features.getOrThrow(EamWorldgen.FLINT_SHARD);
		var stick = features.getOrThrow(EamWorldgen.GROUND_STICK);
		var copper = features.getOrThrow(EamWorldgen.ORE_COPPER);

		PlacementUtils.register(context, EamWorldgen.PEBBLE_FOREST, pebble,
				onSurface(forestFloor(), CountPlacement.of(4), RarityFilter.onAverageOnceEvery(5)));
		PlacementUtils.register(context, EamWorldgen.FLINT_SHARD_FOREST, flintShard,
				onSurface(forestFloor(), RarityFilter.onAverageOnceEvery(20)));
		PlacementUtils.register(context, EamWorldgen.PEBBLE_MOUNTAIN, pebble,
				onSurface(stoneOrGravel(), CountPlacement.of(4), RarityFilter.onAverageOnceEvery(5)));
		PlacementUtils.register(context, EamWorldgen.FLINT_SHARD_MOUNTAIN, flintShard,
				onSurface(stoneOrGravel(), RarityFilter.onAverageOnceEvery(10)));
		PlacementUtils.register(context, EamWorldgen.GROUND_STICK_UNDER_TREES, stick,
				onSurface(BlockPredicate.allOf(forestFloor(), underLeaves()), CountPlacement.of(3)));
		PlacementUtils.register(context, EamWorldgen.ORE_COPPER_PLACED, copper, List.of(
				CountPlacement.of(COPPER_VEINS_PER_CHUNK),
				InSquarePlacement.spread(),
				HeightRangePlacement.triangle(VerticalAnchor.absolute(-16), VerticalAnchor.absolute(112)),
				BiomeFilter.biome()));
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

	/** Leaves somewhere in the column above, up to {@link #CROWN_HEIGHT} blocks. */
	private static BlockPredicate underLeaves() {
		BlockPredicate noLeaves = BlockPredicate.volumeMatch(
				new Vec3i(0, 1, 0),
				new Vec3i(0, CROWN_HEIGHT, 0),
				BlockPredicate.not(BlockPredicate.matchesTag(BlockTags.LEAVES)));
		return BlockPredicate.not(noLeaves);
	}

	@Override
	protected void configure(HolderLookup.Provider registries, Entries entries) {
		var features = registries.lookupOrThrow(Registries.FEATURE);
		entries.add(features, EamWorldgen.PEBBLE);
		entries.add(features, EamWorldgen.FLINT_SHARD);
		entries.add(features, EamWorldgen.GROUND_STICK);
		entries.add(features, EamWorldgen.ORE_COPPER);
		var placedFeatures = registries.lookupOrThrow(Registries.PLACED_FEATURE);
		entries.add(placedFeatures, EamWorldgen.PEBBLE_FOREST);
		entries.add(placedFeatures, EamWorldgen.FLINT_SHARD_FOREST);
		entries.add(placedFeatures, EamWorldgen.PEBBLE_MOUNTAIN);
		entries.add(placedFeatures, EamWorldgen.FLINT_SHARD_MOUNTAIN);
		entries.add(placedFeatures, EamWorldgen.GROUND_STICK_UNDER_TREES);
		entries.add(placedFeatures, EamWorldgen.ORE_COPPER_PLACED);
	}

	@Override
	public String getName() {
		return "EAM worldgen";
	}
}
