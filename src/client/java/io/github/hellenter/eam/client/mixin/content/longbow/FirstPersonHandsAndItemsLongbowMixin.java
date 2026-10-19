package io.github.hellenter.eam.client.mixin.content.longbow;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import io.github.hellenter.eam.client.content.longbow.LongbowHands;

import net.minecraft.client.player.FirstPersonHandsAndItems;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** A drawn longbow takes both hands like a bow: the other hand is not drawn. */
@Mixin(FirstPersonHandsAndItems.class)
abstract class FirstPersonHandsAndItemsLongbowMixin {
	@WrapOperation(method = "evaluateWhichHandsToRender", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/item/ItemStack;is(Ljava/lang/Object;)Z"))
	private static boolean eam$longbowIsBow(ItemStack stack, Object item, Operation<Boolean> original) {
		return original.call(stack, item) || LongbowHands.countsAsBow(stack, item);
	}
}
