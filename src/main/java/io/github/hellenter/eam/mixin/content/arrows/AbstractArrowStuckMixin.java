package io.github.hellenter.eam.mixin.content.arrows;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import io.github.hellenter.eam.content.arrows.ArrowRecoveryModule;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractArrow.class)
abstract class AbstractArrowStuckMixin {
	/** Vanilla counts an arrow that hurt a mob without piercing it as stuck; we also keep its item to drop on death. */
	@WrapOperation(method = "onHitEntity",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setArrowCount(I)V"))
	private void eam$keepStuckArrow(LivingEntity mob, int count, Operation<Void> original) {
		original.call(mob, count);
		ArrowRecoveryModule.onArrowStuck((AbstractArrow) (Object) this, mob);
	}
}
