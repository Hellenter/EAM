package io.github.hellenter.eam.mixin.mobs.entity.boss;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import io.github.hellenter.eam.mobs.entity.boss.BossBuilding;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** A block a player puts up next to a fighting boss is remembered, so the boss may break it. */
@Mixin(BlockItem.class)
abstract class BlockItemPlacedMixin {
	@WrapOperation(method = "place", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/item/BlockItem;placeBlock(Lnet/minecraft/world/item/context/BlockPlaceContext;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
	private boolean eam$rememberPlaced(BlockItem item, BlockPlaceContext context, BlockState state, Operation<Boolean> original) {
		boolean placed = original.call(item, context, state);
		BossBuilding.placed(context.getLevel(), context.getClickedPos(), context.getPlayer(), placed);
		return placed;
	}
}
