package io.github.hellenter.eam.mixin.world.enchanting;

import io.github.hellenter.eam.world.enchanting.EnchantingShelves;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.MenuType;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Counts the bookshelves around the table when the menu opens and whenever the item changes, and syncs the count to the client. */
@Mixin(EnchantmentMenu.class)
abstract class EnchantmentMenuShelvesMixin extends AbstractContainerMenu implements EnchantingShelves.Counted {
	@Shadow
	@Final
	private ContainerLevelAccess access;

	@Unique
	private final DataSlot eam$shelves = DataSlot.standalone();

	private EnchantmentMenuShelvesMixin(MenuType<?> type, int containerId) {
		super(type, containerId);
	}

	@Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At("TAIL"))
	private void eam$trackShelves(int containerId, Inventory inventory, ContainerLevelAccess access, CallbackInfo ci) {
		addDataSlot(eam$shelves);
		eam$countShelves();
	}

	@Inject(method = "slotsChanged", at = @At("TAIL"))
	private void eam$recountShelves(Container container, CallbackInfo ci) {
		eam$countShelves();
	}

	@Unique
	private void eam$countShelves() {
		access.execute((level, pos) -> eam$shelves.set(EnchantingShelves.count(level, pos)));
	}

	@Override
	public int eam$shelves() {
		return eam$shelves.get();
	}
}
