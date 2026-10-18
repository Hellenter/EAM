package io.github.hellenter.eam.mixin.survival.death;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import io.github.hellenter.eam.survival.death.DeathModule;

import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
abstract class PlayerDeathMixin {
	/** A player killed by a boss keeps their things when the game rule says so. */
	@ModifyExpressionValue(method = "dropEquipment", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/gamerules/GameRules;get(Lnet/minecraft/world/level/gamerules/GameRule;)Ljava/lang/Object;"))
	private Object eam$keepOnBossDeath(Object keepInventory) {
		return DeathModule.keepsInventory((Player) (Object) this, (Boolean) keepInventory);
	}

	/** A dead player leaves no experience orbs: the lost levels are simply gone. */
	@ModifyReturnValue(method = "getBaseExperienceReward", at = @At("RETURN"))
	private int eam$noExperienceOrbs(int original) {
		return DeathModule.experienceReward(original);
	}
}
