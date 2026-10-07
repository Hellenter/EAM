package io.github.hellenter.eam.client.hud;

import io.github.hellenter.eam.client.mixin.survival.hunger.AbstractContainerScreenAccessor;
import io.github.hellenter.eam.survival.hunger.HungerLogic;
import io.github.hellenter.eam.survival.hunger.HungerModule;
import io.github.hellenter.eam.survival.hunger.HungerStats;
import io.github.hellenter.eam.survival.hunger.Nutrition;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/** Protein, fat and carbs bars drawn just above the survival inventory. Hover a bar for details. */
public final class NutritionPanel {
	private static final int CELL_WIDTH = 59;
	private static final int LABEL_WIDTH = 9;
	private static final int BAR_WIDTH = 46;
	private static final int HEIGHT = 9;
	private static final int BORDER = 0xFF000000;
	private static final int EMPTY = 0xFF3A3A3A;
	private static final int MARKER = 0xFFFFFFFF;
	private static final int LABEL = 0xFFFFFFFF;
	private static final int LABEL_BALANCED = 0xFF55FF55;

	private record Bar(String key, int color) {
	}

	private static final List<Bar> BARS = List.of(
			new Bar("protein", 0xFFD9534F),
			new Bar("fat", 0xFFE8C547),
			new Bar("carbs", 0xFF6FBF4A)
	);

	private NutritionPanel() {
	}

	public static void register() {
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (screen instanceof InventoryScreen) {
				ScreenEvents.afterExtract(screen).register(NutritionPanel::draw);
			}
		});
	}

	private static void draw(Screen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		Player player = Screens.getMinecraft(screen).player;
		if (player == null) {
			return;
		}
		Nutrition nutrition = player.getAttached(HungerModule.NUTRITION);
		HungerStats stats = player.getAttached(HungerModule.STATS);
		if (nutrition == null || stats == null) {
			return;
		}

		AbstractContainerScreenAccessor container = (AbstractContainerScreenAccessor) screen;
		Font font = Screens.getFont(screen);
		int top = container.eam$getTopPos() - HEIGHT - 2;
		int threshold = stats.balanceThreshold();
		boolean balanced = HungerLogic.balanced(nutrition, threshold);
		float[] values = {nutrition.protein(), nutrition.fat(), nutrition.carbs()};

		for (int i = 0; i < BARS.size(); i++) {
			Bar bar = BARS.get(i);
			int left = container.eam$getLeftPos() + 2 + i * CELL_WIDTH;
			int barLeft = left + LABEL_WIDTH;
			int value = Math.round(values[i]);

			graphics.text(font, Component.translatable("gui.eam.nutrient." + bar.key() + ".short"), left, top + 1,
					balanced ? LABEL_BALANCED : LABEL, true);
			graphics.fill(barLeft - 1, top + 1, barLeft + BAR_WIDTH + 1, top + 8, BORDER);
			graphics.fill(barLeft, top + 2, barLeft + BAR_WIDTH, top + 7, EMPTY);
			graphics.fill(barLeft, top + 2, barLeft + BAR_WIDTH * value / Nutrition.MAX, top + 7, bar.color());
			int marker = barLeft + BAR_WIDTH * threshold / Nutrition.MAX;
			graphics.fill(marker, top + 1, marker + 1, top + 8, MARKER);

			if (mouseX >= left && mouseX < barLeft + BAR_WIDTH + 1 && mouseY >= top && mouseY < top + HEIGHT) {
				graphics.setComponentTooltipForNextFrame(font, List.of(
						Component.translatable("gui.eam.nutrient.value",
								Component.translatable("gui.eam.nutrient." + bar.key()), value, Nutrition.MAX),
						balanced
								? Component.translatable("gui.eam.nutrition.balanced").withStyle(ChatFormatting.GREEN)
								: Component.translatable("gui.eam.nutrition.unbalanced", threshold).withStyle(ChatFormatting.GRAY)
				), mouseX, mouseY);
			}
		}
	}
}
