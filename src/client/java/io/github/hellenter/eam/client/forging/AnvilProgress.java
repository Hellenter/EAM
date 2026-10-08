package io.github.hellenter.eam.client.forging;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.client.mixin.survival.hunger.AbstractContainerScreenAccessor;
import io.github.hellenter.eam.world.forging.AnvilForge;
import io.github.hellenter.eam.world.forging.ForgingAnvilMenu;
import io.github.hellenter.eam.world.forging.ForgingModule;
import io.github.hellenter.eam.world.forging.HammerSlot;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Fills the anvil arrow while a plate is hammered, with the crafting table's progress arrow of the same size, and says
 * when the anvil is too weak for the item on it.
 */
public final class AnvilProgress {
	private static final Identifier SPRITE = Eam.id("container/crafting/progress");
	private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
	/** The anvil arrow, 22x15 at (102, 48) of the menu. */
	private static final int X = 102;
	private static final int Y = 48;
	private static final int WIDTH = 22;
	private static final int HEIGHT = 15;
	/** The cost line of the vanilla anvil: right-aligned to the screen's inner edge, under the slots. */
	private static final int TEXT_RIGHT = 168;
	private static final int TEXT_Y = 69;
	private static final int TOO_WEAK_COLOR = 0xFFFF6060;
	private static final int TEXT_BACKGROUND = 0x4F000000;

	private AnvilProgress() {
	}

	public static void register() {
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (screen instanceof AnvilScreen) {
				ScreenEvents.afterExtract(screen).register(AnvilProgress::draw);
			}
		});
	}

	/** Called from the anvil screen's background: the frame of the hammer slot, over the hammer drawn there. */
	public static void drawHammerSlot(AnvilScreen screen, GuiGraphicsExtractor graphics) {
		AbstractContainerScreenAccessor container = (AbstractContainerScreenAccessor) screen;
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, container.eam$getLeftPos() + HammerSlot.X - 1,
				container.eam$getTopPos() + HammerSlot.Y - 1, 18, 18);
	}

	private static void draw(Screen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		if (!(screen instanceof AnvilScreen anvil) || !(anvil.getMenu() instanceof ForgingAnvilMenu menu)) {
			return;
		}
		AnvilForge forge = menu.eam$forge();
		AbstractContainerScreenAccessor container = (AbstractContainerScreenAccessor) anvil;
		if (ForgingModule.tooWeak(menu)) {
			// Where vanilla says "Too Expensive!".
			Component text = Component.translatable("gui.eam.anvil.too_weak");
			int left = container.eam$getLeftPos();
			int top = container.eam$getTopPos();
			int x = left + TEXT_RIGHT - Minecraft.getInstance().font.width(text) - 2;
			graphics.fill(x - 2, top + TEXT_Y - 2, left + TEXT_RIGHT, top + TEXT_Y + 10, TEXT_BACKGROUND);
			graphics.text(Minecraft.getInstance().font, text, x, top + TEXT_Y, TOO_WEAK_COLOR);
		}
		int progress = forge.progress();
		int total = forge.total();
		if (!forge.active() || !forge.running() || total <= 0 || progress >= total) {
			return;
		}
		int width = Math.max(1, WIDTH * progress / total);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SPRITE, WIDTH, HEIGHT, 0, 0,
				container.eam$getLeftPos() + X, container.eam$getTopPos() + Y, width, HEIGHT);
	}
}
