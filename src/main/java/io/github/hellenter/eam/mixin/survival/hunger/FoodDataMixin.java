package io.github.hellenter.eam.mixin.survival.hunger;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import io.github.hellenter.eam.survival.hunger.HungerModule;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;

import org.spongepowered.asm.mixin.Mixin;

@Mixin(FoodData.class)
abstract class FoodDataMixin {
	@WrapMethod(method = "tick")
	private void eam$tick(ServerPlayer player, Operation<Void> original) {
		if (HungerModule.enabled()) {
			HungerModule.tick((FoodData) (Object) this, player);
		} else {
			original.call(player);
		}
	}
}
