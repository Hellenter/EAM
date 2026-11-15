package io.github.hellenter.eam.client.mixin.core;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import io.github.hellenter.eam.client.link.HandshakeClient;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** On a server without EAM, items keep their vanilla components (stack sizes, tools, food). */
@Mixin(Holder.Reference.class)
abstract class HolderReferenceComponentsMixin {
	@ModifyReturnValue(method = "components", at = @At("RETURN"))
	private DataComponentMap eam$vanillaOnVanillaServer(DataComponentMap original) {
		return HandshakeClient.components((Holder.Reference<?>) (Object) this, original);
	}
}
