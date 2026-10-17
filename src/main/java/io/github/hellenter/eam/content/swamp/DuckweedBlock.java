package io.github.hellenter.eam.content.swamp;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LilyPadBlock;
import net.minecraft.world.level.block.SegmentableBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jspecify.annotations.Nullable;

/**
 * Duckweed: a mat of tiny leaves floating on still water, placed like a lily pad. It carries nothing, so players
 * and mobs sink through it and boats sail across without breaking it. Like leaf litter it thickens in stages: about
 * 20, 50, 80 and 100% of the water covered ({@link #COVERAGE} 1 to 4); placing more duckweed on it adds a stage.
 */
public class DuckweedBlock extends LilyPadBlock implements SegmentableBlock {
	public static final int MAX_COVERAGE = 4;
	public static final IntegerProperty COVERAGE = IntegerProperty.create("coverage", 1, MAX_COVERAGE);
	/** Follows the sheet, which lies on the water surface 16/9 pixels below the block (still water is 8/9 high). */
	private static final VoxelShape SHAPE = Block.column(16.0, -1.7, -0.7);

	public DuckweedBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(COVERAGE, 1));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(COVERAGE);
	}

	/** For the loot table: one duckweed per stage, like leaf litter drops one per segment. */
	@Override
	public IntegerProperty getSegmentAmountProperty() {
		return COVERAGE;
	}

	@Override
	protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
		return !context.isSecondaryUseActive() && context.getItemInHand().is(this.asItem()) && state.getValue(COVERAGE) < MAX_COVERAGE
				|| super.canBeReplaced(state, context);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState there = context.getLevel().getBlockState(context.getClickedPos());
		return there.is(this) ? there.setValue(COVERAGE, Math.min(MAX_COVERAGE, there.getValue(COVERAGE) + 1)) : this.defaultBlockState();
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier,
			boolean isPrecise) {
		// A lily pad breaks under a boat; duckweed just parts.
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}
}
