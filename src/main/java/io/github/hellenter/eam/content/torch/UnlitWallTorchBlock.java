package io.github.hellenter.eam.content.torch;

import io.github.hellenter.eam.content.campfire.CampFireModule;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** The wall variant of {@link UnlitTorchBlock}: no flame, no smoke, and an igniter lights it into a vanilla wall torch. */
public class UnlitWallTorchBlock extends WallTorchBlock {
	public UnlitWallTorchBlock(BlockBehaviour.Properties properties) {
		super(ParticleTypes.SMOKE, properties);
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hitResult) {
		if (!stack.is(CampFireModule.IGNITERS)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		return UnlitTorchBlock.light(Blocks.WALL_TORCH.defaultBlockState().setValue(FACING, state.getValue(FACING)), stack, level, pos, player, hand);
	}
}
