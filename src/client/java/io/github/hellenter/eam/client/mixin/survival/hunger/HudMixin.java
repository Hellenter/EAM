package io.github.hellenter.eam.client.mixin.survival.hunger;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import io.github.hellenter.eam.survival.hunger.HungerModule;

import net.minecraft.client.gui.Hud;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Hud.class)
abstract class HudMixin {
	/** Draws only as many shanks as the player's food bar holds. */
	@ModifyExpressionValue(method = "extractFood", at = @At(value = "CONSTANT", args = "intValue=10"))
	private int eam$shanks(int original, @Local(argsOnly = true) Player player) {
		return HungerModule.shanks(player, original);
	}
}
