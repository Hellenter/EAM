package io.github.hellenter.eam.content.swamp;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A dense swamp shrub: a ball of leaves, tinted like the trees' leaves, with tufts sticking out, on twig legs. Nothing
 * blocks the way, but pushing through the twigs is like wading through a weak cobweb: half speed.
 */
public class DenseShrubBlock extends VegetationBlock {
	/** Cobweb is 0.25 across and 0.05 up and down; the shrub only halves walking and slows falls and jumps less. */
	private static final Vec3 SLOWDOWN = new Vec3(0.5, 0.75, 0.5);
	private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 14.0, 15.0);

	public DenseShrubBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier,
			boolean isPrecise) {
		entity.makeStuckInBlock(state, SLOWDOWN);
	}
}
