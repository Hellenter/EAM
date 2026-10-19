package io.github.hellenter.eam.client.mixin.content.longbow;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import io.github.hellenter.eam.client.content.longbow.LongbowHands;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** The view narrows while a longbow is drawn, as with a bow. */
@Mixin(AbstractClientPlayer.class)
abstract class AbstractClientPlayerLongbowMixin {
	@WrapOperation(method = "getFieldOfViewModifier", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/item/ItemStack;is(Ljava/lang/Object;)Z"))
	private boolean eam$longbowIsBow(ItemStack stack, Object item, Operation<Boolean> original) {
		return original.call(stack, item) || LongbowHands.countsAsBow(stack, item);
	}
}
