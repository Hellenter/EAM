package io.github.hellenter.eam.mixin.world.mining;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import io.github.hellenter.eam.world.mining.HandBreakingModule;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Most blocks cannot be broken by hand. */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {
	@ModifyReturnValue(method = "getDestroyProgress", at = @At("RETURN"))
	private float eam$handBreaking(float original, Player player, BlockGetter level, BlockPos pos) {
		return HandBreakingModule.destroyProgress((BlockState) (Object) this, player, level, pos, original);
	}
}
