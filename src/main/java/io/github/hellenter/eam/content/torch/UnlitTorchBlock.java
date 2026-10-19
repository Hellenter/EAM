package io.github.hellenter.eam.content.torch;

import io.github.hellenter.eam.content.campfire.CampFireModule;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseTorchBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A torch the rain has put out. It gives no light; an igniter (flint and steel, fire charge, flint, flint shard) lights
 * it again and turns it back into a vanilla torch.
 */
public class UnlitTorchBlock extends BaseTorchBlock {
	public UnlitTorchBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hitResult) {
		if (!stack.is(CampFireModule.IGNITERS)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		return light(Blocks.TORCH.defaultBlockState(), stack, level, pos, player, hand);
	}

	/** Puts {@code lit} in place of the unlit torch and spends the igniter: a fire charge is used up, flint and steel wears. */
	static InteractionResult light(BlockState lit, ItemStack stack, Level level, BlockPos pos, Player player, InteractionHand hand) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		level.setBlock(pos, lit, Block.UPDATE_ALL);
		level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		if (stack.is(Items.FIRE_CHARGE)) {
			level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
			stack.consume(1, player);
		} else {
			level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.4F + 0.8F);
			if (stack.isDamageableItem()) {
				stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
			}
		}
		return InteractionResult.SUCCESS;
	}
}
