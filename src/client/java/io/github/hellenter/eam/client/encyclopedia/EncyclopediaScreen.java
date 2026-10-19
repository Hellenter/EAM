package io.github.hellenter.eam.client.encyclopedia;

import io.github.hellenter.eam.Eam;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The encyclopedia: a book with every stage of progression and the bosses in the order they are meant to be fought.
 * The left page lists them; a click opens one on the right page with its tier, the items it is about and its text,
 * which the wheel scrolls. A stage or boss whose advancement the player has earned is ticked. Purely a guide: nothing
 * is locked by it.
 */
public class EncyclopediaScreen extends Screen {
	private static final Identifier WINDOW = Eam.id("textures/gui/encyclopedia/window.png");
	private static final int WIDTH = 384;
	private static final int HEIGHT = 210;
	private static final int SPLIT = 144;
	private static final int LIST_X = 11;
	private static final int LIST_WIDTH = SPLIT - 5 - LIST_X;
	private static final int LIST_TOP = 26;
	private static final int ROW = 12;
	private static final int HEADER_GAP = 16;
	private static final int PAGE_X = SPLIT + 8;
	private static final int PAGE_WIDTH = WIDTH - 12 - PAGE_X;
	private static final int ICONS_Y = 36;
	private static final int SLOT = 18;
	private static final int BODY_TOP = 60;
	private static final int BODY_BOTTOM = HEIGHT - 9;
	private static final int LINE = 10;
	private static final int PARAGRAPH_GAP = 4;
	private static final int INK = 0xFF3B2414;
	private static final int MUTED = 0xFF7A5E44;
	private static final int DONE = 0xFF2E7D32;
	private static final int SELECTED = 0x40603818;
	private static final int HOVER = 0x20603818;
	private static final int SLOT_SHADE = 0x24603818;
	private static final int SCROLL_BAR = 0x80603818;

	/** The page open when the book was last closed; -1 before the first opening. */
	private static int lastPage = -1;

	private final @Nullable Screen parent;
	private final List<List<ItemStack>> icons = new ArrayList<>();
	private int page;
	private int scroll;
	private List<Line> body = List.of();
	private int left;
	private int top;

	private record Line(FormattedCharSequence text, int y) {
	}

	public EncyclopediaScreen(@Nullable Screen parent) {
		super(Component.translatable("gui.eam.encyclopedia.title"));
		this.parent = parent;
		for (EncyclopediaPage entry : EncyclopediaPage.PAGES) {
			icons.add(entry.icons().stream().map(ItemStack::new).toList());
		}
	}

	@Override
	protected void init() {
		left = (width - WIDTH) / 2;
		top = (height - HEIGHT) / 2;
		if (lastPage < 0) lastPage = firstUnreachedStage();
		open(lastPage);
	}

	/** Opens on the first stage the player has not reached yet, so the book starts where he is. */
	private int firstUnreachedStage() {
		List<EncyclopediaPage> pages = EncyclopediaPage.PAGES;
		for (int i = 0; i < pages.size(); i++) {
			EncyclopediaPage entry = pages.get(i);
			if (entry.kind() == EncyclopediaPage.Kind.STAGE && entry.advancement() != null && !reached(entry)) return i;
		}
		return 0;
	}

	private void open(int index) {
		page = index;
		lastPage = index;
		scroll = 0;
		List<Line> lines = new ArrayList<>();
		int y = 0;
		String text = Component.translatable(EncyclopediaPage.PAGES.get(index).translationKey("text")).getString();
		for (String paragraph : text.split("\n")) {
			for (FormattedCharSequence line : font.split(Component.literal(paragraph), PAGE_WIDTH - 4)) {
				lines.add(new Line(line, y));
				y += LINE;
			}
			y += PARAGRAPH_GAP;
		}
		body = lines;
	}

	private int bodyHeight() {
		return body.isEmpty() ? 0 : body.getLast().y() + LINE;
	}

	private int maxScroll() {
		return Math.max(0, bodyHeight() - (BODY_BOTTOM - BODY_TOP));
	}

	private boolean reached(EncyclopediaPage entry) {
		ClientPacketListener connection = minecraft.getConnection();
		if (entry.advancement() == null || connection == null) return false;
		ClientAdvancements advancements = connection.getAdvancements();
		AdvancementHolder holder = advancements.get(entry.advancement());
		if (holder == null) return false;
		AdvancementProgress progress = advancements.progress().get(holder);
		return progress != null && progress.isDone();
	}

	/** The y of a row in the list, relative to the window: stages first, then the bosses under their own heading. */
	private static int rowY(int index) {
		int bossStart = firstBoss();
		return LIST_TOP + index * ROW + (index >= bossStart ? HEADER_GAP : 0);
	}

	private static int firstBoss() {
		List<EncyclopediaPage> pages = EncyclopediaPage.PAGES;
		for (int i = 0; i < pages.size(); i++) {
			if (pages.get(i).kind() == EncyclopediaPage.Kind.BOSS) return i;
		}
		return pages.size();
	}

	private int rowAt(double x, double y) {
		if (x < left + LIST_X - 2 || x >= left + LIST_X + LIST_WIDTH) return -1;
		for (int i = 0; i < EncyclopediaPage.PAGES.size(); i++) {
			int rowTop = top + rowY(i) - 2;
			if (y >= rowTop && y < rowTop + ROW) return i;
		}
		return -1;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		int index = rowAt(event.x(), event.y());
		if (event.button() == 0 && index >= 0) {
			if (index != page) open(index);
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		scroll = Mth.clamp(scroll - (int) Math.signum(scrollY) * LINE * 2, 0, maxScroll());
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (Encyclopedia.KEY.matches(event)) {
			onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void onClose() {
		minecraft.gui.setScreen(parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		graphics.blit(RenderPipelines.GUI_TEXTURED, WINDOW, left, top, 0.0F, 0.0F, WIDTH, HEIGHT, 512, 256);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);
		drawList(graphics, mouseX, mouseY);
		ItemStack hovered = drawPage(graphics, mouseX, mouseY);
		if (!hovered.isEmpty()) graphics.setTooltipForNextFrame(font, hovered, mouseX, mouseY);
	}

	private void drawList(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		graphics.text(font, title, left + LIST_X, top + 11, INK, false);
		List<EncyclopediaPage> pages = EncyclopediaPage.PAGES;
		int bossStart = firstBoss();
		if (bossStart < pages.size()) {
			graphics.text(font, Component.translatable("gui.eam.encyclopedia.bosses"), left + LIST_X,
					top + rowY(bossStart) - HEADER_GAP + 3, MUTED, false);
		}
		int hoveredRow = rowAt(mouseX, mouseY);
		for (int i = 0; i < pages.size(); i++) {
			EncyclopediaPage entry = pages.get(i);
			int y = top + rowY(i);
			if (i == page || i == hoveredRow) {
				graphics.fill(left + LIST_X - 2, y - 2, left + LIST_X + LIST_WIDTH, y + ROW - 2, i == page ? SELECTED : HOVER);
			}
			String prefix = entry.kind() == EncyclopediaPage.Kind.BOSS
					? Component.translatable(entry.translationKey("order")).getString() + " "
					: "";
			int markWidth = reached(entry) ? font.width("✔") + 2 : 0;
			String name = font.plainSubstrByWidth(prefix + Component.translatable(entry.translationKey("name")).getString(),
					LIST_WIDTH - 2 - markWidth);
			graphics.text(font, name, left + LIST_X, y, INK, false);
			if (markWidth > 0) graphics.text(font, "✔", left + LIST_X + LIST_WIDTH - markWidth, y, DONE, false);
		}
	}

	/** Draws the open page and returns the icon under the mouse, if any. */
	private ItemStack drawPage(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		EncyclopediaPage entry = EncyclopediaPage.PAGES.get(page);
		int x = left + PAGE_X;
		graphics.text(font, Component.translatable(entry.translationKey("name")), x, top + 11, INK, false);
		graphics.text(font, Component.translatable(entry.translationKey("subtitle")), x, top + 23, MUTED, false);
		if (reached(entry)) {
			Component done = Component.translatable("gui.eam.encyclopedia.reached");
			graphics.text(font, done, x + PAGE_WIDTH - font.width(done), top + 11, DONE, false);
		}

		ItemStack hovered = ItemStack.EMPTY;
		List<ItemStack> stacks = icons.get(page);
		for (int i = 0; i < stacks.size(); i++) {
			int slotX = x + i * SLOT;
			int slotY = top + ICONS_Y;
			graphics.fill(slotX, slotY, slotX + SLOT - 1, slotY + SLOT - 1, SLOT_SHADE);
			graphics.item(stacks.get(i), slotX + 1, slotY + 1);
			if (mouseX >= slotX && mouseX < slotX + SLOT - 1 && mouseY >= slotY && mouseY < slotY + SLOT - 1) {
				hovered = stacks.get(i);
			}
		}

		int bodyTop = top + BODY_TOP;
		int bodyBottom = top + BODY_BOTTOM;
		graphics.enableScissor(x, bodyTop, x + PAGE_WIDTH, bodyBottom);
		for (Line line : body) {
			int y = bodyTop + line.y() - scroll;
			if (y + LINE < bodyTop || y > bodyBottom) continue;
			graphics.text(font, line.text(), x, y, INK, false);
		}
		graphics.disableScissor();
		int max = maxScroll();
		if (max > 0) {
			int track = bodyBottom - bodyTop;
			int thumb = Math.max(12, track * track / bodyHeight());
			int thumbY = bodyTop + (track - thumb) * scroll / max;
			graphics.fill(x + PAGE_WIDTH + 1, thumbY, x + PAGE_WIDTH + 3, thumbY + thumb, SCROLL_BAR);
		}
		return hovered;
	}
}
