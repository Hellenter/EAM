package io.github.hellenter.eam.mixin.mobs.vanilla;

import io.github.hellenter.eam.mobs.vanilla.nemesis.NemesisModule;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
abstract class LivingEntityNemesisMixin {
	/** Before a dead player's things fall out, the monster that killed them takes the best piece of gear. */
	@Inject(method = "dropAllDeathLoot", at = @At("HEAD"))
	private void eam$nemesis(ServerLevel level, DamageSource source, CallbackInfo ci) {
		NemesisModule.onDeathLoot((LivingEntity) (Object) this, level, source);
	}
}
