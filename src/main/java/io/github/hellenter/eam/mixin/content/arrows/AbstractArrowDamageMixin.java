package io.github.hellenter.eam.mixin.content.arrows;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import io.github.hellenter.eam.content.arrows.ArrowHeadModule;

import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.EntityHitResult;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractArrow.class)
abstract class AbstractArrowDamageMixin {
	/** Metal arrow heads raise the base damage; speed, Power and crits still apply on top. */
	@ModifyExpressionValue(method = "onHitEntity",
			at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/projectile/arrow/AbstractArrow;baseDamage:D"))
	private double eam$arrowHeadDamage(double baseDamage, @Local(argsOnly = true) EntityHitResult hit) {
		return ArrowHeadModule.baseDamage((AbstractArrow) (Object) this, hit.getEntity(), baseDamage);
	}
}
