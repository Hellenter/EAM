package io.github.hellenter.eam.client.mixin.world.forging;

import io.github.hellenter.eam.client.forging.AnvilProgress;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws the hammer slot's frame with the background, under the slot's item. */
@Mixin(AnvilScreen.class)
abstract class AnvilScreenMixin {
	@Inject(method = "extractBackground", at = @At("TAIL"))
	private void eam$hammerSlot(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		AnvilProgress.drawHammerSlot((AnvilScreen) (Object) this, graphics);
	}
}
