package io.github.hellenter.eam.mixin.content.talisman;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import io.github.hellenter.eam.content.talisman.TalismanModule;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Fire placed in a portal frame (flint and steel, fire charges, spreading fire, lava) no longer lights it in the overworld. */
@Mixin(BaseFireBlock.class)
public abstract class BaseFireBlockPortalMixin {
	@WrapOperation(method = "onPlace", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/BaseFireBlock;inPortalDimension(Lnet/minecraft/world/level/Level;)Z"))
	private boolean eam$onlyTalismanLightsPortal(Level level, Operation<Boolean> original) {
		return original.call(level) && TalismanModule.fireLightsPortal(level);
	}
}
