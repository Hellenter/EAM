package io.github.hellenter.eam.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The deep bedrock band between the underworld's two levels. It looks and holds like bedrock, but an item in
 * {@code #eam:breaks_deep_bedrock} breaks it, slowly; nothing else even scratches it.
 */
public class DeepBedrockBlock extends Block {
	public DeepBedrockBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		if (!player.getMainHandItem().is(UnderworldDimension.BREAKS_DEEP_BEDROCK)) {
			return 0.0F;
		}
		return super.getDestroyProgress(state, player, level, pos);
	}
}
