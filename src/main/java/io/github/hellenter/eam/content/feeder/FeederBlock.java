package io.github.hellenter.eam.content.feeder;

import io.github.hellenter.eam.content.EamBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
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

import java.util.ArrayList;
import java.util.List;

/**
 * A feeder: a trough on legs with a tall back wall and a low front lip. Feeders placed in a row join into one trough up to
 * {@link FeederLogic#MAX_LENGTH} long. Each block holds its own feed and feeds {@link FeederSettings#headsPerFeeder()}
 * heads a day, see {@link FeederBlockEntity}. Feed goes in through the trough's inventory, from hoppers or from farmer
 * villagers nearby; each block adds {@link FeederSettings#slotsPerFeeder()} slots. The metal feeder, a
 * galvanized sheet trough, has {@link FeederSettings#metalScale()} times as many slots and heads; metal and
 * wooden feeders do not join.
 */
public class FeederBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
	/** Joined to the feeder on the negative side of {@link #AXIS} (west or north). */
	public static final BooleanProperty NEGATIVE = BooleanProperty.create("negative");
	/** Joined to the feeder on the positive side of {@link #AXIS} (east or south). */
	public static final BooleanProperty POSITIVE = BooleanProperty.create("positive");
	/**
	 * The tall back wall stands on the positive side across the trough: south of one along X, east of one along Z.
	 * Joined feeders share it.
	 */
	public static final BooleanProperty BACK_POSITIVE = BooleanProperty.create("back_positive");
	/** How full the trough looks, see {@link FeederLogic#fill}. */
	public static final IntegerProperty FILL = IntegerProperty.create("fill", 0, FeederLogic.MAX_FILL);

	private static final VoxelShape SHAPE_X = Block.box(0, 0, 1, 16, 15, 15);
	private static final VoxelShape SHAPE_Z = Block.box(1, 0, 0, 15, 15, 16);

	private final boolean metal;

	public FeederBlock(boolean metal, BlockBehaviour.Properties properties) {
		super(properties);
		this.metal = metal;
		registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X).setValue(NEGATIVE, false)
				.setValue(POSITIVE, false).setValue(BACK_POSITIVE, false).setValue(FILL, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AXIS, NEGATIVE, POSITIVE, BACK_POSITIVE, FILL);
	}

	/** Meals one block of this feeder gives a day. */
	public int heads(FeederSettings settings) {
		return metal ? (int) Math.round(settings.headsPerFeeder() * settings.metalScale()) : settings.headsPerFeeder();
	}

	/** Inventory slots of one block of this feeder. */
	public int slots(FeederSettings settings) {
		return metal ? (int) Math.round(settings.slotsPerFeeder() * settings.metalScale()) : settings.slotsPerFeeder();
	}

	/**
	 * Lies across the player's view with its back wall away from them, unless it can continue a trough beside it along
	 * the other axis. A feeder joining a trough takes its back wall side.
	 */
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
				BlockState neighbour = level.getBlockState(pos.relative(Direction.fromAxisAndDirection(axis,
						join[0] ? Direction.AxisDirection.NEGATIVE : Direction.AxisDirection.POSITIVE)));
				return defaultBlockState().setValue(AXIS, axis).setValue(NEGATIVE, join[0]).setValue(POSITIVE, join[1])
						.setValue(BACK_POSITIVE, neighbour.getValue(BACK_POSITIVE));
			}
		}
		return withBack(defaultBlockState().setValue(AXIS, across), context.getHorizontalDirection());
	}

	/** The side across the trough its back wall stands on. */
	private static Direction back(BlockState state) {
		Direction.Axis across = state.getValue(AXIS) == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
		return Direction.fromAxisAndDirection(across,
				state.getValue(BACK_POSITIVE) ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
	}

	/** The back wall on side {@code back}, which lies across the trough. */
	private static BlockState withBack(BlockState state, Direction back) {
		return state.setValue(BACK_POSITIVE, back.getAxisDirection() == Direction.AxisDirection.POSITIVE);
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

	/** The blocks of the trough this feeder belongs to, from its negative end. */
	public List<BlockPos> trough(BlockGetter level, BlockPos pos, BlockState state) {
		Direction.Axis axis = state.getValue(AXIS);
		Direction back = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.NEGATIVE);
		BlockPos start = pos.relative(back, state.getValue(NEGATIVE) ? run(level, pos, axis, Direction.AxisDirection.NEGATIVE) : 0);
		List<BlockPos> blocks = new ArrayList<>();
		for (int i = 0; i < length(level, pos, state); i++) blocks.add(start.relative(back.getOpposite(), i));
		return blocks;
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
		BlockState turned = switch (rotation) {
			case CLOCKWISE_90 -> turn(state, !alongX);
			case COUNTERCLOCKWISE_90 -> turn(state, alongX);
			case CLOCKWISE_180 -> swapSides(state);
			default -> state;
		};
		return withBack(turned, rotation.rotate(back(state)));
	}

	private static BlockState turn(BlockState state, boolean swap) {
		BlockState turned = state.setValue(AXIS, state.getValue(AXIS) == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
		return swap ? swapSides(turned) : turned;
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		Direction.Axis axis = state.getValue(AXIS);
		boolean flips = mirror == Mirror.LEFT_RIGHT && axis == Direction.Axis.Z || mirror == Mirror.FRONT_BACK && axis == Direction.Axis.X;
		return withBack(flips ? swapSides(state) : state, mirror.mirror(back(state)));
	}

	private static BlockState swapSides(BlockState state) {
		return state.setValue(NEGATIVE, state.getValue(POSITIVE)).setValue(POSITIVE, state.getValue(NEGATIVE));
	}

	/** Opens the whole trough's inventory, like a chest. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (level instanceof ServerLevel serverLevel) {
			List<FeederBlockEntity> feeders = new ArrayList<>();
			for (BlockPos block : trough(level, pos, state)) {
				if (level.getBlockEntity(block) instanceof FeederBlockEntity feeder) feeders.add(feeder);
			}
			if (!feeders.isEmpty()) player.openMenu(new FeederMenu.Provider(getName(), serverLevel, feeders));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		return level.getBlockEntity(pos) instanceof FeederBlockEntity feeder
				? AbstractContainerMenu.getRedstoneSignalFromContainer(feeder) : 0;
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
