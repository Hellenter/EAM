package io.github.hellenter.eam.mixin.mobs.entity.lost;

import io.github.hellenter.eam.mobs.entity.lost.LostMark;

import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
abstract class LivingEntityMarkMixin {
	/** The Lost longbowman's mark: the marked one takes more damage from everything. */
	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
	private float eam$markedDamage(float amount) {
		return LostMark.damage((LivingEntity) (Object) this, amount);
	}
}
