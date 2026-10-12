package io.github.hellenter.eam.mixin.mobs.guard;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import io.github.hellenter.eam.mobs.entity.guard.GuardSpawns;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Some pillagers spawning at an outpost come as guards: outposts are the only place pillagers spawn naturally. */
@Mixin(NaturalSpawner.class)
abstract class GuardNaturalSpawnerMixin {
	@WrapOperation(method = "getMobForSpawn", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/EntityType;create(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/EntitySpawnReason;)Lnet/minecraft/world/entity/Entity;"))
	private static Entity eam$guard(EntityType<?> type, Level level, EntitySpawnReason reason, Operation<Entity> original) {
		return GuardSpawns.maybeGuard(original.call(type, level, reason), level, reason);
	}
}
