package io.github.hellenter.eam.mixin.mobs.vanilla.villager;

import io.github.hellenter.eam.mobs.vanilla.villager.TradePanel;
import io.github.hellenter.eam.mobs.vanilla.villager.TradePanelMenu;
import io.github.hellenter.eam.mobs.vanilla.villager.VillagerTradePanel;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adds the villager gear panel's slots to the trade menu; see {@link VillagerTradePanel}. */
@Mixin(MerchantMenu.class)
abstract class MerchantMenuMixin implements TradePanelMenu {
	@Shadow
	@Final
	private Merchant trader;

	@Unique
	private @Nullable TradePanel eam$panel;

	@Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/item/trading/Merchant;)V", at = @At("TAIL"))
	private void eam$addGearSlots(int containerId, Inventory inventory, Merchant merchant, CallbackInfo ci) {
		VillagerTradePanel.addSlots((MerchantMenu) (Object) this, merchant);
	}

	@Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
	private void eam$quickMoveGear(Player player, int slotIndex, CallbackInfoReturnable<ItemStack> cir) {
		if (VillagerTradePanel.isPanelSlot(slotIndex)) cir.setReturnValue(VillagerTradePanel.quickMove((MerchantMenu) (Object) this, player, slotIndex));
	}

	@Override
	public @Nullable TradePanel eam$panel() {
		return eam$panel;
	}

	@Override
	public void eam$setPanel(TradePanel panel) {
		eam$panel = panel;
	}

	@Override
	public Merchant eam$trader() {
		return trader;
	}
}
