package io.github.hellenter.eam.mixin.survival.sleep;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.util.Either;

import io.github.hellenter.eam.survival.sleep.SafeRoomModule;
import io.github.hellenter.eam.world.bloodmoon.BloodMoonModule;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
abstract class ServerPlayerSleepMixin {
	/**
	 * Right after vanilla set the spawn point and found it is night, before the monster check: the bed must stand in a
	 * safe room, and no blood moon may be up. Fabric's sleep events run before the spawn point is set, so the bed would stop setting it.
	 */
	@Inject(method = "startSleepInBed", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;isCreative()Z"),
			cancellable = true)
	private void eam$safeRoom(CallbackInfoReturnable<Either<Player.BedSleepingProblem, Unit>> cir,
			@Local(argsOnly = true) BlockState bed, @Local(argsOnly = true) BlockPos pos) {
		ServerPlayer player = (ServerPlayer) (Object) this;
		Player.BedSleepingProblem problem = BloodMoonModule.sleepProblem(player.level());
		if (problem == null) {
			problem = SafeRoomModule.problem(player, bed, pos);
		}
		if (problem != null) {
			cir.setReturnValue(Either.left(problem));
		}
	}
}
