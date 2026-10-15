package io.github.hellenter.eam.mixin.mobs.vanilla;

import io.github.hellenter.eam.mobs.vanilla.evolution.MonsterEvolutionModule;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
abstract class ServerLevelEvolutionMixin {
	/** Freshly spawned monsters, after vanilla handed out their equipment, get the gear of the world's age; drops lose the worn look. */
	@Inject(method = "addFreshEntity", at = @At("HEAD"))
	private void eam$monsterEvolution(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		MonsterEvolutionModule.onFreshEntity(entity);
	}
}
