package io.github.hellenter.eam.mixin.mobs.vanilla.villager;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import io.github.hellenter.eam.mobs.vanilla.villager.VillagerAlarmModule;
import io.github.hellenter.eam.mobs.vanilla.villager.VillagerButcherModule;
import io.github.hellenter.eam.mobs.vanilla.villager.VillagerClericModule;
import io.github.hellenter.eam.mobs.vanilla.villager.VillagerElderModule;
import io.github.hellenter.eam.mobs.vanilla.villager.VillagerFoodModule;
import io.github.hellenter.eam.mobs.vanilla.villager.VillagerGearModule;
import io.github.hellenter.eam.mobs.vanilla.villager.VillagerGuardModule;
import io.github.hellenter.eam.mobs.vanilla.villager.VillagerRepairModule;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;

import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A villager who takes up a trade gets its work item; now and then his gear gets mended and his trade feeds the
 * village; his brain waits while he fights as a guard, answers the village alarm or a player has his inventory open.
 */
@Mixin(Villager.class)
abstract class VillagerMixin {
	@Inject(method = "setVillagerData", at = @At("TAIL"))
	private void eam$takeWorkItem(VillagerData data, CallbackInfo ci) {
		VillagerGearModule.onVillagerDataChanged((Villager) (Object) this);
	}

	@WrapOperation(method = "customServerAiStep", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/ai/Brain;tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;)V"))
	private void eam$waitWhileFighting(Brain<?> brain, ServerLevel level, LivingEntity body, Operation<Void> original) {
		Villager villager = (Villager) (Object) this;
		if (VillagerGuardModule.thinks(villager) && VillagerGearModule.thinks(villager) && VillagerAlarmModule.thinks(villager)) original.call(brain, level, body);
	}

	@Inject(method = "customServerAiStep", at = @At("HEAD"))
	private void eam$doVillageWork(ServerLevel level, CallbackInfo ci) {
		VillagerRepairModule.tick((Villager) (Object) this, level);
		VillagerFoodModule.tick((Villager) (Object) this, level);
		VillagerButcherModule.tick((Villager) (Object) this, level);
		VillagerClericModule.tick((Villager) (Object) this, level);
		VillagerElderModule.tick((Villager) (Object) this, level);
		VillagerAlarmModule.tick((Villager) (Object) this, level);
	}
}
