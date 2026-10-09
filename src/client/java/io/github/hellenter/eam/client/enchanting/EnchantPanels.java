package io.github.hellenter.eam.client.enchanting;

import io.github.hellenter.eam.world.enchanting.EnchantingOptions;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Drawing helpers shared by the enchantment list and the summary beside the enchanting table. */
final class EnchantPanels {
	static final int TEXT = 0xFF404040;
	static final int MISSING = 0xFFAA0000;
	static final int ENOUGH = 0xFF2F7F17;
	private static final WidgetSprites BUTTON = new WidgetSprites(Identifier.withDefaultNamespace("widget/button"),
			Identifier.withDefaultNamespace("widget/button_disabled"), Identifier.withDefaultNamespace("widget/button_highlighted"));

	private EnchantPanels() {
	}

	static List<EnchantingOptions.Option> options(EnchantmentMenu menu) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null) {
			return List.of();
		}
		return EnchantingOptions.forItem(menu.getSlot(0).getItem(), minecraft.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT));
	}

	/** The light grey container background with the vanilla bevelled border. */
	static void frame(GuiGraphicsExtractor graphics, int x0, int y0, int width, int height) {
		int x1 = x0 + width;
		int y1 = y0 + height;
		graphics.fill(x0 + 1, y0, x1 - 1, y1, 0xFF000000);
		graphics.fill(x0, y0 + 1, x1, y1 - 1, 0xFF000000);
		graphics.fill(x0 + 1, y0 + 1, x1 - 3, y1 - 3, 0xFFFFFFFF);
		graphics.fill(x0 + 3, y0 + 3, x1 - 1, y1 - 1, 0xFF555555);
		graphics.fill(x0 + 3, y0 + 3, x1 - 3, y1 - 3, 0xFFC6C6C6);
	}

	/** A sunken box like an inventory slot. */
	static void inset(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int body) {
		graphics.fill(x0, y0, x1, y1, 0xFF373737);
		graphics.fill(x0 + 1, y0 + 1, x1, y1, 0xFFFFFFFF);
		graphics.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, body);
	}

	static void button(GuiGraphicsExtractor graphics, Font font, Component label, int x, int y, int width, int height, boolean active,
			boolean hovered) {
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BUTTON.get(active, hovered), x, y, width, height);
		FormattedText fitted = fit(font, label, width - 4);
		graphics.text(font, Language.getInstance().getVisualOrder(fitted), x + (width - font.width(fitted) + 1) / 2,
				y + (height - 8) / 2, active ? 0xFFFFFFFF : 0xFFA0A0A0, true);
	}

	/** An item drawn at half size, 8 by 8. */
	static void smallItem(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y) {
		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y);
		graphics.pose().scale(0.5F);
		graphics.item(stack, 0, 0);
		graphics.pose().popMatrix();
	}

	static FormattedText fit(Font font, Component text, int width) {
		if (font.width(text) <= width) {
			return text;
		}
		return FormattedText.composite(font.substrByWidth(text, width - font.width("…")), FormattedText.of("…"));
	}

	static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}
}
