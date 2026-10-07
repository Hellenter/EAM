package io.github.hellenter.eam.client.hud;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.survival.thirst.Thirst;
import io.github.hellenter.eam.survival.thirst.ThirstModule;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudStatusBarHeightRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import org.jspecify.annotations.Nullable;

/** Ten water droplets above the food bar, drawn right to left like food. */
public final class ThirstHud {
	private static final Identifier ID = Eam.id("thirst");
	private static final Identifier FULL = Eam.id("hud/thirst_full");
	private static final Identifier HALF = Eam.id("hud/thirst_half");
	private static final Identifier EMPTY = Eam.id("hud/thirst_empty");
	private static final int ROW_HEIGHT = 10;

	private ThirstHud() {
	}

	public static void register() {
		HudElementRegistry.attachElementAfter(VanillaHudElements.FOOD_BAR, ID, ThirstHud::render);
		HudStatusBarHeightRegistry.addRight(ID, player -> visibleThirst(player) != null ? ROW_HEIGHT : 0);
	}

	private static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		if (!(Minecraft.getInstance().getCameraEntity() instanceof Player player)) {
			return;
		}
		Thirst thirst = visibleThirst(player);
		if (thirst == null) {
			return;
		}

		int xRight = graphics.guiWidth() / 2 + 91;
		int y = graphics.guiHeight() - HudStatusBarHeightRegistry.getHeight(ID);
		for (int i = 0; i < Thirst.MAX / 2; i++) {
			int x = xRight - i * 8 - 9;
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, EMPTY, x, y, 9, 9);
			if (i * 2 + 1 < thirst.water()) {
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FULL, x, y, 9, 9);
			} else if (i * 2 + 1 == thirst.water()) {
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HALF, x, y, 9, 9);
			}
		}
	}

	/** The bar shows where vanilla shows food: survival or adventure, not riding a mount with its own hearts. */
	private static @Nullable Thirst visibleThirst(Player player) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.gameMode == null || !minecraft.gameMode.canHurtPlayer()) {
			return null;
		}
		if (player.getVehicle() instanceof LivingEntity vehicle && vehicle.showVehicleHealth()) {
			return null;
		}
		return player.getAttached(ThirstModule.THIRST);
	}
}
