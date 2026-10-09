package io.github.hellenter.eam.client.mixin.world.bloodmoon;

import com.llamalad7.mixinextras.sugar.Local;

import io.github.hellenter.eam.client.world.bloodmoon.BloodMoonSky;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogRenderer;

import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogRenderer.class)
abstract class FogRendererMixin {
	/** The horizon turns red under a blood moon. */
	@Inject(method = "computeFogColor", at = @At("TAIL"))
	private void eam$bloodMoon(CallbackInfo ci, @Local(argsOnly = true) ClientLevel level, @Local(argsOnly = true) Vector4f color) {
		if (BloodMoonSky.activeIn(level)) {
			BloodMoonSky.tintFog(color);
		}
	}
}
