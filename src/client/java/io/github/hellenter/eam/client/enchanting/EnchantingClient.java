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

/** Adds the enchantment choice panel to the vanilla enchanting screen when the server has the mechanic on. */
public final class EnchantingClient {
	private static final int GAP = 2;
	private static final int IMAGE_WIDTH = 176;

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
			// Right of the table if it fits, else left of it.
			int x = container.eam$getLeftPos() + IMAGE_WIDTH + GAP;
			if (x + EnchantChoicePanel.WIDTH > width) {
				x = Math.max(0, container.eam$getLeftPos() - EnchantChoicePanel.WIDTH - GAP);
			}
			EnchantChoicePanel panel = new EnchantChoicePanel(x, container.eam$getTopPos(), enchanting.getMenu());
			Screens.getWidgets(screen).add(panel);
			ScreenMouseEvents.allowMouseScroll(screen).register((s, mouseX, mouseY, scrollX, scrollY) -> {
				if (panel.isMouseOver(mouseX, mouseY)) {
					panel.scroll(scrollY);
					return false;
				}
				return true;
			});
		});
	}
}
