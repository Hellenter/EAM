package io.github.hellenter.eam.mixin.mobs.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;

import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLordLogic;
import io.github.hellenter.eam.mobs.entity.fallenlord.FallenLordModule;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Part of the Fallen Lord's damage skips the player's armor and Protection. */
@Mixin(Player.class)
abstract class PlayerArmorPiercingMixin {
	@WrapOperation(method = "actuallyHurt", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/player/Player;getDamageAfterArmorAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"))
	private float eam$armorPiercing(Player player, DamageSource source, float damage, Operation<Float> original,
			@Share("pierced") LocalFloatRef pierced) {
		float part = FallenLordLogic.piercingPart(damage, FallenLordModule.armorPiercing(source));
		pierced.set(part);
		return original.call(player, source, damage - part);
	}

	@WrapOperation(method = "actuallyHurt", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/player/Player;getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"))
	private float eam$addPierced(Player player, DamageSource source, float damage, Operation<Float> original,
			@Share("pierced") LocalFloatRef pierced) {
		return original.call(player, source, damage) + FallenLordModule.piercedAfterResistance(player, source, pierced.get());
	}
}
