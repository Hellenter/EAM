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
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.SimpleBlockFeature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.RandomizedIntStateProvider;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RarityFilter;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Pebbles and flint shards on the surface. Flint shards are 8 times rarer than pebbles: in forests and
 * mountains a pebble tries to spawn once every 2 chunks and a shard once every 16; next to surface gravel
 * a pebble tries twice per chunk and a shard once every 4 chunks.
 */
public class EamWorldgenProvider extends FabricDynamicRegistryProvider {
	/** Within this many blocks sideways of surface gravel counts as "next to gravel". */
	private static final int GRAVEL_RADIUS = 2;

	public EamWorldgenProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	public static void bootstrapFeatures(BootstrapContext<Feature> context) {
		context.register(EamWorldgen.PEBBLE, groundItem(EamBlocks.PEBBLE));
		context.register(EamWorldgen.FLINT_SHARD, groundItem(EamBlocks.FLINT_SHARD));
	}

	public static void bootstrapPlacedFeatures(BootstrapContext<PlacedFeature> context) {
		HolderGetter<Feature> features = context.lookup(Registries.FEATURE);
		var pebble = features.getOrThrow(EamWorldgen.PEBBLE);
		var flintShard = features.getOrThrow(EamWorldgen.FLINT_SHARD);

		PlacementUtils.register(context, EamWorldgen.PEBBLE_SCATTERED, pebble,
				onSurface(RarityFilter.onAverageOnceEvery(2), naturalGround()));
		PlacementUtils.register(context, EamWorldgen.FLINT_SHARD_SCATTERED, flintShard,
				onSurface(RarityFilter.onAverageOnceEvery(16), naturalGround()));
		PlacementUtils.register(context, EamWorldgen.PEBBLE_NEAR_GRAVEL, pebble,
				onSurface(CountPlacement.of(2), nearGravel()));
		PlacementUtils.register(context, EamWorldgen.FLINT_SHARD_NEAR_GRAVEL, flintShard,
				onSurface(RarityFilter.onAverageOnceEvery(4), nearGravel()));
	}

	private static Feature groundItem(Block block) {
		return new SimpleBlockFeature(new RandomizedIntStateProvider(
				BlockStateProvider.of(block), GroundItemBlock.POSITION, UniformInt.of(0, GroundItemLogic.POSITIONS - 1)));
	}

	private static List<PlacementModifier> onSurface(PlacementModifier frequency, BlockPredicate ground) {
		return List.of(
				frequency,
				InSquarePlacement.spread(),
				PlacementUtils.HEIGHTMAP_NO_LEAVES,
				BiomeFilter.biome(),
				BlockPredicateFilter.forPredicate(BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE, ground))
		);
	}

	/** Dirt, grass, stone or gravel underneath. */
	private static BlockPredicate naturalGround() {
		return BlockPredicate.anyOf(
				BlockPredicate.matchesTag(Direction.DOWN, BlockTags.DIRT),
				BlockPredicate.matchesTag(Direction.DOWN, BlockTags.BASE_STONE_OVERWORLD),
				BlockPredicate.matchesBlocks(Direction.DOWN, Blocks.GRAVEL)
		);
	}

	/** Natural ground with gravel within {@link #GRAVEL_RADIUS} blocks around and below. */
	private static BlockPredicate nearGravel() {
		BlockPredicate noGravel = BlockPredicate.volumeMatch(
				new Vec3i(-GRAVEL_RADIUS, -2, -GRAVEL_RADIUS),
				new Vec3i(GRAVEL_RADIUS, -1, GRAVEL_RADIUS),
				BlockPredicate.not(BlockPredicate.matchesBlocks(Blocks.GRAVEL)));
		return BlockPredicate.allOf(naturalGround(), BlockPredicate.not(noGravel));
	}

	@Override
	protected void configure(HolderLookup.Provider registries, Entries entries) {
		var features = registries.lookupOrThrow(Registries.FEATURE);
		entries.add(features, EamWorldgen.PEBBLE);
		entries.add(features, EamWorldgen.FLINT_SHARD);
		var placedFeatures = registries.lookupOrThrow(Registries.PLACED_FEATURE);
		entries.add(placedFeatures, EamWorldgen.PEBBLE_SCATTERED);
		entries.add(placedFeatures, EamWorldgen.FLINT_SHARD_SCATTERED);
		entries.add(placedFeatures, EamWorldgen.PEBBLE_NEAR_GRAVEL);
		entries.add(placedFeatures, EamWorldgen.FLINT_SHARD_NEAR_GRAVEL);
	}

	@Override
	public String getName() {
		return "EAM worldgen";
	}
}
