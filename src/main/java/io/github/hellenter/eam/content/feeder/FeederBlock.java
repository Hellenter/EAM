package io.github.hellenter.eam.content.feeder;

import io.github.hellenter.eam.content.EamBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jspecify.annotations.Nullable;

/**
 * A feeder: a wooden trough under a hay rack. Feeders placed in a row join into one trough up to
 * {@link FeederLogic#MAX_LENGTH} long. Each block holds its own feed and feeds {@link FeederSettings#headsPerFeeder()}
 * heads a day, see {@link FeederBlockEntity}. Feed goes in by hand or from farmer villagers nearby.
 */
public class FeederBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
	/** Joined to the feeder on the negative side of {@link #AXIS} (west or north). */
	public static final BooleanProperty NEGATIVE = BooleanProperty.create("negative");
	/** Joined to the feeder on the positive side of {@link #AXIS} (east or south). */
	public static final BooleanProperty POSITIVE = BooleanProperty.create("positive");
	/** How full the trough looks, see {@link FeederLogic#fill}. */
	public static final IntegerProperty FILL = IntegerProperty.create("fill", 0, FeederLogic.MAX_FILL);

	private static final VoxelShape SHAPE_X = Block.box(0, 0, 1, 16, 14, 15);
	private static final VoxelShape SHAPE_Z = Block.box(1, 0, 0, 15, 14, 16);

	public FeederBlock(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X).setValue(NEGATIVE, false)
				.setValue(POSITIVE, false).setValue(FILL, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AXIS, NEGATIVE, POSITIVE, FILL);
	}

	/** Lies across the player's view, unless it can continue a trough beside it along the other axis. */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		Direction.Axis across = context.getHorizontalDirection().getClockWise().getAxis();
		Direction.Axis along = across == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
		for (Direction.Axis axis : new Direction.Axis[] {across, along}) {
			boolean[] join = FeederLogic.join(run(level, pos, axis, Direction.AxisDirection.NEGATIVE),
					run(level, pos, axis, Direction.AxisDirection.POSITIVE));
			if (join[0] || join[1]) {
				return defaultBlockState().setValue(AXIS, axis).setValue(NEGATIVE, join[0]).setValue(POSITIVE, join[1]);
			}
		}
		return defaultBlockState().setValue(AXIS, across);
	}

	/** Feeders in a row on one side of {@code pos}, joined to each other and lying along {@code axis}. */
	private int run(BlockGetter level, BlockPos pos, Direction.Axis axis, Direction.AxisDirection side) {
		Direction step = Direction.fromAxisAndDirection(axis, side);
		BooleanProperty further = side == Direction.AxisDirection.NEGATIVE ? NEGATIVE : POSITIVE;
		int count = 0;
		BlockPos cursor = pos.relative(step);
		while (count < FeederLogic.MAX_LENGTH) {
			BlockState state = level.getBlockState(cursor);
			if (!state.is(this) || state.getValue(AXIS) != axis) break;
			count++;
			if (!state.getValue(further)) break;
			cursor = cursor.relative(step);
		}
		return count;
	}

	/** The whole trough this feeder belongs to, in blocks. */
	public int length(BlockGetter level, BlockPos pos, BlockState state) {
		Direction.Axis axis = state.getValue(AXIS);
		int length = 1;
		if (state.getValue(NEGATIVE)) length += run(level, pos, axis, Direction.AxisDirection.NEGATIVE);
		if (state.getValue(POSITIVE)) length += run(level, pos, axis, Direction.AxisDirection.POSITIVE);
		return length;
	}

	/** A side stays joined while the feeder there lies the same way and is joined back. */
	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
			Direction direction, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
		if (direction.getAxis() != state.getValue(AXIS)) return state;
		boolean negative = direction.getAxisDirection() == Direction.AxisDirection.NEGATIVE;
		BooleanProperty side = negative ? NEGATIVE : POSITIVE;
		BooleanProperty back = negative ? POSITIVE : NEGATIVE;
		boolean joined = neighbourState.is(this) && neighbourState.getValue(AXIS) == state.getValue(AXIS) && neighbourState.getValue(back);
		return state.setValue(side, joined);
	}

	/** Turning a quarter swaps the axis; a side ends up reversed when the turn sends it the other way along the new axis. */
	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		boolean alongX = state.getValue(AXIS) == Direction.Axis.X;
		return switch (rotation) {
			case CLOCKWISE_90 -> turn(state, !alongX);
			case COUNTERCLOCKWISE_90 -> turn(state, alongX);
			case CLOCKWISE_180 -> swapSides(state);
			default -> state;
		};
	}

	private static BlockState turn(BlockState state, boolean swap) {
		BlockState turned = state.setValue(AXIS, state.getValue(AXIS) == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
		return swap ? swapSides(turned) : turned;
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		Direction.Axis axis = state.getValue(AXIS);
		boolean flips = mirror == Mirror.LEFT_RIGHT && axis == Direction.Axis.Z || mirror == Mirror.FRONT_BACK && axis == Direction.Axis.X;
		return flips ? swapSides(state) : state;
	}

	private static BlockState swapSides(BlockState state) {
		return state.setValue(NEGATIVE, state.getValue(POSITIVE)).setValue(POSITIVE, state.getValue(NEGATIVE));
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hitResult) {
		if (!stack.is(FeederModule.FEED)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!(level.getBlockEntity(pos) instanceof FeederBlockEntity feeder) || feeder.isFull()) {
			return InteractionResult.FAIL;
		}
		if (!level.isClientSide()) {
			int added = feeder.add(stack.getItem(), stack.getCount());
			stack.consume(added, player);
		}
		return InteractionResult.SUCCESS;
	}

	/** With an empty hand: how much feed is in it and how many have eaten today. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (level instanceof ServerLevel serverLevel && level.getBlockEntity(pos) instanceof FeederBlockEntity feeder) {
			FeederSettings settings = FeederModule.settings();
			player.sendOverlayMessage(Component.translatable("block.eam.feeder.status", feeder.food(), settings.foodCapacity(),
					feeder.mealsToday(serverLevel), settings.headsPerFeeder()));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		if (!(level.getBlockEntity(pos) instanceof FeederBlockEntity feeder)) return 0;
		return (int) Math.ceil(feeder.food() * 15.0 / FeederModule.settings().foodCapacity());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(AXIS) == Direction.Axis.X ? SHAPE_X : SHAPE_Z;
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType type) {
		return false;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new FeederBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, EamBlockEntities.FEEDER, FeederBlockEntity::serverTick);
	}
}
