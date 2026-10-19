package io.github.hellenter.eam.mixin.mobs.vanilla.raid;

import io.github.hellenter.eam.mobs.vanilla.raid.VillageRaidsModule;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.raid.Raid;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Reinforcements join each new wave; raiders of a long wave glow. */
@Mixin(Raid.class)
abstract class RaidMixin {
	@Inject(method = "spawnGroup", at = @At("TAIL"))
	private void eam$reinforce(ServerLevel level, BlockPos pos, CallbackInfo info) {
		VillageRaidsModule.waveSpawned((Raid) (Object) this, level, pos);
	}

	@Inject(method = "tick", at = @At("RETURN"))
	private void eam$glow(ServerLevel level, CallbackInfo info) {
		VillageRaidsModule.tick((Raid) (Object) this, level);
	}
}
