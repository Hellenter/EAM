package io.github.hellenter.eam.mixin.mobs.entity.ghosthorse;

import com.llamalad7.mixinextras.sugar.Local;

import io.github.hellenter.eam.mobs.entity.ghosthorse.GhostHorseModule;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
abstract class LivingEntityGhostRiderMixin {
	/** The ghost horse's rider takes less damage and deals more. */
	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
	private float eam$ghostRider(float amount, @Local(argsOnly = true) DamageSource source) {
		return GhostHorseModule.hurt((LivingEntity) (Object) this, source, amount);
	}
}
