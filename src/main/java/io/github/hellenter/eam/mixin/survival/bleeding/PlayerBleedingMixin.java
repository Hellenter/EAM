package io.github.hellenter.eam.mixin.survival.bleeding;

import io.github.hellenter.eam.survival.bleeding.BleedingModule;

import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Player.class)
abstract class PlayerBleedingMixin {
	/** A bleeding player tires faster: activity costs more food and, through the thirst hook, more water. */
	@ModifyVariable(method = "causeFoodExhaustion", at = @At("HEAD"), argsOnly = true)
	private float eam$bleedingExhaustion(float amount) {
		return BleedingModule.activityExhaustion((Player) (Object) this, amount);
	}
}
