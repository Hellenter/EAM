package io.github.hellenter.eam.mixin.dimension;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import io.github.hellenter.eam.dimension.UnderworldModule;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Fire lights nether portals in the underworld too, as in the overworld and the nether. */
@Mixin(BaseFireBlock.class)
public abstract class BaseFireBlockMixin {
	@ModifyReturnValue(method = "inPortalDimension", at = @At("RETURN"))
	private static boolean eam$underworldPortals(boolean original, Level level) {
		return original || UnderworldModule.portalsWork(level);
	}
}
