package io.github.hellenter.eam.mixin.world.forging;

import io.github.hellenter.eam.world.forging.AnvilForge;
import io.github.hellenter.eam.world.forging.AnvilHammer;
import io.github.hellenter.eam.world.forging.AnvilRepair;
import io.github.hellenter.eam.world.forging.ForgingAnvilMenu;
import io.github.hellenter.eam.world.forging.ForgingModule;
import io.github.hellenter.eam.world.forging.HammerSlot;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hot iron in the anvil's left and right slots is hammered into iron plates, taking time and no experience; a hammer
 * slot speeds that up, and copper and silver anvils refuse items above their tier. The rules are in {@link ForgingModule}.
 */
@Mixin(AnvilMenu.class)
abstract class AnvilMenuMixin extends ItemCombinerMenu implements ForgingAnvilMenu {
	@Shadow
	@Final
	private DataSlot cost;

	@Shadow
	private int repairItemCountCost;

	@Shadow
	private @Nullable String itemName;

	@Shadow
	private boolean onlyRenaming;

	@Unique
	private AnvilForge eam$forge;

	@Unique
	private AnvilHammer eam$hammer;

	private AnvilMenuMixin(MenuType<?> type, int containerId, Inventory inventory, ContainerLevelAccess access,
			ItemCombinerMenuSlotDefinition slots) {
		super(type, containerId, inventory, access, slots);
	}

	@Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
			at = @At("TAIL"))
	private void eam$addForge(int containerId, Inventory inventory, ContainerLevelAccess access, CallbackInfo ci) {
		eam$hammer = new AnvilHammer(access);
		eam$forge = new AnvilForge(() -> this.player, () -> ForgingModule.anvilTier(this));
		addDataSlot(eam$forge.progressSlot());
		addDataSlot(eam$forge.flagsSlot());
		addDataSlot(eam$forge.totalSlot());
		addDataSlot(eam$forge.anvilTierSlot());
		addSlot(new HammerSlot(eam$hammer));
	}

	@Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
	private void eam$forgePlates(CallbackInfo ci) {
		if (ForgingModule.updateResult(this, this.player)) {
			this.cost.set(0);
			ci.cancel();
		} else if (ForgingModule.tooWeak(this)) {
			this.resultSlots.setItem(0, ItemStack.EMPTY);
			this.cost.set(0);
			ci.cancel();
		} else {
			int used = AnvilRepair.update(this, this.player, this.itemName);
			if (used >= 0) {
				this.repairItemCountCost = used;
				this.onlyRenaming = false;
				this.cost.set(0);
				ci.cancel();
			}
		}
	}

	/** Vanilla only lets a result with an experience cost be taken; forged plates and repairs cost none. */
	@ModifyReturnValue(method = "mayPickup", at = @At("RETURN"))
	private boolean eam$mayTakePlates(boolean original) {
		return original || ForgingModule.mayTakeFree(this);
	}

	@Inject(method = "onTake", at = @At("HEAD"), cancellable = true)
	private void eam$takePlates(Player player, ItemStack carried, CallbackInfo ci) {
		if (ForgingModule.takeOutput(this, player)) {
			ci.cancel();
		}
	}

	@Override
	public void clicked(int slotIndex, int buttonNum, ContainerInput input, Player player) {
		if (!ForgingModule.clickResult(this, slotIndex, getResultSlot(), input, player)) {
			super.clicked(slotIndex, buttonNum, input, player);
		}
	}

	@Override
	public void removed(Player player) {
		ForgingModule.closed(this, player);
		super.removed(player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		ItemStack moved = ForgingModule.quickMoveHammer((AnvilMenu) (Object) this, slotIndex);
		return moved != null ? moved : super.quickMoveStack(player, slotIndex);
	}

	@Override
	public AnvilHammer eam$hammer() {
		return eam$hammer;
	}

	@Override
	public AnvilForge eam$forge() {
		return eam$forge;
	}

	@Override
	public Container eam$inputSlots() {
		return this.inputSlots;
	}

	@Override
	public ResultContainer eam$resultSlots() {
		return this.resultSlots;
	}

	@Override
	public ContainerLevelAccess eam$access() {
		return this.access;
	}
}
