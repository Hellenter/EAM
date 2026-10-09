package io.github.hellenter.eam.client.mixin.world.bloodmoon;

import io.github.hellenter.eam.client.world.bloodmoon.BloodMoonSky;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.world.level.MoonPhase;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyRenderer.class)
abstract class SkyRendererMixin {
	/** A blood moon is a full moon in a red sky. */
	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void eam$bloodMoon(ClientLevel level, float partialTicks, Camera camera, SkyRenderState state, CallbackInfo ci) {
		if (BloodMoonSky.activeIn(level)) {
			state.skyColor = BloodMoonSky.skyColor(state.skyColor);
			state.moonPhase = MoonPhase.FULL_MOON;
		}
	}
}
