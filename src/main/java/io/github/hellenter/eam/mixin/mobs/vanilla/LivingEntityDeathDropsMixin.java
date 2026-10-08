package io.github.hellenter.eam.mixin.mobs.vanilla;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;

import io.github.hellenter.eam.mobs.vanilla.drops.MobDropsModule;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** A mob burned to death by daylight drops no loot, equipment or experience. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityDeathDropsMixin {
	@WrapWithCondition(method = "die", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/LivingEntity;dropAllDeathLoot(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;)V"))
	private boolean eam$noSunDeathDrops(LivingEntity entity, ServerLevel level, DamageSource source) {
		return MobDropsModule.dropsOnDeath(entity, source);
	}
}
