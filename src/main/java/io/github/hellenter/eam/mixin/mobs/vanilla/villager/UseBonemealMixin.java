package io.github.hellenter.eam.mixin.mobs.vanilla.villager;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import io.github.hellenter.eam.mobs.vanilla.villager.VillagerGearModule;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.behavior.UseBonemeal;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** A farmer using bone meal puts what he holds aside instead of throwing it away. */
@Mixin(UseBonemeal.class)
abstract class UseBonemealMixin {
	@WrapOperation(method = {
			"start(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/npc/villager/Villager;J)V",
			"stop(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/npc/villager/Villager;J)V"},
			at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/npc/villager/Villager;setItemSlot(Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/item/ItemStack;)V"))
	private void eam$showAside(Villager villager, EquipmentSlot slot, ItemStack stack, Operation<Void> original) {
		VillagerGearModule.showInHand(villager, stack);
	}
}
