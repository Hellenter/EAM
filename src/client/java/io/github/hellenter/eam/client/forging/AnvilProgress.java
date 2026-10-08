package io.github.hellenter.eam.client.forging;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.client.mixin.survival.hunger.AbstractContainerScreenAccessor;
import io.github.hellenter.eam.world.forging.AnvilForge;
import io.github.hellenter.eam.world.forging.ForgingAnvilMenu;
import io.github.hellenter.eam.world.forging.ForgingLogic;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Fills the anvil arrow while a plate is hammered, with the crafting table's progress arrow of the same size. */
public final class AnvilProgress {
	private static final Identifier SPRITE = Eam.id("container/crafting/progress");
	/** The anvil arrow, 22x15 at (102, 48) of the menu. */
	private static final int X = 102;
	private static final int Y = 48;
	private static final int WIDTH = 22;
	private static final int HEIGHT = 15;

	private AnvilProgress() {
	}

	public static void register() {
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (screen instanceof AnvilScreen) {
				ScreenEvents.afterExtract(screen).register(AnvilProgress::draw);
			}
		});
	}

	private static void draw(Screen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		if (!(screen instanceof AnvilScreen anvil) || !(anvil.getMenu() instanceof ForgingAnvilMenu menu)) {
			return;
		}
		AnvilForge forge = menu.eam$forge();
		int progress = forge.progress();
		if (!forge.active() || !forge.running() || progress >= ForgingLogic.FORGE_TICKS) {
			return;
		}
		AbstractContainerScreenAccessor container = (AbstractContainerScreenAccessor) anvil;
		int width = Math.max(1, WIDTH * progress / ForgingLogic.FORGE_TICKS);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SPRITE, WIDTH, HEIGHT, 0, 0,
				container.eam$getLeftPos() + X, container.eam$getTopPos() + Y, width, HEIGHT);
	}
}
