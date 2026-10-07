package io.github.hellenter.eam.mixin.survival.thirst;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import io.github.hellenter.eam.survival.thirst.ThirstModule;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
abstract class PlayerThirstMixin {
	/** Food exhaustion from activity also drains thirst. Vanilla only reaches this call on the server. */
	@WrapOperation(
			method = "causeFoodExhaustion",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;addExhaustion(F)V")
	)
	private void eam$addThirstExhaustion(FoodData foodData, float amount, Operation<Void> original) {
		original.call(foodData, amount);
		ThirstModule.addExhaustion((Player) (Object) this, amount);
	}

	/** Sprinting needs water as well as food. */
	@ModifyReturnValue(method = "hasEnoughFoodToDoExhaustiveManoeuvres", at = @At("RETURN"))
	private boolean eam$requireWater(boolean original) {
		return original && ThirstModule.canSprint((Player) (Object) this);
	}
}
