package io.github.hellenter.eam.dimension.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A small boulder on a cave floor: a few overlapping lumps like vanilla's forest rock, each block picked from
 * {@code stone} so it can mix, say, cobblestone and mossy cobblestone. It sinks a block into the floor so it never
 * hangs, needs room above it, and fills only air, plants and the floor under it. {@code top} (moss carpet, snow) now and
 * then lies on its upper blocks.
 *
 * @param lumps     how many lumps, each shifted a little from the last
 * @param radius    radius of each lump
 * @param topChance chance that an upper block gets {@code top}
 */
public record CaveBoulderFeature(Holder<BlockStateProvider> stone, IntProvider lumps, IntProvider radius,
		Optional<Holder<BlockStateProvider>> top, float topChance) implements Feature {
	public static final MapCodec<CaveBoulderFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			BlockStateProvider.CODEC.fieldOf("stone").forGetter(CaveBoulderFeature::stone),
			IntProviders.codec(1, 6).fieldOf("lumps").forGetter(CaveBoulderFeature::lumps),
			IntProviders.codec(1, 3).fieldOf("radius").forGetter(CaveBoulderFeature::radius),
			BlockStateProvider.CODEC.optionalFieldOf("top").forGetter(CaveBoulderFeature::top),
			Codec.floatRange(0.0F, 1.0F).optionalFieldOf("top_chance", 0.0F).forGetter(CaveBoulderFeature::topChance)
	).apply(i, CaveBoulderFeature::new));

	@Override
	public MapCodec<CaveBoulderFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
		if (!level.getBlockState(origin.below()).isSolid()) {
			return false;
		}
		int lumps = this.lumps.sample(random);
		int radius = this.radius.sample(random);
		// Room for the whole boulder: a boulder jammed against a low ceiling looks like part of the wall.
		for (int y = 0; y <= radius + 1; y++) {
			if (!open(level.getBlockState(origin.above(y)))) {
				return false;
			}
		}
		List<BlockPos> placed = new ArrayList<>();
		BlockPos centre = origin.below();
		for (int n = 0; n < lumps; n++) {
			float r = radius * (0.6F + random.nextFloat() * 0.5F) + 0.5F;
			int reach = (int) Math.ceil(r);
			for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-reach, -reach, -reach), centre.offset(reach, reach, reach))) {
				if (pos.distSqr(centre) > r * r) {
					continue;
				}
				BlockState there = level.getBlockState(pos);
				boolean fill = open(there) || pos.getY() < origin.getY() && there.isSolid() && !there.hasBlockEntity()
						&& !there.is(BlockTags.FEATURES_CANNOT_REPLACE);
				if (fill) {
					level.setBlock(pos, this.stone.value().getState(level, random, pos), Block.UPDATE_CLIENTS);
					placed.add(pos.immutable());
				}
			}
			// The next lump beside this one and never higher, so the boulder is wider than tall.
			centre = centre.offset(random.nextInt(3) - 1, -random.nextInt(2), random.nextInt(3) - 1);
		}
		if (placed.isEmpty()) {
			return false;
		}
		// A tall plant whose lower half is now rock would float.
		for (BlockPos pos : placed) {
			if (level.getBlockState(pos.above()).getBlock() instanceof DoublePlantBlock) {
				level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
		}
		if (this.top.isPresent() && this.topChance > 0.0F) {
			for (BlockPos pos : placed) {
				BlockPos above = pos.above();
				if (random.nextFloat() < this.topChance && level.isEmptyBlock(above)) {
					BlockState cover = this.top.get().value().getState(level, random, above);
					if (cover.canSurvive(level, above)) {
						level.setBlock(above, cover, Block.UPDATE_CLIENTS);
					}
				}
			}
		}
		return true;
	}

	/** Air or a small plant the boulder may take the place of; never a fluid. */
	private static boolean open(BlockState state) {
		return state.isAir() || state.canBeReplaced() && state.getFluidState().isEmpty();
	}
}
