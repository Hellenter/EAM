package io.github.hellenter.eam.client.enchanting;

import io.github.hellenter.eam.client.mixin.survival.hunger.AbstractContainerScreenAccessor;
import io.github.hellenter.eam.world.enchanting.EnchantingSettings;
import io.github.hellenter.eam.world.enchanting.EnchantingSettingsPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;

import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;

import org.jspecify.annotations.Nullable;

/** Adds the enchantment list and the summary of picked enchantments to the vanilla enchanting screen when the server has the mechanic on. */
public final class EnchantingClient {
	private static final int GAP = 2;
	private static final int IMAGE_WIDTH = 176;
	private static final int MIN_WIDTH = 112;
	private static final int MAX_WIDTH = 150;

	/** The server's prices; null until it sends them, so the panel stays hidden on servers with the mechanic off. */
	private static @Nullable EnchantingSettings settings;

	private EnchantingClient() {
	}

	static @Nullable EnchantingSettings settings() {
		return settings;
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(EnchantingSettingsPayload.TYPE, (payload, context) -> settings = payload.settings());
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> settings = null);
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (!(screen instanceof EnchantmentScreen enchanting) || settings == null) {
				return;
			}
			AbstractContainerScreenAccessor container = (AbstractContainerScreenAccessor) screen;
			// The list goes left of the table and the summary right of it, each as wide as the space beside the table.
			int left = container.eam$getLeftPos();
			int top = container.eam$getTopPos();
			int panelWidth = Math.clamp((width - IMAGE_WIDTH) / 2 - 2 * GAP, MIN_WIDTH, MAX_WIDTH);
			EnchantSelection selection = new EnchantSelection();
			EnchantListPanel list = new EnchantListPanel(Math.max(0, left - GAP - panelWidth), top, panelWidth, enchanting.getMenu(), selection);
			EnchantSummaryPanel summary = new EnchantSummaryPanel(Math.min(width - panelWidth, left + IMAGE_WIDTH + GAP), top, panelWidth,
					enchanting.getMenu(), selection);
			Screens.getWidgets(screen).add(list);
			Screens.getWidgets(screen).add(summary);
			ScreenMouseEvents.allowMouseScroll(screen).register((s, mouseX, mouseY, scrollX, scrollY) -> {
				if (list.isMouseOver(mouseX, mouseY)) {
					list.scroll(scrollY);
					return false;
				}
				return true;
			});
		});
	}
}
