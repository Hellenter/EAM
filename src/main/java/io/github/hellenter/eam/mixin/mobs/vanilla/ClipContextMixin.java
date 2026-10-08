package io.github.hellenter.eam.mixin.mobs.vanilla;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import io.github.hellenter.eam.mobs.vanilla.leaves.ThroughLeavesModule;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** Arrow flight and skeleton line of sight ignore leaves. */
@Mixin(ClipContext.class)
public abstract class ClipContextMixin {
	@Shadow
	@Final
	private CollisionContext collisionContext;

	@ModifyReturnValue(method = "getBlockShape", at = @At("RETURN"))
	private VoxelShape eam$throughLeaves(VoxelShape original, BlockState state, BlockGetter level, BlockPos pos) {
		return !original.isEmpty() && ThroughLeavesModule.passes(state, collisionContext) ? Shapes.empty() : original;
	}
}
