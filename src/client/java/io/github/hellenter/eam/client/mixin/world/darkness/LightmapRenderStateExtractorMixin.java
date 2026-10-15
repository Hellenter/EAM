package io.github.hellenter.eam.client.mixin.world.darkness;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import io.github.hellenter.eam.world.darkness.DarknessLogic;
import io.github.hellenter.eam.world.darkness.DarknessModule;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LightmapRenderStateExtractor.class)
abstract class LightmapRenderStateExtractorMixin {
	/** The brightness setting is capped by the eam:visual/max_brightness attribute where the camera is. */
	@ModifyExpressionValue(method = "extract", at = @At(value = "INVOKE", target = "Ljava/lang/Double;floatValue()F", ordinal = 0))
	private float eam$capBrightness(float brightness, @Local Camera camera, @Local(argsOnly = true) float partialTicks) {
		return DarknessLogic.capBrightness(brightness, camera.attributeProbe().getValue(DarknessModule.MAX_BRIGHTNESS, partialTicks));
	}
}
