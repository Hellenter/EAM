package io.github.hellenter.eam.content.swamp;

import io.github.hellenter.eam.content.EamItems;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The crown of a big swamp shrub: a block of leaves you can push through at half speed, like the small dense shrub.
 * Some bear wolfberries ({@link BerryShrub}), which are poisonous.
 */
public class BigShrubLeavesBlock extends Block {
	public BigShrubLeavesBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(BerryShrub.BERRIES, BerryShrub.NONE));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(BerryShrub.BERRIES);
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier,
			boolean isPrecise) {
		entity.makeStuckInBlock(state, DenseShrubBlock.SLOWDOWN);
	}

	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		return BerryShrub.isRandomlyTicking(state);
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		BerryShrub.randomTick(state, level, pos, random);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		InteractionResult result = BerryShrub.pick(state, level, pos, player, () -> EamItems.WOLFBERRY, 1, 2);
		return result == InteractionResult.PASS ? super.useWithoutItem(state, level, pos, player, hitResult) : result;
	}
}
