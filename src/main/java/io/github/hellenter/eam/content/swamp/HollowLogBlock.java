package io.github.hellenter.eam.content.swamp;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * A hollow log: only the bark is left, the core has rotted away. Lies along any axis like a log, two pixels of wall
 * around an empty middle you can crawl through, and holds water when it lies in it.
 */
public class HollowLogBlock extends RotatedPillarBlock implements SimpleWaterloggedBlock {
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
	/** Wall thickness in pixels. */
	public static final int WALL = 2;
	private static final Map<Direction.Axis, VoxelShape> SHAPES = Map.of(
			Direction.Axis.Y, Shapes.or(Block.box(0, 0, 0, 16, 16, WALL), Block.box(0, 0, 16 - WALL, 16, 16, 16),
					Block.box(0, 0, 0, WALL, 16, 16), Block.box(16 - WALL, 0, 0, 16, 16, 16)),
			Direction.Axis.X, Shapes.or(Block.box(0, 0, 0, 16, WALL, 16), Block.box(0, 16 - WALL, 0, 16, 16, 16),
					Block.box(0, 0, 0, 16, 16, WALL), Block.box(0, 0, 16 - WALL, 16, 16, 16)),
			Direction.Axis.Z, Shapes.or(Block.box(0, 0, 0, 16, WALL, 16), Block.box(0, 16 - WALL, 0, 16, 16, 16),
					Block.box(0, 0, 0, WALL, 16, 16), Block.box(16 - WALL, 0, 0, 16, 16, 16)));

	public HollowLogBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.defaultBlockState().setValue(WATERLOGGED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(WATERLOGGED);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = super.getStateForPlacement(context);
		return state == null ? null : state.setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).is(Fluids.WATER));
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour,
			BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
		if (state.getValue(WATERLOGGED)) {
			ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}
		return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(AXIS));
	}

	@Override
	protected boolean useShapeForLightOcclusion(BlockState state) {
		return true;
	}
}
