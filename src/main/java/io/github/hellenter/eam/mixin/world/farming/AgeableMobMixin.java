package io.github.hellenter.eam.mixin.world.farming;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import io.github.hellenter.eam.world.farming.FarmingModule;

import net.minecraft.world.entity.AgeableMob;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AgeableMob.class)
abstract class AgeableMobMixin {
	@WrapOperation(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/AgeableMob;setAge(I)V"))
	private void eam$ageSpeed(AgeableMob mob, int age, Operation<Void> original) {
		int next = FarmingModule.nextAge(mob, age);
		if (next != mob.getAge()) original.call(mob, next);
	}
}
