package io.github.hellenter.eam.client.content.lockedchest;

import io.github.hellenter.eam.content.lockedchest.LockedChestMenu;
import io.github.hellenter.eam.content.lockedchest.LockedChestOwnerNamePayload;
import io.github.hellenter.eam.content.lockedchest.LockedChestOwnerPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;

/**
 * A locked chest on the vanilla chest background, with whose it is beside it and, for the player who placed it, a button
 * that gives it to the next villager near it or to the village.
 */
public class LockedChestScreen extends ContainerScreen {
	private static final int OWNER_COLOR = 0xFFFFFFFF;
	private final LockedChestMenu chest;

	/** Takes a chest menu, as the vanilla chest screen does; the menu type it opens for is always a locked chest's. */
	public LockedChestScreen(ChestMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		this.chest = (LockedChestMenu) menu;
	}

	/** Shows the new owner when the server says the button changed it. */
	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(LockedChestOwnerNamePayload.TYPE, (payload, context) -> {
			if (context.player().containerMenu instanceof LockedChestMenu menu && menu.containerId == payload.containerId()) {
				menu.setOwnerName(payload.owner());
			}
		});
	}

	@Override
	protected void init() {
		super.init();
		if (!chest.data().canChange()) return;
		Component label = Component.translatable("container.eam.locked_chest.change");
		addRenderableWidget(Button.builder(label, button -> ClientPlayNetworking.send(LockedChestOwnerPayload.INSTANCE))
				.bounds(leftPos + imageWidth + 2, topPos + 18, font.width(label) + 12, 20)
				.build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);
		Component owner = Component.translatable("container.eam.locked_chest.owner", chest.data().owner());
		graphics.text(font, owner, leftPos + imageWidth + 4, topPos + 6, OWNER_COLOR, true);
	}
}
