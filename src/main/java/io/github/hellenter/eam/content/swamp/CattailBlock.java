package io.github.hellenter.eam.content.swamp;

import io.github.hellenter.eam.Eam;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.SegmentableBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jspecify.annotations.Nullable;

/**
 * Cattail: two blocks tall, growing on the shore or out of water one block deep. The lower half can stand in water,
 * the upper half never does, so a cattail cannot be planted where the water is deeper. Like leaf litter it comes in
 * clumps of 1 to 4 stalks ({@link #AMOUNT}): using a cattail on one adds a stalk.
 */
public class CattailBlock extends DoublePlantBlock implements SimpleWaterloggedBlock {
	/** What a cattail roots in: soil, mud, clay, sand and gravel. */
	public static final TagKey<Block> SUPPORTS_CATTAIL = TagKey.create(Registries.BLOCK, Eam.id("supports_cattail"));
	private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
	/** Stalks in the clump, 1 to 4; both halves always agree. */
	public static final IntegerProperty AMOUNT = BlockStateProperties.SEGMENT_AMOUNT;
	private static final VoxelShape SHAPE = Block.column(10.0, 0.0, 16.0);

	public CattailBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(HALF, DoubleBlockHalf.LOWER).setValue(WATERLOGGED, false)
				.setValue(AMOUNT, 1));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
		return state.is(SUPPORTS_CATTAIL);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		if (!level.getFluidState(pos.above()).isEmpty()) {
			return null;
		}
		BlockState state = super.getStateForPlacement(context);
		return state == null ? null : copyWaterloggedFrom(level, pos, state);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hitResult) {
		if (!stack.is(this.asItem()) || state.getValue(AMOUNT) >= SegmentableBlock.MAX_SEGMENT || player.isSecondaryUseActive()) {
			return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
		}
		BlockPos lower = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
		BlockState lowerState = level.getBlockState(lower);
		BlockState upperState = level.getBlockState(lower.above());
		if (!lowerState.is(this) || !upperState.is(this)) {
			return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
		}
		if (!level.isClientSide()) {
			int amount = lowerState.getValue(AMOUNT) + 1;
			level.setBlock(lower, lowerState.setValue(AMOUNT, amount), Block.UPDATE_CLIENTS);
			level.setBlock(lower.above(), upperState.setValue(AMOUNT, amount), Block.UPDATE_CLIENTS);
			level.playSound(null, pos, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 1.0F, 0.9F);
			stack.consume(1, player);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
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
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(HALF, WATERLOGGED, AMOUNT);
	}
}
