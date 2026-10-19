package io.github.hellenter.eam.content.swamp;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * A shelf mushroom (bracket fungus) growing out of the side of a log, like cocoa: {@link #FACING} points to the log.
 * Falls off when the log is gone. Placing another on it adds a second, smaller shelf above ({@link #MUSHROOMS}), like
 * sea pickles; it drops one per shelf.
 */
public class ShelfMushroomBlock extends HorizontalDirectionalBlock {
	public static final int MAX_MUSHROOMS = 2;
	public static final IntegerProperty MUSHROOMS = IntegerProperty.create("mushrooms", 1, MAX_MUSHROOMS);
	/** One wide shelf against the log's face, for a log to the north; turned for the other sides. */
	private static final Map<Direction, VoxelShape> ONE = Shapes.rotateHorizontal(Block.box(2, 5, 0, 14, 8, 6));
	/** The wide shelf lower down and a smaller one above it. */
	private static final Map<Direction, VoxelShape> TWO = Shapes.rotateHorizontal(
			Shapes.or(Block.box(2, 3, 0, 14, 6, 6), Block.box(4, 9, 0, 11, 12, 5)));

	public ShelfMushroomBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(MUSHROOMS, 1));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, MUSHROOMS);
	}

	@Override
	protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
		return !context.isSecondaryUseActive() && context.getItemInHand().is(this.asItem()) && state.getValue(MUSHROOMS) < MAX_MUSHROOMS
				|| super.canBeReplaced(state, context);
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return level.getBlockState(pos.relative(state.getValue(FACING))).is(BlockTags.LOGS);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState existing = context.getLevel().getBlockState(context.getClickedPos());
		if (existing.is(this)) {
			return existing.setValue(MUSHROOMS, Math.min(MAX_MUSHROOMS, existing.getValue(MUSHROOMS) + 1));
		}
		for (Direction direction : context.getNearestLookingDirections()) {
			if (direction.getAxis().isHorizontal()) {
				BlockState state = this.defaultBlockState().setValue(FACING, direction);
				if (state.canSurvive(context.getLevel(), context.getClickedPos())) {
					return state;
				}
			}
		}
		return null;
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour,
			BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
		return directionToNeighbour == state.getValue(FACING) && !state.canSurvive(level, pos)
				? Blocks.AIR.defaultBlockState()
				: super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return (state.getValue(MUSHROOMS) == 1 ? ONE : TWO).get(state.getValue(FACING));
	}
}
