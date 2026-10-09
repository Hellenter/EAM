package io.github.hellenter.eam.mixin.survival.sleep;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.util.Either;

import io.github.hellenter.eam.survival.sleep.SafeRoomModule;

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
	 * safe room. Fabric's sleep events run before the spawn point is set, so the bed would stop setting it.
	 */
	@Inject(method = "startSleepInBed", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;isCreative()Z"),
			cancellable = true)
	private void eam$safeRoom(CallbackInfoReturnable<Either<Player.BedSleepingProblem, Unit>> cir,
			@Local(argsOnly = true) BlockState bed, @Local(argsOnly = true) BlockPos pos) {
		Player.BedSleepingProblem problem = SafeRoomModule.problem(((ServerPlayer) (Object) this).level(), bed, pos);
		if (problem != null) {
			cir.setReturnValue(Either.left(problem));
		}
	}
}
