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
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Right of the enchanting table: the picked enchantments in order, their total price in diamonds, emeralds and levels, and
 * the button that enchants the item with all of them. The server checks and applies the choice.
 */
final class EnchantSummaryPanel extends AbstractWidget {
	static final int HEIGHT = 166;
	private static final int PAD = 7;
	private static final int LIST_TOP = 18;
	private static final int LINE = 10;
	private static final int LINES = 7;
	private static final int PRICE_TOP = 94;
	private static final int SHELVES_TOP = 134;
	private static final int BUTTON_TOP = 143;
	private static final int BUTTON_HEIGHT = 18;

	private static final Component TITLE = Component.translatable("gui.eam.enchanting.chosen");
	private static final Component HINT = Component.translatable("gui.eam.enchanting.hint");
	private static final Component PRICE = Component.translatable("gui.eam.enchanting.price");
	private static final Component ENCHANT = Component.translatable("gui.eam.enchanting.enchant");

	private final EnchantmentMenu menu;
	private final EnchantSelection selection;

	EnchantSummaryPanel(int x, int y, int width, EnchantmentMenu menu, EnchantSelection selection) {
		super(x, y, width, HEIGHT, TITLE);
		this.menu = menu;
		this.selection = selection;
	}

	/** @param affordable whether the player can pay and the table has enough shelves */
	private record State(List<EnchantingOptions.Pick> picks, EnchantingLogic.Price price, boolean affordable) {
	}

	private @Nullable State state(EnchantingSettings settings) {
		List<EnchantingOptions.Option> options = EnchantPanels.options(menu);
		selection.refresh(options);
		List<EnchantingOptions.Pick> picks = EnchantingOptions.check(options, selection.choices());
		LocalPlayer player = Minecraft.getInstance().player;
		if (picks == null || player == null) {
			return null;
		}
		EnchantingLogic.Price price = EnchantingOptions.price(settings, picks);
		Item offensive = settings.resource(true);
		Item supportive = settings.resource(false);
		boolean affordable = EnchantingLogic.affordable(price, player.getInventory().countItem(offensive),
				player.getInventory().countItem(supportive), offensive == supportive, player.experienceLevel, player.hasInfiniteMaterials())
				&& EnchantingOptions.requiredShelves(settings, menu.getSlot(0).getItem(), picks) <= EnchantPanels.shelves(menu);
		return new State(picks, price, affordable);
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		EnchantingSettings settings = EnchantingClient.settings();
		if (settings == null) {
			return;
		}
		Font font = Minecraft.getInstance().font;
		State state = state(settings);
		int left = getX() + PAD;
		int textWidth = width - 2 * PAD;

		EnchantPanels.frame(graphics, getX(), getY(), width, height);
		graphics.text(font, TITLE, getX() + (width - font.width(TITLE)) / 2, getY() + 6, EnchantPanels.TEXT, false);
		int shelves = EnchantPanels.shelves(menu);
		if (state == null && shelves < settings.minShelves()) {
			graphics.textWithWordWrap(font, Component.translatable("gui.eam.enchanting.shelves_needed", settings.minShelves(), shelves), left,
					getY() + LIST_TOP + 2, textWidth, EnchantPanels.MISSING, false);
		} else if (state == null) {
			graphics.textWithWordWrap(font, HINT, left, getY() + LIST_TOP + 2, textWidth, 0xFF707070, false);
		} else {
			extractPicks(graphics, font, state.picks(), left, textWidth);
		}

		graphics.fill(left, getY() + PRICE_TOP - 4, left + textWidth, getY() + PRICE_TOP - 3, 0xFF8B8B8B);
		graphics.text(font, PRICE, left, getY() + PRICE_TOP, EnchantPanels.TEXT, false);
		if (state != null) {
			extractPrice(graphics, font, settings, state.price(), left, mouseX, mouseY);
		} else {
			graphics.text(font, "—", left + font.width(PRICE) + 4, getY() + PRICE_TOP, EnchantPanels.TEXT, false);
		}

		int needed = EnchantPanels.requiredShelves(settings, menu, EnchantPanels.options(menu), selection.choices());
		graphics.text(font, Component.translatable("gui.eam.enchanting.shelves", shelves, needed), left, getY() + SHELVES_TOP,
				shelves >= needed ? EnchantPanels.ENOUGH : EnchantPanels.MISSING, false);

		boolean active = state != null && state.affordable();
		int buttonTop = getY() + BUTTON_TOP;
		EnchantPanels.button(graphics, font, ENCHANT, left - 2, buttonTop, textWidth + 4, BUTTON_HEIGHT, active,
				active && EnchantPanels.inside(mouseX, mouseY, left - 2, buttonTop, textWidth + 4, BUTTON_HEIGHT));
	}

	private void extractPicks(GuiGraphicsExtractor graphics, Font font, List<EnchantingOptions.Pick> picks, int left, int textWidth) {
		boolean overflow = picks.size() > LINES;
		int shown = overflow ? LINES - 1 : picks.size();
		for (int i = 0; i < shown; i++) {
			EnchantingOptions.Pick pick = picks.get(i);
			Component name = Enchantment.getFullname(pick.option().enchantment(), pick.level()).copy()
					.withStyle(style -> style.withColor(pick.option().enchantment().is(EnchantmentTags.CURSE) ? 0xAA0000 : 0x5B3A8F));
			Component line = Component.literal((i + 1) + ". ").withColor(EnchantPanels.TEXT).append(name);
			FormattedText fitted = EnchantPanels.fit(font, line, textWidth);
			graphics.text(font, Language.getInstance().getVisualOrder(fitted), left, getY() + LIST_TOP + i * LINE, EnchantPanels.TEXT, false);
		}
		if (overflow) {
			graphics.text(font, Component.translatable("gui.eam.enchanting.more", picks.size() - shown), left,
					getY() + LIST_TOP + shown * LINE, 0xFF707070, false);
		}
	}

	private void extractPrice(GuiGraphicsExtractor graphics, Font font, EnchantingSettings settings, EnchantingLogic.Price price, int left,
			int mouseX, int mouseY) {
		LocalPlayer player = Minecraft.getInstance().player;
		boolean infinite = player.hasInfiniteMaterials();
		Item offensive = settings.resource(true);
		Item supportive = settings.resource(false);
		int haveOffensive = player.getInventory().countItem(offensive);
		int haveSupportive = player.getInventory().countItem(supportive);
		boolean same = offensive == supportive;
		int top = getY() + PRICE_TOP + 11;
		int x = left;
		if (price.offensive() > 0) {
			boolean enough = infinite || haveOffensive >= price.offensive() + (same ? price.supportive() : 0);
			x = extractResource(graphics, font, new ItemStack(offensive), price.offensive(), enough, x, top, mouseX, mouseY);
		}
		if (price.supportive() > 0) {
			boolean enough = infinite || haveSupportive >= price.supportive() + (same ? price.offensive() : 0);
			extractResource(graphics, font, new ItemStack(supportive), price.supportive(), enough, x, top, mouseX, mouseY);
		}
		boolean enoughLevels = infinite || player.experienceLevel >= price.levels();
		graphics.text(font, Component.translatable("gui.eam.enchanting.levels", price.levels()), left, top + 20,
				enoughLevels ? EnchantPanels.ENOUGH : EnchantPanels.MISSING, false);
	}

	/** An item icon with "× count", red when the player has too few; returns where the next one goes. */
	private static int extractResource(GuiGraphicsExtractor graphics, Font font, ItemStack icon, int count, boolean enough, int x, int top,
			int mouseX, int mouseY) {
		graphics.item(icon, x, top);
		String text = "× " + count;
		graphics.text(font, text, x + 18, top + 4, enough ? EnchantPanels.TEXT : EnchantPanels.MISSING, false);
		if (EnchantPanels.inside(mouseX, mouseY, x, top, 16, 16)) {
			graphics.setTooltipForNextFrame(font, icon, mouseX, mouseY);
		}
		return x + 18 + font.width(text) + 8;
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		EnchantingSettings settings = EnchantingClient.settings();
		if (settings == null || !EnchantPanels.inside(event.x(), event.y(), getX() + PAD - 2, getY() + BUTTON_TOP, width - 2 * PAD + 4, BUTTON_HEIGHT)) {
			return;
		}
		State state = state(settings);
		if (state != null && state.affordable() && ClientPlayNetworking.canSend(EnchantChoicePayload.TYPE)) {
			ClientPlayNetworking.send(new EnchantChoicePayload(selection.choices()));
		}
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		defaultButtonNarrationText(output);
	}
}
