package io.github.hellenter.eam.mixin.content.silver;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;

import io.github.hellenter.eam.content.silver.SilverModule;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerPlayer.class)
abstract class ServerPlayerMixin {
	/** Silver weapons hit undead harder; vanilla scales this part by attack strength like Smite. */
	@ModifyReturnValue(method = "getEnchantedDamage", at = @At("RETURN"))
	private float eam$silverUndeadBonus(float damage, @Local(argsOnly = true) Entity target, @Local(argsOnly = true) float baseDamage) {
		return SilverModule.attackDamage((ServerPlayer) (Object) this, target, baseDamage, damage);
	}
}
