package io.github.hellenter.eam.client.enchanting;

import io.github.hellenter.eam.world.enchanting.EnchantingOptions;
import io.github.hellenter.eam.world.enchanting.EnchantingSettings;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Holder;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * Left of the enchanting table: every enchantment the item can get, each with a + and a − button that pick it and set its
 * level. Clicking a name picks or unpicks it. Enchantments that clash with a picked one are greyed out.
 */
final class EnchantListPanel extends AbstractWidget {
	static final int HEIGHT = 166;
	private static final int PAD = 5;
	private static final int LIST_TOP = 17;
	private static final int ROW = 18;
	private static final int ROWS = 8;
	private static final int BUTTON = 13;
	private static final int SCROLLBAR = 3;
	private static final int ICON = 8;

	private static final int NAME = 0xFF8B8B8B;
	private static final int NAME_HOVER = 0xFF9E9E9E;
	private static final int NAME_PICKED = 0xFF6B4E9B;
	private static final int NAME_PICKED_HOVER = 0xFF7D5EB0;
	private static final int NAME_BLOCKED = 0xFF7A7A7A;

	private static final Component TITLE = Component.translatable("gui.eam.enchanting.title");
	private static final Component EMPTY = Component.translatable("gui.eam.enchanting.empty");
	private static final Component NONE = Component.translatable("gui.eam.enchanting.none");
	private static final Component PLUS = Component.literal("+");
	private static final Component MINUS = Component.literal("−");

	private final EnchantmentMenu menu;
	private final EnchantSelection selection;
	private int scroll;

	EnchantListPanel(int x, int y, int width, EnchantmentMenu menu, EnchantSelection selection) {
		super(x, y, width, HEIGHT, TITLE);
		this.menu = menu;
		this.selection = selection;
	}

	private int minusX() {
		return getX() + width - PAD - SCROLLBAR - 2 - BUTTON;
	}

	private int plusX() {
		return minusX() - BUTTON - 1;
	}

	private int nameLeft() {
		return getX() + PAD + ICON + 2;
	}

	private int nameRight() {
		return plusX() - 2;
	}

	private int rowTop(int row) {
		return getY() + LIST_TOP + row * ROW;
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		EnchantingSettings settings = EnchantingClient.settings();
		if (settings == null) {
			return;
		}
		Font font = Minecraft.getInstance().font;
		List<EnchantingOptions.Option> options = EnchantPanels.options(menu);
		selection.refresh(options);
		scroll = Math.clamp(scroll, 0, Math.max(0, options.size() - ROWS));

		EnchantPanels.frame(graphics, getX(), getY(), width, height);
		graphics.text(font, TITLE, getX() + (width - font.width(TITLE)) / 2, getY() + 6, EnchantPanels.TEXT, false);
		if (options.isEmpty()) {
			Component message = menu.getSlot(0).getItem().isEmpty() ? EMPTY : NONE;
			graphics.textWithWordWrap(font, message, getX() + PAD + 2, getY() + LIST_TOP + 4, width - 2 * PAD - 4, EnchantPanels.TEXT, false);
			return;
		}

		int shelves = EnchantPanels.shelves(menu);
		for (int row = 0; row < ROWS && scroll + row < options.size(); row++) {
			extractRow(graphics, font, settings, options, options.get(scroll + row), shelves, rowTop(row), mouseX, mouseY);
		}
		if (options.size() > ROWS) {
			int trackX = getX() + width - PAD - SCROLLBAR;
			int trackTop = rowTop(0);
			int trackHeight = ROWS * ROW;
			int thumb = Math.max(10, trackHeight * ROWS / options.size());
			int thumbTop = trackTop + (trackHeight - thumb) * scroll / (options.size() - ROWS);
			graphics.fill(trackX, trackTop, trackX + SCROLLBAR, trackTop + trackHeight, 0xFF8B8B8B);
			graphics.fill(trackX, thumbTop, trackX + SCROLLBAR, thumbTop + thumb, 0xFF373737);
		}
	}

	/** Bookshelves the picks need after a press of + on {@code option}. */
	private int shelvesToRaise(EnchantingSettings settings, List<EnchantingOptions.Option> options, EnchantingOptions.Option option) {
		return EnchantPanels.requiredShelves(settings, menu, options, selection.choicesRaised(option));
	}

	private void extractRow(GuiGraphicsExtractor graphics, Font font, EnchantingSettings settings, List<EnchantingOptions.Option> options,
			EnchantingOptions.Option option, int shelves, int top, int mouseX, int mouseY) {
		int picked = selection.level(option);
		Holder<Enchantment> blocker = selection.blockedBy(option);
		boolean canRaise = selection.canRaise(option);
		int shelvesNeeded = canRaise ? shelvesToRaise(settings, options, option) : 0;
		boolean tooFewShelves = shelvesNeeded > shelves;
		canRaise &= !tooFewShelves;
		// Greyed out when it cannot be picked at all: it clashes with a pick, or the table has too few shelves for it.
		boolean greyed = blocker != null || picked == 0 && tooFewShelves;
		int left = nameLeft();
		int right = nameRight();
		boolean nameHovered = EnchantPanels.inside(mouseX, mouseY, left, top, right - left, ROW);

		EnchantPanels.smallItem(graphics, new ItemStack(settings.resource(option.offensive())), getX() + PAD, top + 5);

		int body = greyed ? NAME_BLOCKED : picked > 0 ? nameHovered ? NAME_PICKED_HOVER : NAME_PICKED : nameHovered ? NAME_HOVER : NAME;
		EnchantPanels.inset(graphics, left, top + 1, right, top + ROW - 1, body);

		// The level the item gets (yellow) or already has (grey); none for one-level enchantments.
		int shown = picked > 0 ? picked : option.currentLevel();
		int numeralWidth = 0;
		if (shown > 0 && option.maxLevel() > 1) {
			Component numeral = Component.translatable("enchantment.level." + shown);
			numeralWidth = font.width(numeral) + 2;
			graphics.text(font, numeral, right - 2 - font.width(numeral), top + 5, picked > 0 ? 0xFFFFFF55 : 0xFFC0C0C0, true);
		}
		FormattedText name = EnchantPanels.fit(font, option.enchantment().value().description(), right - left - 5 - numeralWidth);
		graphics.text(font, Language.getInstance().getVisualOrder(name), left + 3, top + 5, greyed ? 0xFFB8B8B8 : 0xFFFFFFFF,
				!greyed);

		int buttonTop = top + (ROW - BUTTON) / 2;
		boolean canLower = picked > 0;
		EnchantPanels.button(graphics, font, PLUS, plusX(), buttonTop, BUTTON, BUTTON, canRaise,
				canRaise && EnchantPanels.inside(mouseX, mouseY, plusX(), buttonTop, BUTTON, BUTTON));
		EnchantPanels.button(graphics, font, MINUS, minusX(), buttonTop, BUTTON, BUTTON, canLower,
				canLower && EnchantPanels.inside(mouseX, mouseY, minusX(), buttonTop, BUTTON, BUTTON));

		if (nameHovered || EnchantPanels.inside(mouseX, mouseY, getX() + PAD, top, ICON + 2, ROW)) {
			graphics.setComponentTooltipForNextFrame(font, tooltip(settings, option, blocker, tooFewShelves ? shelvesNeeded : 0, shelves),
					mouseX, mouseY);
		}
	}

	/** @param shelvesNeeded shelves the next level would need when the table has too few, else 0 */
	private static List<Component> tooltip(EnchantingSettings settings, EnchantingOptions.Option option, Holder<Enchantment> blocker,
			int shelvesNeeded, int shelves) {
		List<Component> lines = new ArrayList<>();
		lines.add(Enchantment.getFullname(option.enchantment(), option.maxLevel()));
		lines.add(Component.translatable(option.offensive() ? "gui.eam.enchanting.offensive" : "gui.eam.enchanting.supportive")
				.withStyle(ChatFormatting.GRAY));
		StringJoiner costs = new StringJoiner(" / ");
		for (int level = option.currentLevel() + 1; level <= option.maxLevel(); level++) {
			costs.add(String.valueOf(option.resourceCost(settings, level)));
		}
		lines.add(Component.translatable("gui.eam.enchanting.costs", costs.toString()).withStyle(ChatFormatting.GRAY));
		StringJoiner shelvesPerLevel = new StringJoiner(" / ");
		for (int level = option.currentLevel() + 1; level <= option.maxLevel(); level++) {
			shelvesPerLevel.add(String.valueOf(option.shelves(settings, level)));
		}
		lines.add(Component.translatable("gui.eam.enchanting.shelves_per_level", shelvesPerLevel.toString()).withStyle(ChatFormatting.GRAY));
		if (option.currentLevel() > 0) {
			lines.add(Component.translatable("gui.eam.enchanting.has", Enchantment.getFullname(option.enchantment(), option.currentLevel()))
					.withStyle(ChatFormatting.GRAY));
		}
		if (blocker != null) {
			lines.add(Component.translatable("gui.eam.enchanting.incompatible", blocker.value().description()).withStyle(ChatFormatting.RED));
		} else if (shelvesNeeded > 0) {
			lines.add(Component.translatable("gui.eam.enchanting.too_few_shelves", shelvesNeeded, shelves).withStyle(ChatFormatting.RED));
		}
		return lines;
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		List<EnchantingOptions.Option> options = EnchantPanels.options(menu);
		selection.refresh(options);
		double mx = event.x();
		double my = event.y();
		if (my < rowTop(0)) {
			return;
		}
		int index = scroll + (int) ((my - rowTop(0)) / ROW);
		int row = index - scroll;
		if (row >= ROWS || index >= options.size()) {
			return;
		}
		EnchantingOptions.Option option = options.get(index);
		EnchantingSettings settings = EnchantingClient.settings();
		boolean shelvesAllow = settings != null && shelvesToRaise(settings, options, option) <= EnchantPanels.shelves(menu);
		int buttonTop = rowTop(row) + (ROW - BUTTON) / 2;
		if (EnchantPanels.inside(mx, my, plusX(), buttonTop, BUTTON, BUTTON)) {
			if (shelvesAllow) {
				selection.raise(option);
			}
		} else if (EnchantPanels.inside(mx, my, minusX(), buttonTop, BUTTON, BUTTON)) {
			selection.lower(option);
		} else if (mx >= nameLeft() && mx < nameRight() && (selection.level(option) > 0 || shelvesAllow)) {
			selection.toggle(option);
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
