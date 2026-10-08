package io.github.hellenter.eam.mixin.world.forging;

import io.github.hellenter.eam.world.forging.ForgingModule;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hot iron in the anvil's left slot forges into iron plates for no experience; the rules are in {@link ForgingModule}. */
@Mixin(AnvilMenu.class)
abstract class AnvilMenuMixin extends ItemCombinerMenu {
	@Shadow
	@Final
	private DataSlot cost;

	private AnvilMenuMixin(MenuType<?> type, int containerId, Inventory inventory, ContainerLevelAccess access,
			ItemCombinerMenuSlotDefinition slots) {
		super(type, containerId, inventory, access, slots);
	}

	@Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
	private void eam$forgePlates(CallbackInfo ci) {
		ItemStack plates = eam$plates();
		if (!plates.isEmpty()) {
			this.resultSlots.setItem(0, plates);
			this.cost.set(0);
			ci.cancel();
		}
	}

	/** Vanilla only lets a result with an experience cost be taken; forging costs none. */
	@ModifyReturnValue(method = "mayPickup", at = @At("RETURN"))
	private boolean eam$mayTakePlates(boolean original) {
		return original || !eam$plates().isEmpty();
	}

	private ItemStack eam$plates() {
		return ForgingModule.anvilResult(this.inputSlots.getItem(0), this.inputSlots.getItem(1), this.player.level().getGameTime());
	}
}
