package io.github.hellenter.eam.mixin.world.forging;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import io.github.hellenter.eam.content.anvil.MetalAnvilBlock;
import io.github.hellenter.eam.world.forging.ForgingModule;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Copper and silver anvils wear in stages too, when used or dropped, instead of breaking at once. Every anvil gets a
 * block entity that keeps its hammer.
 */
@Mixin(AnvilBlock.class)
abstract class AnvilBlockMixin implements EntityBlock {
	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return ForgingModule.newBlockEntity(pos, state);
	}

	@ModifyReturnValue(method = "damage", at = @At("RETURN"))
	private static @Nullable BlockState eam$wearMetalAnvil(@Nullable BlockState original, BlockState state) {
		return original == null && state.getBlock() instanceof MetalAnvilBlock ? MetalAnvilBlock.worn(state) : original;
	}
}
