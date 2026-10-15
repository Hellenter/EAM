package io.github.hellenter.eam.client.content.fridge;

import io.github.hellenter.eam.content.fridge.FridgeMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;

/**
 * A fridge's inventory on the vanilla chest background: the food rows, then a row with the ice slots on the right, tinted
 * icy blue, and how long the ice keeps the fridge cold on the left.
 */
public class FridgeScreen extends AbstractContainerScreen<FridgeMenu> {
	private static final Identifier CONTAINER_BACKGROUND = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
	private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
	private static final int BODY = 0xFFC6C6C6;
	private static final int ICE_TINT = 0x5582B4E6;
	private static final int TEXT_COLOR = 0xFF404040;
	private static final int WARM_COLOR = 0xFFA02020;
	private static final int SLOT = 18;

	public FridgeScreen(FridgeMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, menu.inventoryTop() + 83);
		inventoryLabelY = imageHeight - 94;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		int top = menu.topHeight();
		graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND, leftPos, topPos, 0.0F, 0.0F, imageWidth, top, 256, 256);
		graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND, leftPos, topPos + top, 0.0F, 126.0F, imageWidth, 96, 256, 256);
		// The chest's frames under the ice row's free part are painted over; the ice slots get their own.
		int iceY = topPos + menu.iceY() - 1;
		graphics.fill(leftPos + 7, iceY, leftPos + menu.iceX(0) - 1, iceY + SLOT, BODY);
		for (int i = 0; i < menu.size().ice(); i++) {
			int x = leftPos + menu.iceX(i) - 1;
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, x, iceY, SLOT, SLOT);
			graphics.fill(x + 1, iceY + 1, x + SLOT - 1, iceY + SLOT - 1, ICE_TINT);
		}
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
		super.extractLabels(graphics, xm, ym);
		int hours = menu.hoursOfCold();
		Component cold = hours <= 0
				? Component.translatable("container.eam.fridge.no_ice")
				: Component.translatable("container.eam.fridge.cold", String.format(Locale.ROOT, "%.1f", hours / 24.0F));
		graphics.text(font, cold, 8, menu.iceY() + 5, hours <= 0 ? WARM_COLOR : TEXT_COLOR, false);
	}
}
