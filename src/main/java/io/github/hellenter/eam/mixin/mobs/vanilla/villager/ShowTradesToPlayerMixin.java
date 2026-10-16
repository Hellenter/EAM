package io.github.hellenter.eam.mixin.mobs.vanilla.villager;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import io.github.hellenter.eam.mobs.vanilla.villager.VillagerGearModule;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.behavior.ShowTradesToPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** A villager showing a trade puts what he holds aside instead of throwing it away. */
@Mixin(ShowTradesToPlayer.class)
abstract class ShowTradesToPlayerMixin {
	@WrapOperation(method = {"clearHeldItem", "displayAsHeldItem"}, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/npc/villager/Villager;setItemSlot(Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/item/ItemStack;)V"))
	private static void eam$showAside(Villager villager, EquipmentSlot slot, ItemStack stack, Operation<Void> original) {
		VillagerGearModule.showInHand(villager, stack);
	}

	@WrapOperation(method = {"clearHeldItem", "displayAsHeldItem"}, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/npc/villager/Villager;setDropChance(Lnet/minecraft/world/entity/EquipmentSlot;F)V"))
	private static void eam$keepDropChance(Villager villager, EquipmentSlot slot, float chance, Operation<Void> original) {
		if (VillagerGearModule.keepsVanillaDropChance()) original.call(villager, slot, chance);
	}
}
