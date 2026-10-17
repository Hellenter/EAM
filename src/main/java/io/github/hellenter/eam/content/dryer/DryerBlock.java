package io.github.hellenter.eam.content.dryer;

import io.github.hellenter.eam.content.EamBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jspecify.annotations.Nullable;

/**
 * A food dryer: a low wooden rack strung with twine. Dryable food used on it lies on the twine, up to four items, and
 * dries while no rain falls on it; an empty hand takes it back, dried food first. Breaking it drops the food.
 */
public class DryerBlock extends BaseEntityBlock {
	/** The height of the twine the food lies on, in pixels. */
	public static final double RACK_HEIGHT = 10.0;

	private static final VoxelShape SHAPE = Shapes.or(
			Block.box(1.0, 9.0, 1.0, 15.0, RACK_HEIGHT + 1.0, 15.0),
			Block.box(1.0, 0.0, 1.0, 3.0, 9.0, 3.0),
			Block.box(13.0, 0.0, 1.0, 15.0, 9.0, 3.0),
			Block.box(1.0, 0.0, 13.0, 3.0, 9.0, 15.0),
			Block.box(13.0, 0.0, 13.0, 15.0, 9.0, 15.0));

	public DryerBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hitResult) {
		if (!DryerBlockEntity.accepts(stack) || !(level.getBlockEntity(pos) instanceof DryerBlockEntity dryer)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (level instanceof ServerLevel server && dryer.placeFood(server, player, stack)) {
			level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.6F, 1.2F);
			return InteractionResult.SUCCESS_SERVER;
		}
		return InteractionResult.CONSUME;
	}

	/** An empty hand takes food off the rack, dried food first. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (!(level.getBlockEntity(pos) instanceof DryerBlockEntity dryer) || !dryer.hasFood()) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			ItemStack food = dryer.takeFood();
			player.getInventory().placeItemBackInInventory(food, Prediction.SERVER_ONLY);
			level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, 1.0F);
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
		return new DryerBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, EamBlockEntities.FOOD_DRYER, DryerBlockEntity::serverTick);
	}
}
