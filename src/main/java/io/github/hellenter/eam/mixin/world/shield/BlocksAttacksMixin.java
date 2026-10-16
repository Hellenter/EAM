package io.github.hellenter.eam.mixin.world.shield;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import io.github.hellenter.eam.world.shield.ShieldLogic;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlocksAttacks;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Every way a shield gets disabled (axes, our mobs and bosses) goes through {@code BlocksAttacks.disable}. */
@Mixin(BlocksAttacks.class)
abstract class BlocksAttacksMixin {
	@WrapOperation(method = "disable", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/item/component/BlocksAttacks;disableBlockingForTicks(F)I"))
	private int eam$shortenDisable(BlocksAttacks self, float baseSeconds, Operation<Integer> original,
			@Local(argsOnly = true) ServerLevel level, @Local(argsOnly = true) ItemStack blockingWith) {
		return original.call(self, ShieldLogic.disableSeconds(blockingWith, baseSeconds, level.getRandom()));
	}
}
