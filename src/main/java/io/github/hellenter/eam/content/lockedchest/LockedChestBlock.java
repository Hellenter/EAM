package io.github.hellenter.eam.content.lockedchest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * A chest with an iron lock: a villager's box for his things, or the village's store. See {@link LockedChestBlockEntity}
 * for who opens it. One a villager placed himself breaks into nothing but its items, so villagers do not make chests
 * for players. Placed beside one facing the same way that the same player placed, it joins it into a double chest, as a
 * vanilla chest does; sneaking places it alone.
 */
public class LockedChestBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final EnumProperty<ChestType> TYPE = BlockStateProperties.CHEST_TYPE;
	private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 14.0, 15.0);
	/** A half reaches to the block edge on its partner's side, keyed by that side. */
	private static final Map<Direction, VoxelShape> HALF_SHAPES = Shapes.rotateHorizontal(Block.boxZ(14.0, 0.0, 14.0, 0.0, 15.0));

	public LockedChestBlock(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TYPE, ChestType.SINGLE));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, TYPE);
	}

	/**
	 * The lock faces the player who places it. Unless he sneaks, it joins a lone locked chest of his beside it that
	 * faces the same way.
	 */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction facing = context.getHorizontalDirection().getOpposite();
		ChestType type = ChestType.SINGLE;
		Player player = context.getPlayer();
		if (player != null && !context.isSecondaryUseActive()) {
			if (joins(context.getLevel(), context.getClickedPos().relative(facing.getClockWise()), facing, player)) {
				type = ChestType.LEFT;
			} else if (joins(context.getLevel(), context.getClickedPos().relative(facing.getCounterClockWise()), facing, player)) {
				type = ChestType.RIGHT;
			}
		}
		return defaultBlockState().setValue(FACING, facing).setValue(TYPE, type);
	}

	/** Whether a lone locked chest at {@code pos} faces {@code facing} and is one {@code player} placed. */
	private boolean joins(Level level, BlockPos pos, Direction facing, Player player) {
		BlockState state = level.getBlockState(pos);
		return state.is(this) && state.getValue(TYPE) == ChestType.SINGLE && state.getValue(FACING) == facing
				&& level.getBlockEntity(pos) instanceof LockedChestBlockEntity chest && player.getUUID().equals(chest.placer());
	}

	/** Where the other half of a double chest is. */
	public static Direction connectedDirection(BlockState state) {
		Direction facing = state.getValue(FACING);
		return state.getValue(TYPE) == ChestType.LEFT ? facing.getClockWise() : facing.getCounterClockWise();
	}

	/** A half becomes the other half of a chest placed against it, and a lone chest again when its partner goes. */
	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour,
			BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
		if (neighbourState.is(this) && directionToNeighbour.getAxis().isHorizontal()) {
			ChestType neighbourType = neighbourState.getValue(TYPE);
			if (state.getValue(TYPE) == ChestType.SINGLE && neighbourType != ChestType.SINGLE
					&& state.getValue(FACING) == neighbourState.getValue(FACING)
					&& connectedDirection(neighbourState) == directionToNeighbour.getOpposite()) {
				return state.setValue(TYPE, neighbourType.getOpposite());
			}
		} else if (state.getValue(TYPE) != ChestType.SINGLE && connectedDirection(state) == directionToNeighbour) {
			return state.setValue(TYPE, ChestType.SINGLE);
		}
		return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		BlockState rotated = state.rotate(mirror.getRotation(state.getValue(FACING)));
		ChestType type = state.getValue(TYPE);
		return type == ChestType.SINGLE ? rotated : rotated.setValue(TYPE, type.getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(TYPE) == ChestType.SINGLE ? SHAPE : HALF_SHAPES.get(connectedDirection(state));
	}

	/** Records who placed it; a half placed onto another goes to whoever has the other. */
	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack itemStack) {
		if (!(level.getBlockEntity(pos) instanceof LockedChestBlockEntity chest)) return;
		if (by instanceof Player player) chest.setPlacer(player.getUUID());
		LockedChestBlockEntity partner = partner(level, pos, state);
		if (partner != null) chest.setOwner(partner.owner(), partner.village());
	}

	/** The other half of a double chest, or {@code null} for a lone chest. */
	public static @Nullable LockedChestBlockEntity partner(Level level, BlockPos pos, BlockState state) {
		if (state.getValue(TYPE) == ChestType.SINGLE) return null;
		BlockPos other = pos.relative(connectedDirection(state));
		BlockState otherState = level.getBlockState(other);
		if (!otherState.is(state.getBlock()) || otherState.getValue(TYPE) != state.getValue(TYPE).getOpposite()) return null;
		return level.getBlockEntity(other) instanceof LockedChestBlockEntity chest ? chest : null;
	}

	/** The chest's halves in slot order: the right half's rows come first, as in a vanilla double chest. */
	public static List<LockedChestBlockEntity> halves(Level level, BlockPos pos, BlockState state, LockedChestBlockEntity chest) {
		LockedChestBlockEntity partner = partner(level, pos, state);
		if (partner == null) return List.of(chest);
		return state.getValue(TYPE) == ChestType.RIGHT ? List.of(chest, partner) : List.of(partner, chest);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (level instanceof ServerLevel && level.getBlockEntity(pos) instanceof LockedChestBlockEntity chest) {
			List<LockedChestBlockEntity> halves = halves(level, pos, state, chest);
			if (halves.stream().allMatch(half -> half.opens(player))) {
				player.openMenu(new LockedChestMenuProvider(halves));
			} else {
				player.sendOverlayMessage(Component.translatable("container.eam.locked_chest.locked"));
				level.playSound(null, pos, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 1.0F, 1.0F);
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** A chest a villager made drops only what is in it. */
	@Override
	public void playerDestroy(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity,
			ItemStack destroyedWith) {
		if (blockEntity instanceof LockedChestBlockEntity chest && chest.villagerMade()) {
			player.awardStat(Stats.BLOCK_MINED.get(this));
			player.causeFoodExhaustion(0.005F);
			return;
		}
		super.playerDestroy(level, player, pos, state, blockEntity, destroyedWith);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new LockedChestBlockEntity(pos, state);
	}
}
