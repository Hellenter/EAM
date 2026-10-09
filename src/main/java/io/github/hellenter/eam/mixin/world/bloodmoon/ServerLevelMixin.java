package io.github.hellenter.eam.mixin.world.bloodmoon;

import io.github.hellenter.eam.world.bloodmoon.BloodMoonModule;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
abstract class ServerLevelMixin {
	/** Freshly spawned mobs, after vanilla handed out their equipment: blood moon armor and weapons go on top. */
	@Inject(method = "addFreshEntity", at = @At("HEAD"))
	private void eam$bloodMoonEquipment(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		BloodMoonModule.onFreshEntity(entity);
	}
}
