package io.github.hellenter.eam.client.encyclopedia;

import io.github.hellenter.eam.Eam;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The encyclopedia, an open book like the vanilla written book: the left page is the contents, every stage of
 * progression and then the bosses in the order they are meant to be fought; the right page shows the chosen entry with
 * its tier, the items it is about and its text. Text that does not fit goes on to more pages, turned with the vanilla page
 * arrows or Page Up / Page Down, which also run on into the next entry. An entry whose advancement the player has earned
 * is ticked. Purely a guide: nothing is locked by it.
 */
public class EncyclopediaScreen extends Screen {
	private static final Identifier WINDOW = Eam.id("textures/gui/encyclopedia/window.png");
	private static final int WIDTH = 340;
	private static final int HEIGHT = 200;
	private static final int LEFT_PAGE_X = 18;
	private static final int RIGHT_PAGE_X = 182;
	private static final int PAGE_WIDTH = 142;
	private static final int CONTENTS_TOP = 32;
	private static final int ROW = 11;
	private static final int HEADER_GAP = 13;
	private static final int ICON_Y = 46;
	private static final int SLOT = 17;
	private static final int FIRST_TEXT_TOP = 68;
	private static final int TEXT_TOP = 26;
	private static final int TEXT_BOTTOM = 172;
	private static final int LINE = 9;
	private static final int PARAGRAPH_GAP = 4;
	private static final int ARROW_Y = 177;
	private static final int INK = 0xFF000000;
	private static final int FADED = 0xFF6B5B45;
	private static final int CHOSEN = 0xFF8B1A10;
	private static final int DONE = 0xFF2E7D32;
	private static final int CHOSEN_SHADE = 0x1A8B1A10;
	private static final int SLOT_SHADE = 0x18603818;

	/** The entry and sheet open when the book was last closed; -1 before the first opening. */
	private static int lastEntry = -1;
	private static int lastSheet;

	private final @Nullable Screen parent;
	private final List<List<ItemStack>> icons = new ArrayList<>();
	/** Each entry's text cut into sheets, a sheet being the lines on one right page with their y. */
	private final List<List<List<Line>>> sheets = new ArrayList<>();
	private int entry;
	private int sheet;
	private int left;
	private int top;
	private PageButton forward;
	private PageButton back;

	private record Line(FormattedCharSequence text, int y) {
	}

	public EncyclopediaScreen(@Nullable Screen parent) {
		super(Component.translatable("gui.eam.encyclopedia.title"));
		this.parent = parent;
		for (EncyclopediaPage page : EncyclopediaPage.PAGES) {
			icons.add(page.icons().stream().map(ItemStack::new).toList());
		}
	}

	@Override
	protected void init() {
		left = (width - WIDTH) / 2;
		top = (height - HEIGHT) / 2;
		sheets.clear();
		for (EncyclopediaPage page : EncyclopediaPage.PAGES) {
			sheets.add(paginate(Component.translatable(page.translationKey("text")).getString()));
		}
		if (lastEntry < 0) {
			lastEntry = firstUnreachedStage();
			lastSheet = 0;
		}
		entry = lastEntry;
		sheet = Math.min(lastSheet, sheets.get(entry).size() - 1);
		forward = addRenderableWidget(new PageButton(left + RIGHT_PAGE_X + PAGE_WIDTH - 23, top + ARROW_Y, true,
				pressed -> turn(1), true));
		back = addRenderableWidget(new PageButton(left + RIGHT_PAGE_X, top + ARROW_Y, false, pressed -> turn(-1), true));
		updateArrows();
	}

	/** Lays the paragraphs out line by line; the first sheet starts lower, under the title and the icons. */
	private List<List<Line>> paginate(String text) {
		List<List<Line>> result = new ArrayList<>();
		List<Line> current = new ArrayList<>();
		int y = FIRST_TEXT_TOP;
		for (String paragraph : text.split("\n")) {
			if (!current.isEmpty()) y += PARAGRAPH_GAP;
			for (FormattedCharSequence line : font.split(Component.literal(paragraph), PAGE_WIDTH)) {
				if (y + LINE > TEXT_BOTTOM) {
					result.add(current);
					current = new ArrayList<>();
					y = TEXT_TOP;
				}
				current.add(new Line(line, y));
				y += LINE;
			}
		}
		result.add(current);
		return result;
	}

	/** Opens on the first stage the player has not reached yet, so the book starts where he is. */
	private int firstUnreachedStage() {
		List<EncyclopediaPage> pages = EncyclopediaPage.PAGES;
		for (int i = 0; i < pages.size(); i++) {
			EncyclopediaPage page = pages.get(i);
			if (page.kind() == EncyclopediaPage.Kind.STAGE && page.advancement() != null && !reached(page)) return i;
		}
		return 0;
	}

	private void open(int index, int sheetIndex) {
		entry = index;
		sheet = sheetIndex;
		lastEntry = entry;
		lastSheet = sheet;
		updateArrows();
	}

	/** One sheet on or back, running on into the next or previous entry. */
	private void turn(int step) {
		int next = sheet + step;
		if (next >= 0 && next < sheets.get(entry).size()) {
			open(entry, next);
		} else if (step > 0 && entry + 1 < sheets.size()) {
			open(entry + 1, 0);
		} else if (step < 0 && entry > 0) {
			open(entry - 1, sheets.get(entry - 1).size() - 1);
		}
	}

	private void updateArrows() {
		forward.visible = entry + 1 < sheets.size() || sheet + 1 < sheets.get(entry).size();
		back.visible = entry > 0 || sheet > 0;
	}

	private boolean reached(EncyclopediaPage page) {
		ClientPacketListener connection = minecraft.getConnection();
		if (page.advancement() == null || connection == null) return false;
		ClientAdvancements advancements = connection.getAdvancements();
		AdvancementHolder holder = advancements.get(page.advancement());
		if (holder == null) return false;
		AdvancementProgress progress = advancements.progress().get(holder);
		return progress != null && progress.isDone();
	}

	/** The y of a contents row, relative to the book: stages first, then the bosses under their own heading. */
	private static int rowY(int index) {
		return CONTENTS_TOP + index * ROW + (index >= firstBoss() ? HEADER_GAP : 0);
	}

	private static int firstBoss() {
		List<EncyclopediaPage> pages = EncyclopediaPage.PAGES;
		for (int i = 0; i < pages.size(); i++) {
			if (pages.get(i).kind() == EncyclopediaPage.Kind.BOSS) return i;
		}
		return pages.size();
	}

	private int rowAt(double x, double y) {
		if (x < left + LEFT_PAGE_X - 2 || x >= left + LEFT_PAGE_X + PAGE_WIDTH + 2) return -1;
		for (int i = 0; i < EncyclopediaPage.PAGES.size(); i++) {
			int rowTop = top + rowY(i) - 2;
			if (y >= rowTop && y < rowTop + ROW) return i;
		}
		return -1;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) return true;
		int index = rowAt(event.x(), event.y());
		if (event.button() != InputConstants.MOUSE_BUTTON_LEFT || index < 0) return false;
		if (index != entry || sheet != 0) {
			open(index, 0);
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
		}
		return true;
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		if (scrollY != 0) turn(scrollY < 0 ? 1 : -1);
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (Encyclopedia.KEY.matches(event)) {
			onClose();
			return true;
		}
		if (super.keyPressed(event)) return true;
		return switch (event.key()) {
			case InputConstants.KEY_PAGEUP -> {
				if (back.visible) back.onPress(event);
				yield true;
			}
			case InputConstants.KEY_PAGEDOWN -> {
				if (forward.visible) forward.onPress(event);
				yield true;
			}
			default -> false;
		};
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
		drawContents(graphics, mouseX, mouseY);
		ItemStack hovered = drawEntry(graphics, mouseX, mouseY);
		if (!hovered.isEmpty()) graphics.setTooltipForNextFrame(font, hovered, mouseX, mouseY);
	}

	private void centered(GuiGraphicsExtractor graphics, Component text, int pageX, int y, int color) {
		graphics.text(font, text, pageX + (PAGE_WIDTH - font.width(text)) / 2, y, color, false);
	}

	private void drawContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int x = left + LEFT_PAGE_X;
		centered(graphics, title, x, top + 14, INK);
		List<EncyclopediaPage> pages = EncyclopediaPage.PAGES;
		int bossStart = firstBoss();
		if (bossStart < pages.size()) {
			centered(graphics, Component.translatable("gui.eam.encyclopedia.bosses"), x, top + rowY(bossStart) - HEADER_GAP + 1, FADED);
		}
		int hoveredRow = rowAt(mouseX, mouseY);
		for (int i = 0; i < pages.size(); i++) {
			EncyclopediaPage page = pages.get(i);
			int y = top + rowY(i);
			if (i == entry) graphics.fill(x - 2, y - 2, x + PAGE_WIDTH + 2, y + ROW - 2, CHOSEN_SHADE);
			String prefix = page.kind() == EncyclopediaPage.Kind.BOSS
					? Component.translatable(page.translationKey("order")).getString() + " "
					: "";
			int markWidth = reached(page) ? font.width("✔") + 2 : 0;
			String name = font.plainSubstrByWidth(prefix + Component.translatable(page.translationKey("name")).getString(),
					PAGE_WIDTH - markWidth);
			Component label = Component.literal(name).withStyle(Style.EMPTY.withUnderlined(i == hoveredRow && i != entry));
			graphics.text(font, label, x, y, i == entry ? CHOSEN : INK, false);
			if (markWidth > 0) graphics.text(font, "✔", x + PAGE_WIDTH - markWidth + 2, y, DONE, false);
		}
	}

	/** Draws the open entry's sheet on the right page and returns the icon under the mouse, if any. */
	private ItemStack drawEntry(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		EncyclopediaPage page = EncyclopediaPage.PAGES.get(entry);
		int x = left + RIGHT_PAGE_X;
		List<List<Line>> pageSheets = sheets.get(entry);
		if (pageSheets.size() > 1) {
			Component indicator = Component.translatable("book.pageIndicator", sheet + 1, pageSheets.size());
			graphics.text(font, indicator, x + PAGE_WIDTH - font.width(indicator), top + 12, FADED, false);
		}
		ItemStack hovered = ItemStack.EMPTY;
		if (sheet == 0) {
			Component name = Component.translatable(page.translationKey("name"));
			if (reached(page)) name = Component.empty().append(name).append(Component.literal(" ✔").withColor(DONE));
			centered(graphics, name, x, top + 24, INK);
			centered(graphics, Component.translatable(page.translationKey("subtitle")), x, top + 34, FADED);
			List<ItemStack> stacks = icons.get(entry);
			int rowX = x + (PAGE_WIDTH - stacks.size() * SLOT) / 2;
			for (int i = 0; i < stacks.size(); i++) {
				int slotX = rowX + i * SLOT;
				int slotY = top + ICON_Y;
				graphics.fill(slotX, slotY, slotX + SLOT - 1, slotY + SLOT - 1, SLOT_SHADE);
				graphics.item(stacks.get(i), slotX, slotY);
				if (mouseX >= slotX && mouseX < slotX + SLOT - 1 && mouseY >= slotY && mouseY < slotY + SLOT - 1) {
					hovered = stacks.get(i);
				}
			}
		}
		for (Line line : pageSheets.get(sheet)) {
			graphics.text(font, line.text(), x, top + line.y(), INK, false);
		}
		return hovered;
	}
}
