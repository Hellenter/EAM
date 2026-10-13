package io.github.hellenter.eam.content.sieve;

import io.github.hellenter.eam.content.EamBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jspecify.annotations.Nullable;

/**
 * A sieve: a frame of planks on four stick legs with a mesh of leather thread. Gravel used on it sifts for a while and
 * gives what {@link SieveModule#LOOT} rolls, nothing half the time. Each sift wears the mesh; after {@link SieveLogic#USES} sifts
 * the sieve falls apart into sticks. How worn it is travels with the item as its damage.
 */
public class SieveBlock extends BaseEntityBlock {
	/** How torn the mesh looks, see {@link SieveLogic#wear}. */
	public static final IntegerProperty WEAR = IntegerProperty.create("wear", 0, SieveLogic.MAX_WEAR);
	/** Gravel left on the mesh; 0 when the sieve is empty. */
	public static final IntegerProperty GRAVEL = IntegerProperty.create("gravel", 0, SieveLogic.MAX_GRAVEL);

	private static final VoxelShape SHAPE = Shapes.or(
			Block.box(0, 11, 0, 16, 14, 16),
			Block.box(1, 0, 1, 3, 11, 3),
			Block.box(13, 0, 1, 15, 11, 3),
			Block.box(1, 0, 13, 3, 11, 15),
			Block.box(13, 0, 13, 15, 11, 15));

	public SieveBlock(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(WEAR, 0).setValue(GRAVEL, 0));
	}

	/** A worn sieve placed again shows its wear at once. */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		int damage = context.getItemInHand().getOrDefault(DataComponents.DAMAGE, 0);
		return defaultBlockState().setValue(WEAR, SieveLogic.wear(damage));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(WEAR, GRAVEL);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hitResult) {
		if (!stack.is(Items.GRAVEL)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (state.getValue(GRAVEL) > 0) {
			// Still sifting the last gravel.
			return InteractionResult.FAIL;
		}
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SieveBlockEntity sieve) {
			sieve.start();
			level.setBlock(pos, state.setValue(GRAVEL, SieveLogic.MAX_GRAVEL), Block.UPDATE_ALL);
			level.playSound(null, pos, SoundEvents.GRAVEL_PLACE, SoundSource.BLOCKS, 1.0F, 0.9F);
			level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
			stack.consume(1, player);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType type) {
		return false;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SieveBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (state.getValue(GRAVEL) == 0) {
			return null;
		}
		BlockEntityTicker<SieveBlockEntity> ticker = level.isClientSide() ? SieveBlockEntity::clientTick : SieveBlockEntity::serverTick;
		return createTickerHelper(type, EamBlockEntities.SIEVE, ticker);
	}
}
