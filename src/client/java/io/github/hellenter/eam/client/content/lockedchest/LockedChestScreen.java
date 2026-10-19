package io.github.hellenter.eam.client.content.lockedchest;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.hellenter.eam.content.lockedchest.LockedChestMenu;
import io.github.hellenter.eam.content.lockedchest.LockedChestOwnerNamePayload;
import io.github.hellenter.eam.content.lockedchest.LockedChestOwnerPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A locked chest on the vanilla chest background, with whose it is beside it and, for the player who placed it, the
 * list to give it to someone else: nobody, the village (the elder's store) and every grown villager of the village the
 * chest stands in, with a search box over them that matches a name or a profession. A click on a row gives the chest
 * to that one; the wheel scrolls the rows.
 */
public class LockedChestScreen extends ContainerScreen {
	private static final int OWNER_COLOR = 0xFFFFFFFF;
	private static final int ROW_TEXT = 0xFFE0E0E0;
	private static final int ROW_MUTED = 0xFFA0A0A0;
	private static final int LIST_BACKGROUND = 0xA0000000;
	private static final int ROW_HOVER = 0x40FFFFFF;
	private static final int ROW_CURRENT = 0x8040A040;
	private static final int PANEL_WIDTH = 160;
	private static final int SEARCH_Y = 18;
	private static final int LIST_Y = SEARCH_Y + 18;
	private static final int ROW = 12;
	private final LockedChestMenu chest;
	private @Nullable EditBox search;
	private List<Entry> shown = List.of();
	private int scroll;

	/** Takes a chest menu, as the vanilla chest screen does; the menu type it opens for is always a locked chest's. */
	public LockedChestScreen(ChestMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		this.chest = (LockedChestMenu) menu;
	}

	/** Shows the new owner when the server says a row of the list changed it. */
	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(LockedChestOwnerNamePayload.TYPE, (payload, context) -> {
			if (context.player().containerMenu instanceof LockedChestMenu menu && menu.containerId == payload.containerId()) {
				menu.setOwner(payload.owner(), payload.current());
			}
		});
	}

	/** One row: who it gives the chest to, as {@link LockedChestOwnerPayload} names him, and its text. */
	private record Entry(int owner, Component label, @Nullable Component detail) {
	}

	@Override
	protected void init() {
		super.init();
		if (!chest.data().canChange()) return;
		String query = search == null ? "" : search.getValue();
		search = new EditBox(font, panelX(), topPos + SEARCH_Y, PANEL_WIDTH, 14, search,
				Component.translatable("container.eam.locked_chest.search"));
		search.setHint(Component.translatable("container.eam.locked_chest.search"));
		search.setValue(query);
		search.setResponder(value -> filter());
		addRenderableWidget(search);
		filter();
	}

	private int panelX() {
		return leftPos + imageWidth + 4;
	}

	private int rows() {
		return Math.max(1, (imageHeight - LIST_Y - 4) / ROW);
	}

	/** The rows whose text holds what is typed in the search box. */
	private void filter() {
		String query = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
		List<Entry> entries = new ArrayList<>();
		entries.add(new Entry(LockedChestMenu.OpenData.NOBODY, Component.translatable("container.eam.locked_chest.nobody_row"), null));
		entries.add(new Entry(LockedChestMenu.OpenData.VILLAGE, Component.translatable("container.eam.locked_chest.village"), null));
		for (LockedChestMenu.Candidate candidate : chest.data().candidates()) {
			Component label = candidate.name()
					.map(name -> (Component) Component.translatable("gui.eam.village.named", name, candidate.profession()))
					.orElse(candidate.profession());
			entries.add(new Entry(candidate.id(), label, Component.translatable("container.eam.locked_chest.level", candidate.level(), candidate.distance())));
		}
		List<Entry> matching = new ArrayList<>();
		for (Entry entry : entries) {
			if (query.isEmpty() || entry.label().getString().toLowerCase(Locale.ROOT).contains(query)) matching.add(entry);
		}
		shown = matching;
		scroll = Mth.clamp(scroll, 0, Math.max(0, shown.size() - rows()));
	}

	/** The row under the mouse, or -1. */
	private int rowAt(double x, double y) {
		if (search == null || x < panelX() || x >= panelX() + PANEL_WIDTH || y < topPos + LIST_Y) return -1;
		int row = (int) (y - topPos - LIST_Y) / ROW;
		int index = scroll + row;
		return row < rows() && index < shown.size() ? index : -1;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		int index = rowAt(event.x(), event.y());
		if (index >= 0 && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
			ClientPlayNetworking.send(new LockedChestOwnerPayload(shown.get(index).owner()));
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		if (search != null && x >= panelX() && x < panelX() + PANEL_WIDTH && y >= topPos + LIST_Y) {
			scroll = Mth.clamp(scroll - (int) Math.signum(scrollY), 0, Math.max(0, shown.size() - rows()));
			return true;
		}
		return super.mouseScrolled(x, y, scrollX, scrollY);
	}

	/** While the search box takes typing, keys go to it, so the inventory key does not close the chest. */
	@Override
	public boolean keyPressed(KeyEvent event) {
		if (search != null && search.isFocused() && !event.isEscape()) {
			if (search.keyPressed(event)) return true;
			if (search.canConsumeInput()) return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);
		Component owner = Component.translatable("container.eam.locked_chest.owner", chest.data().owner());
		graphics.text(font, owner, panelX(), topPos + 6, OWNER_COLOR, true);
		if (search == null) return;
		int x0 = panelX();
		int y0 = topPos + LIST_Y;
		graphics.fill(x0, y0, x0 + PANEL_WIDTH, y0 + rows() * ROW, LIST_BACKGROUND);
		int hovered = rowAt(mouseX, mouseY);
		for (int row = 0; row < rows() && scroll + row < shown.size(); row++) {
			Entry entry = shown.get(scroll + row);
			int y = y0 + row * ROW;
			if (entry.owner() == chest.data().current()) graphics.fill(x0, y, x0 + PANEL_WIDTH, y + ROW, ROW_CURRENT);
			if (scroll + row == hovered) graphics.fill(x0, y, x0 + PANEL_WIDTH, y + ROW, ROW_HOVER);
			int detailWidth = entry.detail() == null ? 0 : font.width(entry.detail()) + 4;
			String label = font.plainSubstrByWidth(entry.label().getString(), PANEL_WIDTH - 6 - detailWidth);
			graphics.text(font, label, x0 + 3, y + 2, ROW_TEXT, false);
			if (entry.detail() != null) {
				graphics.text(font, entry.detail(), x0 + PANEL_WIDTH - 3 - font.width(entry.detail()), y + 2, ROW_MUTED, false);
			}
		}
		if (shown.isEmpty()) {
			graphics.text(font, Component.translatable("container.eam.locked_chest.no_match"), x0 + 3, y0 + 2, ROW_MUTED, false);
		}
	}
}
