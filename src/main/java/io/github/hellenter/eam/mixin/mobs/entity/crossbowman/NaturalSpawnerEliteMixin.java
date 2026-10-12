package io.github.hellenter.eam.mixin.mobs.entity.crossbowman;

import io.github.hellenter.eam.mobs.entity.crossbowman.EliteCrossbowmanModule;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** A mob spawning naturally (pillagers do only at outposts): a pillager may spawn as an elite crossbowman instead. */
@Mixin(NaturalSpawner.class)
abstract class NaturalSpawnerEliteMixin {
	@WrapOperation(method = "getMobForSpawn", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/EntityType;create(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/EntitySpawnReason;)Lnet/minecraft/world/entity/Entity;"))
	private static Entity eam$eliteCrossbowman(EntityType<?> type, Level level, EntitySpawnReason reason, Operation<Entity> original) {
		return EliteCrossbowmanModule.promote(original.call(type, level, reason), level, reason);
	}
}
