package io.github.hellenter.eam.mixin.mobs.entity.leshy;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import io.github.hellenter.eam.mobs.entity.leshy.LeshyModule;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** The marker of a player led astray by a leshy whirls on every copy of the map. */
@Mixin(MapItemSavedData.class)
abstract class MapItemSavedDataLeshyMixin {
	@WrapOperation(method = "tickCarriedBy", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getYRot()F"))
	private float eam$bewilderedYaw(Player player, Operation<Float> original) {
		return LeshyModule.mapYaw(player, original.call(player));
	}
}
