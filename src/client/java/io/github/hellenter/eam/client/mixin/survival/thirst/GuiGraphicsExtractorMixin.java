package io.github.hellenter.eam.client.mixin.survival.thirst;

import io.github.hellenter.eam.client.survival.thirst.FlaskWaterBar;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphicsExtractor.class)
abstract class GuiGraphicsExtractorMixin {
	/** Adds the flask water bar above the icon; vanilla has a single bar slot below it. */
	@Inject(method = "itemBar", at = @At("TAIL"))
	private void eam$flaskWaterBar(ItemStack stack, int x, int y, CallbackInfo ci) {
		FlaskWaterBar.extract((GuiGraphicsExtractor) (Object) this, stack, x, y);
	}
}
