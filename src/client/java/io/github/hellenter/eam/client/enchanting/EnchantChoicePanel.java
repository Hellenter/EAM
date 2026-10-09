package io.github.hellenter.eam.client.enchanting;

import io.github.hellenter.eam.world.enchanting.EnchantChoicePayload;
import io.github.hellenter.eam.world.enchanting.EnchantingLogic;
import io.github.hellenter.eam.world.enchanting.EnchantingOptions;
import io.github.hellenter.eam.world.enchanting.EnchantingSettings;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Panel beside the enchanting table: a list of the enchantments the item in the table can get, level buttons, the price
 * in diamonds or emeralds and levels, and an enchant button. The server checks and applies the choice.
 */
final class EnchantChoicePanel extends AbstractWidget {
	static final int WIDTH = 136;
	static final int HEIGHT = 166;
	private static final int PAD = 6;
	private static final int LIST_TOP = 18;
	private static final int ROW = 12;
	private static final int ROWS = 7;
	private static final int MARKER = 5;
	private static final int SCROLLBAR = 3;
	private static final int LEVELS_TOP = 106;
	private static final int LEVEL_WIDTH = 22;
	private static final int LEVEL_HEIGHT = 14;
	private static final int COST_TOP = 124;
	private static final int BUTTON_TOP = 146;
	private static final int BUTTON_HEIGHT = 16;

	private static final int BODY = 0xFFC6C6C6;
	private static final int TEXT = 0xFF404040;
	private static final int ROW_HOVER = 0xFFB0B0B0;
	private static final int ROW_SELECTED = 0xFF8B8B8B;
	private static final int OFFENSIVE = 0xFF4AEDD9;
	private static final int SUPPORTIVE = 0xFF17DD62;
	private static final int MISSING = 0xFFAA0000;
	private static final int LEVELS = 0xFF2F7F17;
	private static final WidgetSprites BUTTON = new WidgetSprites(Identifier.withDefaultNamespace("widget/button"),
			Identifier.withDefaultNamespace("widget/button_disabled"), Identifier.withDefaultNamespace("widget/button_highlighted"));

	private static final Component TITLE = Component.translatable("gui.eam.enchanting.title");
	private static final Component EMPTY = Component.translatable("gui.eam.enchanting.empty");
	private static final Component NONE = Component.translatable("gui.eam.enchanting.none");
	private static final Component ENCHANT = Component.translatable("gui.eam.enchanting.enchant");
	private static final Component PICK = Component.translatable("gui.eam.enchanting.pick");

	private final EnchantmentMenu menu;
	private @Nullable Holder<Enchantment> selected;
	private int level;
	private int scroll;

	EnchantChoicePanel(int x, int y, EnchantmentMenu menu) {
		super(x, y, WIDTH, HEIGHT, TITLE);
		this.menu = menu;
	}

	private static Minecraft minecraft() {
		return Minecraft.getInstance();
	}

	private List<EnchantingOptions.Option> options() {
		Minecraft minecraft = minecraft();
		if (minecraft.level == null) {
			return List.of();
		}
		return EnchantingOptions.forItem(menu.getSlot(0).getItem(), minecraft.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT));
	}

	/** Drops a selection the item no longer allows and keeps the level within the option's range. */
	private EnchantingOptions.@Nullable Option current(List<EnchantingOptions.Option> options) {
		EnchantingOptions.Option option = selected == null ? null : EnchantingOptions.find(options, selected);
		if (option == null) {
			selected = null;
			return null;
		}
		if (!option.allows(level)) {
			level = option.currentLevel() + 1;
		}
		return option;
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		EnchantingSettings settings = EnchantingClient.settings();
		if (settings == null) {
			return;
		}
		Font font = minecraft().font;
		List<EnchantingOptions.Option> options = options();
		EnchantingOptions.Option option = current(options);
		scroll = Math.clamp(scroll, 0, Math.max(0, options.size() - ROWS));

		int x0 = getX();
		int y0 = getY();
		int x1 = x0 + WIDTH;
		int y1 = y0 + HEIGHT;
		graphics.fill(x0 + 1, y0, x1 - 1, y1, 0xFF000000);
		graphics.fill(x0, y0 + 1, x1, y1 - 1, 0xFF000000);
		graphics.fill(x0 + 1, y0 + 1, x1 - 3, y1 - 3, 0xFFFFFFFF);
		graphics.fill(x0 + 3, y0 + 3, x1 - 1, y1 - 1, 0xFF555555);
		graphics.fill(x0 + 3, y0 + 3, x1 - 3, y1 - 3, BODY);
		graphics.text(font, TITLE, x0 + (WIDTH - font.width(TITLE)) / 2, y0 + 7, TEXT, false);

		if (options.isEmpty()) {
			Component message = menu.getSlot(0).getItem().isEmpty() ? EMPTY : NONE;
			graphics.textWithWordWrap(font, message, x0 + PAD, y0 + LIST_TOP + 4, WIDTH - 2 * PAD, TEXT, false);
			return;
		}

		extractList(graphics, font, options, mouseX, mouseY);
		if (option == null) {
			graphics.textWithWordWrap(font, PICK, x0 + PAD, y0 + LEVELS_TOP + 4, WIDTH - 2 * PAD, TEXT, false);
			return;
		}
		extractLevels(graphics, font, option, mouseX, mouseY);
		extractCost(graphics, font, settings, option, mouseX, mouseY);
	}

	private void extractList(GuiGraphicsExtractor graphics, Font font, List<EnchantingOptions.Option> options, int mouseX, int mouseY) {
		int left = getX() + PAD;
		int right = getX() + WIDTH - PAD - SCROLLBAR - 1;
		int nameWidth = right - left - MARKER - 4;
		for (int i = 0; i < ROWS && scroll + i < options.size(); i++) {
			EnchantingOptions.Option option = options.get(scroll + i);
			int top = getY() + LIST_TOP + i * ROW;
			boolean hovered = mouseX >= left && mouseX < right && mouseY >= top && mouseY < top + ROW;
			if (option.enchantment().equals(selected)) {
				graphics.fill(left, top, right, top + ROW, ROW_SELECTED);
			} else if (hovered) {
				graphics.fill(left, top, right, top + ROW, ROW_HOVER);
			}
			graphics.fill(left + 2, top + 3, left + 2 + MARKER, top + 3 + MARKER, 0xFF000000);
			graphics.fill(left + 3, top + 4, left + 1 + MARKER, top + 2 + MARKER, option.offensive() ? OFFENSIVE : SUPPORTIVE);
			Component name = option.enchantment().value().description();
			graphics.text(font, Language.getInstance().getVisualOrder(fit(font, name, nameWidth)), left + MARKER + 5, top + 2, TEXT, false);
			if (hovered) {
				graphics.setComponentTooltipForNextFrame(font, List.of(
						Enchantment.getFullname(option.enchantment(), option.maxLevel()),
						Component.translatable(option.offensive() ? "gui.eam.enchanting.offensive" : "gui.eam.enchanting.supportive")
				), mouseX, mouseY);
			}
		}
		if (options.size() > ROWS) {
			int trackTop = getY() + LIST_TOP;
			int trackHeight = ROWS * ROW;
			int thumb = Math.max(8, trackHeight * ROWS / options.size());
			int thumbTop = trackTop + (trackHeight - thumb) * scroll / (options.size() - ROWS);
			graphics.fill(right + 1, trackTop, right + 1 + SCROLLBAR, trackTop + trackHeight, 0xFF8B8B8B);
			graphics.fill(right + 1, thumbTop, right + 1 + SCROLLBAR, thumbTop + thumb, 0xFF373737);
		}
	}

	private static FormattedText fit(Font font, Component name, int width) {
		if (font.width(name) <= width) {
			return name;
		}
		return FormattedText.composite(font.substrByWidth(name, width - font.width("…")), FormattedText.of("…"));
	}

	private void extractLevels(GuiGraphicsExtractor graphics, Font font, EnchantingOptions.Option option, int mouseX, int mouseY) {
		for (int l = 1; l <= option.maxLevel(); l++) {
			int x = levelX(l);
			int y = getY() + LEVELS_TOP;
			boolean enabled = option.allows(l);
			boolean hovered = enabled && mouseX >= x && mouseX < x + LEVEL_WIDTH - 2 && mouseY >= y && mouseY < y + LEVEL_HEIGHT;
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BUTTON.get(enabled, hovered || l == level), x, y, LEVEL_WIDTH - 2, LEVEL_HEIGHT);
			Component label = Component.translatable("enchantment.level." + l);
			int color = !enabled ? 0xFFA0A0A0 : l == level ? 0xFFFFFF55 : 0xFFFFFFFF;
			graphics.text(font, label, x + (LEVEL_WIDTH - 2 - font.width(label)) / 2, y + 3, color, true);
		}
	}

	private int levelX(int level) {
		return getX() + PAD + (level - 1) * LEVEL_WIDTH;
	}

	private void extractCost(GuiGraphicsExtractor graphics, Font font, EnchantingSettings settings, EnchantingOptions.Option option,
			int mouseX, int mouseY) {
		LocalPlayer player = minecraft().player;
		Item resource = settings.resource(option.offensive());
		int resourceCost = option.resourceCost(settings, level);
		int levelCost = option.levelCost(settings, level);
		boolean infinite = player.hasInfiniteMaterials();
		boolean enoughResources = infinite || player.getInventory().countItem(resource) >= resourceCost;
		boolean enoughLevels = infinite || player.experienceLevel >= levelCost;

		int left = getX() + PAD;
		int top = getY() + COST_TOP;
		ItemStack icon = new ItemStack(resource);
		graphics.item(icon, left, top);
		Component count = Component.literal("× " + resourceCost);
		graphics.text(font, count, left + 18, top + 4, enoughResources ? TEXT : MISSING, false);
		if (mouseX >= left && mouseX < left + 16 && mouseY >= top && mouseY < top + 16) {
			graphics.setTooltipForNextFrame(font, icon, mouseX, mouseY);
		}
		Component levels = Component.translatable("gui.eam.enchanting.levels", levelCost);
		graphics.text(font, levels, getX() + WIDTH - PAD - font.width(levels), top + 4, enoughLevels ? LEVELS : MISSING, false);

		boolean active = EnchantingLogic.affordable(player.getInventory().countItem(resource), resourceCost, player.experienceLevel, levelCost, infinite);
		int bx = left;
		int by = getY() + BUTTON_TOP;
		int bw = WIDTH - 2 * PAD;
		boolean hovered = active && mouseX >= bx && mouseX < bx + bw && mouseY >= by && mouseY < by + BUTTON_HEIGHT;
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BUTTON.get(active, hovered), bx, by, bw, BUTTON_HEIGHT);
		Component label = Component.empty().append(ENCHANT).append(CommonComponents.SPACE)
				.append(Enchantment.getFullname(option.enchantment(), level).copy().withStyle(style -> style.withColor((Integer) null)));
		FormattedText fitted = fit(font, label, bw - 6);
		graphics.text(font, Language.getInstance().getVisualOrder(fitted), bx + (bw - font.width(fitted)) / 2, by + 4,
				active ? 0xFFFFFFFF : 0xFFA0A0A0, true);
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		List<EnchantingOptions.Option> options = options();
		EnchantingOptions.Option option = current(options);
		double mx = event.x();
		double my = event.y();
		int listTop = getY() + LIST_TOP;
		if (my >= listTop && my < listTop + ROWS * ROW) {
			int index = scroll + (int) ((my - listTop) / ROW);
			if (index < options.size()) {
				EnchantingOptions.Option clicked = options.get(index);
				selected = clicked.enchantment();
				level = clicked.currentLevel() + 1;
			}
			return;
		}
		if (option == null) {
			return;
		}
		int levelsTop = getY() + LEVELS_TOP;
		if (my >= levelsTop && my < levelsTop + LEVEL_HEIGHT) {
			int l = (int) ((mx - getX() - PAD) / LEVEL_WIDTH) + 1;
			if (option.allows(l)) {
				level = l;
			}
			return;
		}
		int buttonTop = getY() + BUTTON_TOP;
		if (my >= buttonTop && my < buttonTop + BUTTON_HEIGHT && ClientPlayNetworking.canSend(EnchantChoicePayload.TYPE)) {
			ClientPlayNetworking.send(new EnchantChoicePayload(option.enchantment(), level));
		}
	}

	/** Scrolls the list; the container screen does not pass scrolling to its widgets, so the client hooks it in. */
	void scroll(double amount) {
		scroll -= (int) Math.signum(amount);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		defaultButtonNarrationText(output);
	}
}
