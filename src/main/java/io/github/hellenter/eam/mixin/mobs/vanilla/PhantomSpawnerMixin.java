package io.github.hellenter.eam.mixin.mobs.vanilla;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import io.github.hellenter.eam.mobs.vanilla.evolution.MonsterEvolutionModule;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.PhantomSpawner;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PhantomSpawner.class)
abstract class PhantomSpawnerMixin {
	/** No phantoms in a young world: they spawn, as the game rule allows, only from the evolution's phantom day. */
	@ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/gamerules/GameRules;get(Lnet/minecraft/world/level/gamerules/GameRule;)Ljava/lang/Object;"))
	private Object eam$phantomsWaitForTheirDay(Object spawnPhantoms, ServerLevel level) {
		return (Boolean) spawnPhantoms && MonsterEvolutionModule.phantomsAllowed(level);
	}
}
