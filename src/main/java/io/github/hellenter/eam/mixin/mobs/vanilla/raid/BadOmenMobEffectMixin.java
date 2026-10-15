package io.github.hellenter.eam.mixin.mobs.vanilla.raid;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import io.github.hellenter.eam.mobs.vanilla.raid.VillageRaidsModule;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** In a village guarded by the trophy, Bad Omen does not turn into Raid Omen: the player keeps it for another village. */
@Mixin(targets = "net.minecraft.world.effect.BadOmenMobEffect")
abstract class BadOmenMobEffectMixin {
	@WrapOperation(method = "applyEffectTick", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/server/level/ServerLevel;isVillage(Lnet/minecraft/core/BlockPos;)Z"))
	private boolean eam$guardedVillage(ServerLevel level, BlockPos pos, Operation<Boolean> original) {
		return original.call(level, pos) && !VillageRaidsModule.guarded(level, pos);
	}
}
