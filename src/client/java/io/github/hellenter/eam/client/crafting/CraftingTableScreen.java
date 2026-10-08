package io.github.hellenter.eam.client.crafting;

import io.github.hellenter.eam.world.crafting.CraftingTableMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** The vanilla crafting screen with the tool slot under the arrow and the craft progress on the arrow. */
public class CraftingTableScreen extends CraftingScreen {
	private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
	private static final Component TOOL_HINT = Component.translatable("gui.eam.crafting_table.tool");

	public CraftingTableScreen(CraftingTableMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE,
				leftPos + CraftingTableMenu.TOOL_X - 1, topPos + CraftingTableMenu.TOOL_Y - 1, 18, 18);
		CraftProgress.draw(graphics, menu, CraftProgress.TABLE, leftPos, topPos);
	}

	@Override
	protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractTooltip(graphics, mouseX, mouseY);
		if (hoveredSlot != null && hoveredSlot.index == CraftingTableMenu.TOOL_SLOT && !hoveredSlot.hasItem()
				&& menu.getCarried().isEmpty()) {
			graphics.setTooltipForNextFrame(TOOL_HINT, mouseX, mouseY);
		}
	}
}
