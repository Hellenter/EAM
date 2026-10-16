package io.github.hellenter.eam.client.mixin.mobs.villager;

import io.github.hellenter.eam.client.mobs.villager.VillagerTradePanelScreen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The villager gear panel beside the trades; drawing and rules are in {@link VillagerTradePanelScreen}. */
@Mixin(MerchantScreen.class)
abstract class MerchantScreenMixin extends AbstractContainerScreen<MerchantMenu> {
	@Unique
	private Button eam$inventoryButton;

	private MerchantScreenMixin(MerchantMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void eam$addInventoryButton(CallbackInfo ci) {
		eam$inventoryButton = addRenderableWidget(VillagerTradePanelScreen.button(leftPos, topPos));
	}

	@Inject(method = "extractBackground", at = @At("TAIL"))
	private void eam$extractPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		VillagerTradePanelScreen.extract(menu, graphics, leftPos, topPos, mouseX, mouseY, eam$inventoryButton);
	}

	@Override
	protected boolean hasClickedOutside(double mx, double my, int xo, int yo) {
		return super.hasClickedOutside(mx, my, xo, yo) && !VillagerTradePanelScreen.inPanel(menu, mx, my, leftPos, topPos);
	}
}
