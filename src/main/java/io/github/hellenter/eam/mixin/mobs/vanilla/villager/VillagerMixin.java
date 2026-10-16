package io.github.hellenter.eam.mixin.mobs.vanilla.villager;

import io.github.hellenter.eam.mobs.vanilla.villager.VillagerGearModule;

import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A villager who takes up a trade gets its work item. */
@Mixin(Villager.class)
abstract class VillagerMixin {
	@Inject(method = "setVillagerData", at = @At("TAIL"))
	private void eam$takeWorkItem(VillagerData data, CallbackInfo ci) {
		VillagerGearModule.onVillagerDataChanged((Villager) (Object) this);
	}
}
