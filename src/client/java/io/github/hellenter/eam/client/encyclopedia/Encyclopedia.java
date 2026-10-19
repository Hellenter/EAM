package io.github.hellenter.eam.client.encyclopedia;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.client.mixin.survival.hunger.AbstractContainerScreenAccessor;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

/**
 * Opens the encyclopedia: with its own key (J by default, in the EAM section of the controls) or the book button next
 * to the recipe book button in the survival inventory.
 */
public final class Encyclopedia {
	public static final KeyMapping KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.eam.encyclopedia",
			InputConstants.KEY_J, KeyMapping.Category.register(Eam.id("eam"))));
	private static final WidgetSprites BUTTON = new WidgetSprites(Eam.id("encyclopedia/button"),
			Eam.id("encyclopedia/button_highlighted"));
	/** The button's place in the inventory, just right of the recipe book button. */
	private static final int BUTTON_X = 126;
	private static final int BUTTON_Y = 61;

	private Encyclopedia() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (KEY.consumeClick()) {
				if (client.gui.screen() == null && client.player != null) client.gui.setScreen(new EncyclopediaScreen(null));
			}
		});
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (!(screen instanceof InventoryScreen inventory)) return;
			AbstractContainerScreenAccessor container = (AbstractContainerScreenAccessor) inventory;
			ImageButton button = new ImageButton(0, 0, 20, 18, BUTTON,
					pressed -> client.gui.setScreen(new EncyclopediaScreen(inventory)),
					Component.translatable("gui.eam.encyclopedia.title"));
			button.setTooltip(Tooltip.create(Component.translatable("gui.eam.encyclopedia.open", KEY.getTranslatedKeyMessage())));
			Screens.getWidgets(inventory).add(button);
			// The recipe book moves the inventory sideways without a new init, so the button follows it every frame.
			ScreenEvents.beforeExtract(inventory).register((s, graphics, mouseX, mouseY, partialTick) ->
					button.setPosition(container.eam$getLeftPos() + BUTTON_X, container.eam$getTopPos() + BUTTON_Y));
		});
	}
}
