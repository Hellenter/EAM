package io.github.hellenter.eam.content.ground;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jspecify.annotations.Nullable;

/**
 * A small loose item lying on top of a block (pebble, flint shard), at one of 16 positions. Breaks
 * instantly and drops its item; pops off when the block below goes.
 */
public class GroundItemBlock extends Block {
	public static final IntegerProperty POSITION = IntegerProperty.create("position", 0, GroundItemLogic.POSITIONS - 1);

	private final GroundItemLogic.Shape shape;
	private final VoxelShape[] shapes = new VoxelShape[GroundItemLogic.POSITIONS];

	public GroundItemBlock(GroundItemLogic.Shape shape, BlockBehaviour.Properties properties) {
		super(properties);
		this.shape = shape;
		for (int position = 0; position < shapes.length; position++) {
			VoxelShape outline = Shapes.empty();
			for (GroundItemLogic.Box box : shape.at(position).outline()) {
				outline = Shapes.or(outline, Block.box(box.minX(), 0, box.minZ(), box.maxX(), box.height(), box.maxZ()));
			}
			shapes[position] = outline;
		}
		registerDefaultState(stateDefinition.any().setValue(POSITION, 0));
	}

	/** How the item lies at each position; the block models are generated from it. */
	public GroundItemLogic.Shape shape() {
		return shape;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(POSITION);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shapes[state.getValue(POSITION)];
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockPos pos = context.getClickedPos();
		int position;
		if (context.getClickedFace() == Direction.UP) {
			// Lie where the player clicked on the top face of the block below.
			Vec3 click = context.getClickLocation();
			position = GroundItemLogic.positionAt(click.x - pos.getX(), click.z - pos.getZ());
		} else {
			position = context.getLevel().getRandom().nextInt(GroundItemLogic.POSITIONS);
		}
		return defaultBlockState().setValue(POSITION, position);
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return Block.canSupportCenter(level, pos.below(), Direction.UP);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
			Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
		return !state.canSurvive(level, pos)
				? Blocks.AIR.defaultBlockState()
				: super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
	}
}
