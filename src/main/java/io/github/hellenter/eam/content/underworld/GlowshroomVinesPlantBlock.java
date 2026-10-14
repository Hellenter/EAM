package io.github.hellenter.eam.content.underworld;

import io.github.hellenter.eam.content.EamBlocks;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GrowingPlantBodyBlock;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A strand of glowshroom vines above its tip, {@link GlowshroomVinesBlock}. */
public class GlowshroomVinesPlantBlock extends GrowingPlantBodyBlock {
	private static final VoxelShape SHAPE = Block.column(14.0, 0.0, 16.0);

	public GlowshroomVinesPlantBlock(BlockBehaviour.Properties properties) {
		super(properties, Direction.DOWN, SHAPE, false);
	}

	@Override
	protected GrowingPlantHeadBlock getHeadBlock() {
		return (GrowingPlantHeadBlock) EamBlocks.GLOWSHROOM_VINES;
	}
}
