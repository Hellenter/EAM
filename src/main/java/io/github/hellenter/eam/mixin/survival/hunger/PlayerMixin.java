package io.github.hellenter.eam.mixin.survival.hunger;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import io.github.hellenter.eam.survival.hunger.HungerModule;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
abstract class PlayerMixin {
	@ModifyExpressionValue(method = "canEat", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;needsFood()Z"))
	private boolean eam$needsFood(boolean original) {
		return HungerModule.needsFood((Player) (Object) this, original);
	}

	@ModifyExpressionValue(method = "hasEnoughFoodToDoExhaustiveManoeuvres", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;hasEnoughFood()Z"))
	private boolean eam$canSprint(boolean original) {
		return HungerModule.canSprint((Player) (Object) this, original);
	}
}
