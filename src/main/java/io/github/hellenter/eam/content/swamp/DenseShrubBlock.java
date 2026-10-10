package io.github.hellenter.eam.content.swamp;

import io.github.hellenter.eam.content.EamItems;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A small dense swamp shrub: a ball of leaves, tinted like the trees' leaves, with tufts sticking out, on twig legs.
 * Some bear cranberries ({@link BerryShrub}). Nothing blocks the way, but pushing through the twigs is like wading
 * through a weak cobweb: half speed.
 */
public class DenseShrubBlock extends VegetationBlock {
	/** Cobweb is 0.25 across and 0.05 up and down; the shrub only halves walking and slows falls and jumps less. */
	public static final Vec3 SLOWDOWN = new Vec3(0.5, 0.75, 0.5);
	private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);

	public DenseShrubBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(BerryShrub.BERRIES, BerryShrub.NONE));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(BerryShrub.BERRIES);
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

	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		return BerryShrub.isRandomlyTicking(state);
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		BerryShrub.randomTick(state, level, pos, random);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		InteractionResult result = BerryShrub.pick(state, level, pos, player, () -> EamItems.CRANBERRY, 1, 3);
		return result == InteractionResult.PASS ? super.useWithoutItem(state, level, pos, player, hitResult) : result;
	}
}
