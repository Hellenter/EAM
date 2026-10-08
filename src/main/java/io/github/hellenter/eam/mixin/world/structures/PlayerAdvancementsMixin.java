package io.github.hellenter.eam.mixin.world.structures;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import io.github.hellenter.eam.world.structures.VillageModule;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Fabric API has no advancement event; rewards are granted exactly once, when an advancement is completed. */
@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementsMixin {
	@WrapOperation(method = "award", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/advancements/AdvancementRewards;grant(Lnet/minecraft/server/level/ServerPlayer;)V"))
	private void eam$advancementDone(AdvancementRewards rewards, ServerPlayer player, Operation<Void> original,
			@Local(argsOnly = true) AdvancementHolder advancement) {
		original.call(rewards, player);
		VillageModule.onAdvancementDone(player, advancement);
	}
}
