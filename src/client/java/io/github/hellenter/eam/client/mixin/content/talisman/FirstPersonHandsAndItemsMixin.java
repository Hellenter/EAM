package io.github.hellenter.eam.client.mixin.content.talisman;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import io.github.hellenter.eam.client.content.talisman.TalismanActivation;

import net.minecraft.client.player.FirstPersonHandsAndItems;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** While the talisman flies out to a portal, the hand that held it lowers empty, and raises it again when it returns. */
@Mixin(FirstPersonHandsAndItems.class)
public abstract class FirstPersonHandsAndItemsMixin {
	@ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/player/LocalPlayer;getMainHandItem()Lnet/minecraft/world/item/ItemStack;"))
	private ItemStack eam$hideFlyingTalismanMain(ItemStack stack, @Local(argsOnly = true) LocalPlayer player) {
		return TalismanActivation.inHand(player, stack);
	}

	@ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/player/LocalPlayer;getOffhandItem()Lnet/minecraft/world/item/ItemStack;"))
	private ItemStack eam$hideFlyingTalismanOff(ItemStack stack, @Local(argsOnly = true) LocalPlayer player) {
		return TalismanActivation.inHand(player, stack);
	}
}
