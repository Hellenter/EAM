package io.github.hellenter.eam.mixin.mobs.entity.yaga;

import io.github.hellenter.eam.mobs.entity.yaga.BabaYagaModule;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.NaturalSpawner;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** No hostile mob spawns naturally near a Baba Yaga: the only monsters about her are her own. */
@Mixin(NaturalSpawner.class)
abstract class NaturalSpawnerYagaMixin {
	@ModifyReturnValue(method = "isValidPositionForMob", at = @At("RETURN"))
	private static boolean eam$noSpawnNearYaga(boolean valid, ServerLevel level, Mob mob, double distance) {
		return valid && !BabaYagaModule.keepsAway(mob);
	}
}
