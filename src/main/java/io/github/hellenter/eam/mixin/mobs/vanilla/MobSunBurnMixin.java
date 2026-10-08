package io.github.hellenter.eam.mixin.mobs.vanilla;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import io.github.hellenter.eam.mobs.vanilla.drops.MobDropsModule;

import net.minecraft.world.entity.Mob;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Remembers when daylight set an undead mob on fire. */
@Mixin(Mob.class)
public abstract class MobSunBurnMixin {
	@WrapOperation(method = "burnUndead", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;igniteForSeconds(F)V"))
	private void eam$markSunIgnition(Mob mob, float seconds, Operation<Void> original) {
		original.call(mob, seconds);
		MobDropsModule.onSunIgnition(mob);
	}
}
