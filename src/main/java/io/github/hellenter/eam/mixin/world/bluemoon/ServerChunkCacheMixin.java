package io.github.hellenter.eam.mixin.world.bluemoon;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import io.github.hellenter.eam.world.bluemoon.BlueMoonModule;

import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerChunkCache.class)
abstract class ServerChunkCacheMixin {
	@Shadow
	@Final
	private ServerLevel level;

	/** No monsters spawn under a blue moon: neither natural spawns nor patrols, phantoms or sieges. */
	@ModifyExpressionValue(method = "tickChunks(Lnet/minecraft/util/profiling/ProfilerFiller;)V", at = @At(value = "FIELD",
			target = "Lnet/minecraft/server/level/ServerChunkCache;spawnEnemies:Z"))
	private boolean eam$blueMoonCalm(boolean spawnEnemies) {
		return BlueMoonModule.spawnEnemies(this.level, spawnEnemies);
	}
}
