package io.github.hellenter.eam.content.underworld;

import io.github.hellenter.eam.content.EamBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.NetherVines;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The glowing tip of glowshroom vines, the threads of a fungus hanging from the ceilings of the underground forest. It
 * grows down like weeping vines; {@link GlowshroomVinesPlantBlock} is the rest of the strand.
 * <p>
 * A {@link #RIPE} tip carries bright caps: shears cut them off ({@link GlowshroomModule#HARVEST} loot) and leave a dim
 * tip that ripens again on random ticks, or at once with bone meal. Tips from worldgen start ripe, as do tips of worlds
 * saved before ripeness existed; a tip that appears any other way (placed, or left when the one below it broke) starts
 * bare, so breaking a strand piece by piece gives no free caps.
 */
public class GlowshroomVinesBlock extends GrowingPlantHeadBlock {
	public static final BooleanProperty RIPE = BooleanProperty.create("ripe");
	private static final VoxelShape SHAPE = Block.column(8.0, 9.0, 16.0);

	public GlowshroomVinesBlock(BlockBehaviour.Properties properties) {
		super(properties, Direction.DOWN, SHAPE, false, 0.1);
		registerDefaultState(defaultBlockState().setValue(RIPE, true));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(RIPE);
	}

	@Override
	public BlockState getStateForPlacement(RandomSource random) {
		return super.getStateForPlacement(random).setValue(RIPE, false);
	}

	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		return super.isRandomlyTicking(state) || !state.getValue(RIPE);
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (GlowshroomLogic.ripens(state.getValue(RIPE), random.nextFloat(), GlowshroomModule.settings().ripenChance())) {
			level.setBlock(pos, state.setValue(RIPE, true), Block.UPDATE_CLIENTS);
			return;
		}
		super.randomTick(state, level, pos, random);
	}

	/** Shears on a ripe tip cut its caps off; on a bare tip they stop it growing, as vanilla shears do. */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		if (!(stack.getItem() instanceof ShearsItem) || !state.getValue(RIPE)) {
			return super.useItemOn(stack, state, level, pos, player, hand, hit);
		}
		if (level instanceof ServerLevel serverLevel) {
			Block.dropFromBlockInteractLootTable(serverLevel, GlowshroomModule.HARVEST, pos, state, null, stack, player,
					(lvl, drop) -> Block.popResource(lvl, pos, drop));
			serverLevel.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 1.0F, 1.2F);
			BlockState cut = state.setValue(RIPE, false);
			serverLevel.setBlock(pos, cut, Block.UPDATE_CLIENTS);
			serverLevel.gameEvent(GameEvent.SHEAR, pos, GameEvent.Context.of(player, cut));
			stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
		}
		return InteractionResult.SUCCESS;
	}

	/** Bone meal ripens a bare tip first; on a ripe one it grows the strand, as on weeping vines. */
	@Override
	public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
		return !state.getValue(RIPE) || super.isValidBonemealTarget(level, pos, state, source);
	}

	@Override
	public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
		if (!state.getValue(RIPE)) {
			level.setBlock(pos, state.setValue(RIPE, true), Block.UPDATE_CLIENTS);
			return;
		}
		super.performBonemeal(level, random, pos, state, source);
	}

	@Override
	protected int getBlocksToGrowWhenBonemealed(RandomSource random) {
		return NetherVines.getBlocksToGrowWhenBonemealed(random);
	}

	@Override
	protected Block getBodyBlock() {
		return EamBlocks.GLOWSHROOM_VINES_PLANT;
	}

	@Override
	protected boolean canGrowInto(BlockState state) {
		return NetherVines.isValidGrowthState(state);
	}
}
