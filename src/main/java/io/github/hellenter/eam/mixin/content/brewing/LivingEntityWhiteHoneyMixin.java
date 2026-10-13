package io.github.hellenter.eam.mixin.content.brewing;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;

import io.github.hellenter.eam.content.brewing.WhiteHoneyLogic;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
abstract class LivingEntityWhiteHoneyMixin {
	/** White honey keeps poison, slowness and weakness off. */
	@ModifyReturnValue(method = "canBeAffected", at = @At("RETURN"))
	private boolean eam$whiteHoneyWard(boolean affected, @Local(argsOnly = true) MobEffectInstance effect) {
		return WhiteHoneyLogic.canBeAffected((LivingEntity) (Object) this, effect, affected);
	}

	/** White honey halves wither damage. */
	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
	private float eam$whiteHoneyWither(float amount, @Local(argsOnly = true) DamageSource source) {
		return WhiteHoneyLogic.hurt((LivingEntity) (Object) this, source, amount);
	}
}
