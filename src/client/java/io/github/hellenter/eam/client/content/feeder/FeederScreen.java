package io.github.hellenter.eam.client.content.feeder;

import io.github.hellenter.eam.content.feeder.FeederMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * A trough's inventory, drawn on the vanilla chest background: as many rows as the trough needs, the chest's slot
 * frames painted over and the trough's own slots drawn where its layout puts them. Shows the meals given today.
 */
public class FeederScreen extends AbstractContainerScreen<FeederMenu> {
	private static final Identifier CONTAINER_BACKGROUND = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
	private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
	private static final int BODY = 0xFFC6C6C6;
	private static final int TEXT_COLOR = 0xFF404040;
	private static final int SLOT = 18;

	public FeederScreen(FeederMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, 114 + menu.layout().rows() * SLOT);
		inventoryLabelY = imageHeight - 94;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		FeederMenu.Layout layout = menu.layout();
		int top = layout.topHeight();
		graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND, leftPos, topPos, 0.0F, 0.0F, imageWidth, top, 256, 256);
		graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND, leftPos, topPos + top, 0.0F, 126.0F, imageWidth, 96, 256, 256);
		graphics.fill(leftPos + 7, topPos + 17, leftPos + 7 + 9 * SLOT, topPos + top, BODY);
		for (int i = 0; i < layout.slots(); i++) {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, leftPos + layout.slotX(i) - 1, topPos + layout.slotY(i) - 1, SLOT, SLOT);
		}
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
		super.extractLabels(graphics, xm, ym);
		Component fed = Component.translatable("container.eam.feeder.fed", menu.mealsToday(), menu.heads());
		// On the inventory label line: a long trough title leaves no room beside it.
		graphics.text(font, fed, imageWidth - 8 - font.width(fed), inventoryLabelY, TEXT_COLOR, false);
	}
}
