package io.github.hellenter.eam.mixin.world.mithril;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import io.github.hellenter.eam.world.mithril.MithrilModule;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Obsidian is softer. */
@Mixin(BlockBehaviour.BlockStateBase.class)
abstract class BlockStateBaseObsidianMixin {
	@ModifyReturnValue(method = "getDestroySpeed", at = @At("RETURN"))
	private float eam$softObsidian(float original) {
		return MithrilModule.destroySpeed((BlockState) (Object) this, original);
	}
}
