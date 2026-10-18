package io.github.hellenter.eam.mixin.mobs.vanilla.villager;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import io.github.hellenter.eam.mobs.vanilla.villager.VillageEconomyModule;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.ai.behavior.VillagerMakeLove;
import net.minecraft.world.entity.npc.villager.Villager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** A child born of two villagers is paid for from the village's provisions; one from a spawn egg is not. */
@Mixin(VillagerMakeLove.class)
abstract class VillagerMakeLoveMixin {
	@WrapOperation(method = "breed", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/npc/villager/Villager;getBreedOffspring(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/AgeableMob;)Lnet/minecraft/world/entity/npc/villager/Villager;"))
	private Villager eam$payForChild(Villager source, ServerLevel level, AgeableMob partner, Operation<Villager> original) {
		return VillageEconomyModule.born(source, original.call(source, level, partner));
	}
}
