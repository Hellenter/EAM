package io.github.hellenter.eam.world.swamp;

import io.github.hellenter.eam.content.swamp.HollowLogBlock;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.codec.RegistryCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.List;

/**
 * A fallen hollow log lying along x or z: on the ground on land, or floating in a pond, its blocks taking the place of
 * the top water layer and holding the water. Every block must rest on ground or water; otherwise the log is shorter
 * or not placed.
 *
 * @param logs        the hollow logs to pick one from
 * @param length      how many blocks long
 * @param waterChance chance that a log whose spot is on water is placed at all, so logs float more rarely than they lie
 */
public record FallenLogFeature(HolderSet<Block> logs, IntProvider length, float waterChance) implements Feature {
	public static final MapCodec<FallenLogFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			RegistryCodecs.holderSet(Registries.BLOCK).fieldOf("logs").forGetter(FallenLogFeature::logs),
			IntProviders.codec(2, 8).fieldOf("length").forGetter(FallenLogFeature::length),
			Codec.floatRange(0.0F, 1.0F).optionalFieldOf("water_chance", 1.0F).forGetter(FallenLogFeature::waterChance)
	).apply(i, FallenLogFeature::new));

	@Override
	public MapCodec<FallenLogFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
		Block log = this.logs.getRandomElement(random).map(Holder::value).orElse(null);
		if (log == null) {
			return false;
		}
		Direction along = random.nextBoolean() ? Direction.EAST : Direction.SOUTH;
		int length = this.length.sample(random);
		int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, origin.getX(), origin.getZ());
		BlockPos start = new BlockPos(origin.getX(), surface, origin.getZ());
		boolean inWater = level.getFluidState(start.below()).isSourceOfType(Fluids.WATER);
		if (inWater && random.nextFloat() >= this.waterChance) {
			return false;
		}
		// In water the log takes the top water block's place; on land it lies on the ground.
		int y = inWater ? surface - 1 : surface;
		List<BlockPos> spots = new ArrayList<>();
		for (int n = 0; n < length; n++) {
			BlockPos spot = new BlockPos(origin.getX(), y, origin.getZ()).relative(along, n);
			BlockState there = level.getBlockState(spot);
			boolean fits = inWater
					? there.getFluidState().isSourceOfType(Fluids.WATER) && there.is(Blocks.WATER)
							&& level.isEmptyBlock(spot.above())
					: (there.isAir() || there.canBeReplaced()) && there.getFluidState().isEmpty()
							&& level.getBlockState(spot.below()).isFaceSturdy(level, spot.below(), Direction.UP);
			if (!fits) {
				break;
			}
			spots.add(spot);
		}
		if (spots.size() < 2) {
			return false;
		}
		BlockState state = log.defaultBlockState().setValue(RotatedPillarBlock.AXIS, along.getAxis())
				.setValue(HollowLogBlock.WATERLOGGED, inWater);
		for (BlockPos spot : spots) {
			level.setBlock(spot, state, 2);
		}
		return true;
	}
}
