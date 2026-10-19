package io.github.hellenter.eam.client.mixin.mobs.leshy;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import io.github.hellenter.eam.mobs.entity.leshy.LeshyModule;

import net.minecraft.client.renderer.item.properties.numeric.CompassAngleState;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.ItemOwner;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** A compass in the hands of a player led astray by a leshy spins as if it had nothing to point at. */
@Mixin(CompassAngleState.class)
abstract class CompassAngleStateMixin {
	@WrapOperation(method = "calculate", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/item/properties/numeric/CompassAngleState;isValidCompassTargetPos(Lnet/minecraft/world/entity/ItemOwner;Lnet/minecraft/core/GlobalPos;)Z"))
	private boolean eam$bewildered(ItemOwner owner, GlobalPos target, Operation<Boolean> original) {
		return original.call(owner, target) && !LeshyModule.bewildered(owner.asLivingEntity());
	}
}
