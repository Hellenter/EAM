package io.github.hellenter.eam.content.underworld;

import io.github.hellenter.eam.content.EamBlocks;

import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.NetherVines;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The glowing tip of glowshroom vines, the threads of a fungus hanging from the ceilings of the underground forest. It
 * grows down like weeping vines; {@link GlowshroomVinesPlantBlock} is the rest of the strand.
 */
public class GlowshroomVinesBlock extends GrowingPlantHeadBlock {
	private static final VoxelShape SHAPE = Block.column(8.0, 9.0, 16.0);

	public GlowshroomVinesBlock(BlockBehaviour.Properties properties) {
		super(properties, Direction.DOWN, SHAPE, false, 0.1);
	}

	@Override
	protected int getBlocksToGrowWhenBonemealed(RandomSource random) {
		return NetherVines.getBlocksToGrowWhenBonemealed(random);
	}

	@Override
	protected Block getBodyBlock() {
		return EamBlocks.GLOWSHROOM_VINES_PLANT;
	}

	@Override
	protected boolean canGrowInto(BlockState state) {
		return NetherVines.isValidGrowthState(state);
	}
}
