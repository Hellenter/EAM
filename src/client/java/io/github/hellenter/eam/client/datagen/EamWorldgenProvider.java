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
 * Pebbles, flint shards and sticks on the surface. In forests a pebble tries to spawn twice per chunk and
 * a flint shard 16 times less often; in the mountains they lie only on stone and gravel, with flint shards
 * 8 times rarer. Sticks lie under tree crowns, 12 tries per chunk in any overworld biome.
 */
public class EamWorldgenProvider extends FabricDynamicRegistryProvider {
	/** How far above the ground a tree crown counts for a stick under it. */
	private static final int CROWN_HEIGHT = 12;

	public EamWorldgenProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	public static void bootstrapFeatures(BootstrapContext<Feature> context) {
		context.register(EamWorldgen.PEBBLE, groundItem(EamBlocks.PEBBLE));
		context.register(EamWorldgen.FLINT_SHARD, groundItem(EamBlocks.FLINT_SHARD));
		context.register(EamWorldgen.GROUND_STICK, groundItem(EamBlocks.GROUND_STICK));
	}

	public static void bootstrapPlacedFeatures(BootstrapContext<PlacedFeature> context) {
		HolderGetter<Feature> features = context.lookup(Registries.FEATURE);
		var pebble = features.getOrThrow(EamWorldgen.PEBBLE);
		var flintShard = features.getOrThrow(EamWorldgen.FLINT_SHARD);
		var stick = features.getOrThrow(EamWorldgen.GROUND_STICK);

		PlacementUtils.register(context, EamWorldgen.PEBBLE_FOREST, pebble,
				onSurface(CountPlacement.of(2), forestFloor()));
		PlacementUtils.register(context, EamWorldgen.FLINT_SHARD_FOREST, flintShard,
				onSurface(RarityFilter.onAverageOnceEvery(8), forestFloor()));
		PlacementUtils.register(context, EamWorldgen.PEBBLE_MOUNTAIN, pebble,
				onSurface(CountPlacement.of(2), stoneOrGravel()));
		PlacementUtils.register(context, EamWorldgen.FLINT_SHARD_MOUNTAIN, flintShard,
				onSurface(RarityFilter.onAverageOnceEvery(4), stoneOrGravel()));
		PlacementUtils.register(context, EamWorldgen.GROUND_STICK_UNDER_TREES, stick,
				onSurface(CountPlacement.of(12), BlockPredicate.allOf(forestFloor(), underLeaves())));
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
		var placedFeatures = registries.lookupOrThrow(Registries.PLACED_FEATURE);
		entries.add(placedFeatures, EamWorldgen.PEBBLE_FOREST);
		entries.add(placedFeatures, EamWorldgen.FLINT_SHARD_FOREST);
		entries.add(placedFeatures, EamWorldgen.PEBBLE_MOUNTAIN);
		entries.add(placedFeatures, EamWorldgen.FLINT_SHARD_MOUNTAIN);
		entries.add(placedFeatures, EamWorldgen.GROUND_STICK_UNDER_TREES);
	}

	@Override
	public String getName() {
		return "EAM worldgen";
	}
}
