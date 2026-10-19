package io.github.hellenter.eam.mixin.mobs.vanilla.villager;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import io.github.hellenter.eam.mobs.vanilla.villager.VillagerRepairModule;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.behavior.HarvestFarmland;
import net.minecraft.world.entity.npc.villager.Villager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** A farmer's hoe wears with each crop he harvests. */
@Mixin(HarvestFarmland.class)
abstract class HarvestFarmlandMixin {
	@WrapOperation(method = "tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/npc/villager/Villager;J)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;)Z"))
	private boolean eam$wearHoe(ServerLevel level, BlockPos pos, boolean drop, Entity breaker, Operation<Boolean> original) {
		boolean harvested = original.call(level, pos, drop, breaker);
		if (breaker instanceof Villager villager) VillagerRepairModule.afterHarvest(villager, harvested);
		return harvested;
	}
}
