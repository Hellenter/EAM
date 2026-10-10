package io.github.hellenter.eam.content.swamp;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jspecify.annotations.Nullable;

/**
 * A dense swamp shrub: a ball of leaves on twig legs, like an azalea. A second one placed on top is all leaves, so
 * thickets grow two blocks tall. Nothing blocks the way, but pushing through the twigs is like wading through a weak
 * cobweb: half speed.
 */
public class DenseShrubBlock extends VegetationBlock {
	/** The upper block of a two-block shrub: leaves only, standing on a lower shrub. */
	public static final BooleanProperty TOP = BooleanProperty.create("top");
	/** Cobweb is 0.25 across and 0.05 up and down; the shrub only halves walking and slows falls and jumps less. */
	private static final Vec3 SLOWDOWN = new Vec3(0.5, 0.75, 0.5);

	public DenseShrubBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(TOP, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(TOP);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState below = context.getLevel().getBlockState(context.getClickedPos().below());
		return this.defaultBlockState().setValue(TOP, below.is(this) && !below.getValue(TOP));
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		if (state.getValue(TOP)) {
			BlockState below = level.getBlockState(pos.below());
			return below.is(this) && !below.getValue(TOP);
		}
		return super.canSurvive(state, level, pos);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.block();
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier,
			boolean isPrecise) {
		entity.makeStuckInBlock(state, SLOWDOWN);
	}
}
