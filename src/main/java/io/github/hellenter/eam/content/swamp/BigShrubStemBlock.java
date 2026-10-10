package io.github.hellenter.eam.content.swamp;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The woody base of a big swamp shrub: a post about as thick as a fence post, holding up the leaves. */
public class BigShrubStemBlock extends Block {
	private static final VoxelShape SHAPE = Block.column(4.0, 0.0, 16.0);

	public BigShrubStemBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}
}
