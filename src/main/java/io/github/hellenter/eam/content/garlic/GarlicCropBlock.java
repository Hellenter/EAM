package io.github.hellenter.eam.content.garlic;

import io.github.hellenter.eam.content.EamItems;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Garlic on farmland: planted from a clove bulb like a carrot, eight stages, harvested for more bulbs. Being a
 * {@link CropBlock}, it grows as slowly as every other crop under the farming rules.
 */
public class GarlicCropBlock extends CropBlock {
	private static final VoxelShape[] SHAPES = Block.boxes(7, age -> Block.column(16.0, 0.0, 2 + age));

	public GarlicCropBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	protected ItemLike getBaseSeedId() {
		return EamItems.GARLIC;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES[getAge(state)];
	}
}
