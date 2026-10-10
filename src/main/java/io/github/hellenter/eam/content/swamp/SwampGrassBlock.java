package io.github.hellenter.eam.content.swamp;

import io.github.hellenter.eam.content.EamBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** Swamp grass: like short grass, but bone meal grows it into a tall swamp grass tussock. */
public class SwampGrassBlock extends TallGrassBlock {
	public SwampGrassBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
		return EamBlocks.TALL_SWAMP_GRASS.defaultBlockState().canSurvive(level, pos) && level.isEmptyBlock(pos.above())
				&& level.isInsideBuildHeight(pos.above());
	}

	@Override
	public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
		DoublePlantBlock.placeAt(level, EamBlocks.TALL_SWAMP_GRASS.defaultBlockState(), pos, 2);
	}
}
