package io.github.hellenter.eam.mixin.mobs.vanilla.villager;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.villager.Villager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Villagers get attack damage, a fist's, so a guard's weapon adds to it. */
@Mixin(Villager.class)
abstract class VillagerAttributesMixin {
	@ModifyReturnValue(method = "createAttributes", at = @At("RETURN"))
	private static AttributeSupplier.Builder eam$addAttackDamage(AttributeSupplier.Builder builder) {
		return builder.add(Attributes.ATTACK_DAMAGE, 1.0);
	}
}
