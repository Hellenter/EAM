package io.github.hellenter.eam.mixin.world.crafting;

import io.github.hellenter.eam.world.crafting.CraftTimer;
import io.github.hellenter.eam.world.crafting.TimedCraftingMenu;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Every crafting menu carries a craft timer, synced to the client as two data slots. */
@Mixin(AbstractCraftingMenu.class)
public abstract class AbstractCraftingMenuMixin implements TimedCraftingMenu {
	@Unique
	private CraftTimer eam$craftTimer;

	@Shadow
	protected abstract Player owner();

	@Inject(method = "<init>", at = @At("TAIL"))
	private void eam$addTimer(MenuType<?> menuType, int containerId, int width, int height, CallbackInfo ci) {
		eam$craftTimer = new CraftTimer(this::owner);
		eam$addDataSlot(eam$craftTimer.progressSlot());
		eam$addDataSlot(eam$craftTimer.totalSlot());
	}

	@Unique
	private void eam$addDataSlot(DataSlot slot) {
		((AbstractContainerMenuAccessor) this).eam$addDataSlot(slot);
	}

	@Override
	public CraftTimer eam$craftTimer() {
		return eam$craftTimer;
	}
}
