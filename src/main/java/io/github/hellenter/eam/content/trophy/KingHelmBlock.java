package io.github.hellenter.eam.content.trophy;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jspecify.annotations.Nullable;

/**
 * The Pillager King's helm, set up on a pike as a trophy. While {@link #ENABLED} (embers glow in its eye slits) no raid
 * starts around it (see {@code mobs.vanilla.raid.VillageRaidsModule}); a right click turns that on and off.
 * {@link #FACING} is where the helm looks: at the player who set it up.
 */
public class KingHelmBlock extends HorizontalDirectionalBlock {
	public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;
	/** Light from the embers in the eye slits of a guarding helm. */
	public static final int LIGHT = 4;

	/** The pike, and the helm on top of it, which rises above the block. */
	private static final VoxelShape SHAPE = Shapes.or(Block.box(7, 0, 7, 9, 18, 9), Block.box(4, 17, 4, 12, 26, 12));
	/** Only the pike stops a walker: the helm is too high to step on. */
	private static final VoxelShape COLLISION = Block.box(6.5, 0, 6.5, 9.5, 24, 9.5);

	public KingHelmBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ENABLED, true));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, ENABLED);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (!level.isClientSide()) {
			boolean enabled = !state.getValue(ENABLED);
			level.setBlock(pos, state.setValue(ENABLED, enabled), Block.UPDATE_ALL);
			level.playSound(null, pos, enabled ? SoundEvents.FIRECHARGE_USE : SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6F,
					enabled ? 0.7F : 1.2F);
			player.sendOverlayMessage(Component.translatable(enabled ? "block.eam.pillager_king_helm.on" : "block.eam.pillager_king_helm.off")
					.withStyle(enabled ? ChatFormatting.GOLD : ChatFormatting.GRAY));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return Block.canSupportCenter(level, pos.below(), Direction.UP);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour,
			BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
		return directionToNeighbour == Direction.DOWN && !state.canSurvive(level, pos)
				? Blocks.AIR.defaultBlockState()
				: super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return COLLISION;
	}
}
