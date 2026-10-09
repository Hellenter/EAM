package io.github.hellenter.eam.mixin.world.bloodmoon;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;

import io.github.hellenter.eam.world.bloodmoon.BloodMoonModule;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
	/** Hostile mobs hit harder under a blood moon. */
	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
	private float eam$bloodMoonDamage(float amount, @Local(argsOnly = true) DamageSource source) {
		return BloodMoonModule.damage((LivingEntity) (Object) this, source, amount);
	}

	/** Hostile mobs leave more experience under a blood moon. */
	@ModifyReturnValue(method = "getExperienceReward", at = @At("RETURN"))
	private int eam$bloodMoonExperience(int amount) {
		return BloodMoonModule.experience((LivingEntity) (Object) this, amount);
	}
}
