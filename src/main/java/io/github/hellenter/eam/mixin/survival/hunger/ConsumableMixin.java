package io.github.hellenter.eam.mixin.survival.hunger;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import io.github.hellenter.eam.survival.hunger.HungerModule;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Consumable.class)
abstract class ConsumableMixin {
	@ModifyReturnValue(method = "canConsume", at = @At("RETURN"))
	private boolean eam$fixesDeficiency(boolean original, LivingEntity user, ItemStack stack) {
		return original || HungerModule.fixesDeficiency(user, stack);
	}
}
