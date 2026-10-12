package io.github.hellenter.eam.mixin.mobs.entity.crossbowman;

import io.github.hellenter.eam.mobs.entity.crossbowman.EliteCrossbowmanModule;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.PatrolSpawner;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** A patrol member: a pillager may spawn as an elite crossbowman instead. */
@Mixin(PatrolSpawner.class)
abstract class PatrolSpawnerEliteMixin {
	@WrapOperation(method = "spawnPatrolMember", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/EntityType;create(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/EntitySpawnReason;)Lnet/minecraft/world/entity/Entity;"))
	private Entity eam$eliteCrossbowman(EntityType<?> type, Level level, EntitySpawnReason reason, Operation<Entity> original) {
		return EliteCrossbowmanModule.promote(original.call(type, level, reason), level, reason);
	}
}
