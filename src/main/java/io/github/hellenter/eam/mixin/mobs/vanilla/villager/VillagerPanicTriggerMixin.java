package io.github.hellenter.eam.mixin.mobs.vanilla.villager;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import io.github.hellenter.eam.mobs.vanilla.villager.VillagerGuardModule;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.VillagerPanicTrigger;
import net.minecraft.world.entity.npc.villager.Villager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Guards do not panic when hurt or near a monster. */
@Mixin(VillagerPanicTrigger.class)
abstract class VillagerPanicTriggerMixin {
	@WrapOperation(method = {
			"canStillUse(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/npc/villager/Villager;J)Z",
			"start(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/npc/villager/Villager;J)V"},
			at = {
					@At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/behavior/VillagerPanicTrigger;isHurt(Lnet/minecraft/world/entity/LivingEntity;)Z"),
					@At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/behavior/VillagerPanicTrigger;hasHostile(Lnet/minecraft/world/entity/LivingEntity;)Z")})
	private boolean eam$guardsHoldGround(LivingEntity body, Operation<Boolean> original, @Local(argsOnly = true) Villager villager) {
		return VillagerGuardModule.panics(villager, original.call(body));
	}
}
