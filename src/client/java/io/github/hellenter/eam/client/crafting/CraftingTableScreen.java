package io.github.hellenter.eam.client.crafting;

import io.github.hellenter.eam.world.crafting.CraftingTableMenu;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/** The vanilla crafting screen with the tool slots under the arrow and the craft progress on the arrow. */
public class CraftingTableScreen extends CraftingScreen {
	private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
	/** Hints for the empty knife, saw and hammer slots, in slot order. */
	private static final List<List<Component>> TOOL_HINTS = List.of(
			List.of(Component.translatable("gui.eam.crafting_table.knife"),
					Component.translatable("gui.eam.crafting_table.knife.desc").withStyle(ChatFormatting.GRAY),
					Component.translatable("gui.eam.crafting_table.knife.tiers").withStyle(ChatFormatting.GRAY)),
			List.of(Component.translatable("gui.eam.crafting_table.saw"),
					Component.translatable("gui.eam.crafting_table.saw.desc").withStyle(ChatFormatting.GRAY)),
			List.of(Component.translatable("gui.eam.crafting_table.hammer"),
					Component.translatable("gui.eam.crafting_table.hammer.desc").withStyle(ChatFormatting.GRAY)));

	public CraftingTableScreen(CraftingTableMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		for (int i = 0; i < CraftingTableMenu.TOOL_COUNT; i++) {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE,
					leftPos + CraftingTableMenu.TOOL_X + i * CraftingTableMenu.TOOL_SPACING - 1, topPos + CraftingTableMenu.TOOL_Y - 1, 18, 18);
		}
		CraftProgress.draw(graphics, menu, CraftProgress.TABLE, leftPos, topPos);
	}

	@Override
	protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractTooltip(graphics, mouseX, mouseY);
		if (hoveredSlot != null && CraftingTableMenu.isToolSlot(hoveredSlot.index) && !hoveredSlot.hasItem()
				&& menu.getCarried().isEmpty()) {
			graphics.setComponentTooltipForNextFrame(font, TOOL_HINTS.get(hoveredSlot.index - CraftingTableMenu.FIRST_TOOL_SLOT), mouseX, mouseY);
		}
	}
}
