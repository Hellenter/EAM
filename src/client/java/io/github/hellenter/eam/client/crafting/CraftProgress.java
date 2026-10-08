package io.github.hellenter.eam.client.crafting;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.client.mixin.survival.hunger.AbstractContainerScreenAccessor;
import io.github.hellenter.eam.world.crafting.CraftTimer;
import io.github.hellenter.eam.world.crafting.TimedCraftingMenu;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Fills the arrow between the crafting grid and the result while a craft runs. */
public final class CraftProgress {
	/** The crafting table arrow, 22x15 at (90, 35) of the menu. */
	static final Arrow TABLE = new Arrow(Eam.id("container/crafting/progress"), 90, 35, 22, 15);
	/** The inventory grid arrow, 16x13 at (135, 29) of the menu. */
	private static final Arrow INVENTORY = new Arrow(Eam.id("container/crafting/progress_small"), 135, 29, 16, 13);

	record Arrow(Identifier sprite, int x, int y, int width, int height) {
	}

	private CraftProgress() {
	}

	public static void register() {
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (screen instanceof InventoryScreen) {
				ScreenEvents.afterExtract(screen).register(CraftProgress::drawInventory);
			}
		});
	}

	private static void drawInventory(Screen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		if (screen instanceof InventoryScreen inventory) {
			AbstractContainerScreenAccessor container = (AbstractContainerScreenAccessor) inventory;
			draw(graphics, inventory.getMenu(), INVENTORY, container.eam$getLeftPos(), container.eam$getTopPos());
		}
	}

	static void draw(GuiGraphicsExtractor graphics, Object menu, Arrow arrow, int left, int top) {
		if (!(menu instanceof TimedCraftingMenu timed)) {
			return;
		}
		CraftTimer timer = timed.eam$craftTimer();
		int total = timer.total();
		int progress = timer.progress();
		if (total <= 0 || progress >= total) {
			return;
		}
		int width = Math.max(1, arrow.width() * progress / total);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, arrow.sprite(), arrow.width(), arrow.height(), 0, 0,
				left + arrow.x(), top + arrow.y(), width, arrow.height());
	}
}
