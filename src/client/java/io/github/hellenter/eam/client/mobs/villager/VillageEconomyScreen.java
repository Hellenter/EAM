package io.github.hellenter.eam.client.mobs.villager;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.hellenter.eam.mobs.vanilla.villager.VillageDonatePayload;
import io.github.hellenter.eam.mobs.vanilla.villager.VillageEconomyPayload;
import io.github.hellenter.eam.mobs.vanilla.villager.VillageOrderPayload;
import io.github.hellenter.eam.mobs.vanilla.villager.VillageResource;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The elder's upgrades menu: what the village has of each resource and gathers each half a day, how it stands with
 * beds, guards and golems and what it lacks, and the orders the stock pays for, each with a button to order it now or
 * have the village save up for it.
 */
public class VillageEconomyScreen extends Screen {
	private static final int OUTLINE = 0xFF000000;
	private static final int LIGHT = 0xFFFFFFFF;
	private static final int SHADOW = 0xFF555555;
	private static final int BODY = 0xFFC6C6C6;
	private static final int HEADER = 0xFF7A6A55;
	private static final int HEADER_DARK = 0xFF5C4E3D;
	private static final int CARD = 0xFFB9B9B9;
	private static final int INSET_DARK = 0xFF8A8A8A;
	private static final int ROW_LIGHT = 0xFFCECECE;
	private static final int ROW_DARK = 0xFFBDBDBD;
	private static final int GOAL = 0xFFC9A227;
	private static final int TEXT = 0xFF3A3A3A;
	private static final int MUTED = 0xFF6E6E6E;
	private static final int GOOD = 0xFF2E7D32;
	private static final int BAD = 0xFFA02020;
	private static final int BAD_BOX = 0xFFD9B8B0;
	private static final int GOOD_BOX = 0xFFB9CDB0;
	private static final int TITLE = 0xFFF4E6C8;
	private static final int WIDTH = 336;
	private static final int PAD = 8;
	private static final int HEADER_HEIGHT = 18;
	private static final int RESOURCES_TOP = 24;
	private static final int CARD_WIDTH = 60;
	private static final int CARD_GAP = 4;
	private static final int CARD_HEIGHT = 26;
	private static final int STATUS_TOP = 58;
	private static final int NEEDS_TOP = 82;
	private static final int ORDERS_LABEL = 130;
	private static final int ORDERS_TOP = 142;
	private static final int ROW = 24;
	private static final int MAX_ROWS = 6;
	private static final int MIN_ROWS = 2;
	private static final int NAME_WIDTH = 96;
	private static final int COSTS_LEFT = 108;
	private static final int COSTS_WIDTH = 160;
	private static final int BUTTON_WIDTH = 60;
	private static final int CARD_HOVER = 0xFFCFCFCF;

	/** The items that stand for the resources, in {@link VillageResource} order. */
	private final ItemStack[] icons = {
			new ItemStack(Items.OAK_LOG), new ItemStack(Items.IRON_INGOT), new ItemStack(Items.COBBLESTONE),
			new ItemStack(Items.EMERALD), new ItemStack(Items.BREAD)};
	private VillageEconomyPayload economy;
	private int scroll;
	private int left;
	private int top;
	/** As many order rows as fit the window, up to six; the rest scroll. */
	private int rows = MAX_ROWS;

	public VillageEconomyScreen(VillageEconomyPayload economy) {
		super(Component.translatable("gui.eam.village.economy.title"));
		this.economy = economy;
	}

	/** Opens the menu when the server sends the economy, or refreshes it when it is open on the same elder. */
	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(VillageEconomyPayload.TYPE, (payload, context) -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.gui.screen() instanceof VillageEconomyScreen screen && screen.economy.elderId() == payload.elderId()) {
				screen.economy = payload;
				screen.rebuildWidgets();
			} else {
				minecraft.gui.setScreen(new VillageEconomyScreen(payload));
			}
		});
	}

	@Override
	protected void init() {
		rows = Mth.clamp((height - 8 - ORDERS_TOP - PAD) / ROW, MIN_ROWS, MAX_ROWS);
		left = (width - WIDTH) / 2;
		top = Math.max(4, (height - panelHeight()) / 2);
		List<VillageEconomyPayload.OrderView> orders = economy.orders();
		scroll = Mth.clamp(scroll, 0, maxScroll());
		for (int row = 0; row < rows && scroll + row < orders.size(); row++) {
			VillageEconomyPayload.OrderView order = orders.get(scroll + row);
			boolean goal = isGoal(order);
			boolean affordable = affords(order);
			boolean take = order.ready() > 0;
			String label = take ? "take" : goal ? "cancel" : !order.available() ? "unavailable" : affordable ? "order" : "save_up";
			Component text = take ? Component.translatable("gui.eam.village.economy.take", order.ready())
					: Component.translatable("gui.eam.village.economy." + label);
			Button button = Button.builder(text, pressed -> {
						ClientPlayNetworking.send(new VillageOrderPayload(economy.elderId(), goal && !take ? "" : order.id(), take));
						pressed.active = false;
					})
					.bounds(left + WIDTH - PAD - BUTTON_WIDTH, top + ORDERS_TOP + row * ROW + 2, BUTTON_WIDTH, 20)
					.build();
			button.active = take || goal || order.available();
			addRenderableWidget(button);
		}
	}

	private int panelHeight() {
		return ORDERS_TOP + rows * ROW + PAD;
	}

	private boolean isGoal(VillageEconomyPayload.OrderView order) {
		return economy.goal().filter(order.id()::equals).isPresent();
	}

	private boolean affords(VillageEconomyPayload.OrderView order) {
		for (int i = 0; i < order.cost().size(); i++) {
			if (economy.stock().get(i) + 1.0E-6 < order.cost().get(i)) return false;
		}
		return true;
	}

	private int maxScroll() {
		return Math.max(0, economy.orders().size() - rows);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		int next = Mth.clamp(scroll - (int) Math.signum(scrollY), 0, maxScroll());
		if (next != scroll) {
			scroll = next;
			rebuildWidgets();
		}
		return true;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		int x0 = left;
		int y0 = top;
		int x1 = left + WIDTH;
		int y1 = top + panelHeight();
		graphics.fill(x0 + 1, y0, x1 - 1, y1, OUTLINE);
		graphics.fill(x0, y0 + 1, x1, y1 - 1, OUTLINE);
		graphics.fill(x0 + 1, y0 + 1, x1 - 2, y1 - 2, LIGHT);
		graphics.fill(x0 + 3, y0 + 3, x1 - 1, y1 - 1, SHADOW);
		graphics.fill(x0 + 3, y0 + 3, x1 - 3, y1 - 3, BODY);
		graphics.fill(x0 + 3, y0 + 3, x1 - 3, y0 + HEADER_HEIGHT + 2, HEADER);
		graphics.fill(x0 + 3, y0 + HEADER_HEIGHT + 2, x1 - 3, y0 + HEADER_HEIGHT + 3, HEADER_DARK);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		graphics.text(font, title, left + PAD, top + 8, TITLE, true);
		Component tooltip = resources(graphics, mouseX, mouseY);
		status(graphics);
		orders(graphics);
		super.extractRenderState(graphics, mouseX, mouseY, a);
		if (tooltip != null) graphics.setTooltipForNextFrame(font, font.split(tooltip, 200), mouseX, mouseY);
	}

	/**
	 * A sunken card per resource: its icon, the stock and what it gains or loses each half a day. A click on it gives the
	 * village the player's items of that resource; a green plus marks the cards he has something for.
	 */
	private Component resources(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		Component tooltip = null;
		VillageResource[] resources = VillageResource.values();
		for (int i = 0; i < resources.length; i++) {
			int x = left + PAD + i * (CARD_WIDTH + CARD_GAP);
			int y = top + RESOURCES_TOP;
			boolean hover = card(mouseX, mouseY) == i;
			double donatable = economy.donatable().get(i);
			inset(graphics, x, y, x + CARD_WIDTH, y + CARD_HEIGHT, hover ? CARD_HOVER : CARD);
			icon(graphics, i, x + 4, y + 5);
			if (donatable > 0) graphics.text(font, "+", x + CARD_WIDTH - 8, y + 2, GOOD, false);
			graphics.text(font, amount(economy.stock().get(i)), x + 23, y + 4, TEXT, false);
			double income = economy.income().get(i);
			String signed = (income > 0 ? "+" : "") + amount(income);
			graphics.text(font, signed, x + 23, y + 15, income < 0 ? BAD : income > 0 ? GOOD : MUTED, false);
			if (hover) {
				String name = resources[i].getSerializedName();
				tooltip = Component.translatable("gui.eam.village.resource." + name).append("\n")
						.append(Component.translatable("gui.eam.village.economy.income", signed)).append("\n\n")
						.append(Component.translatable("gui.eam.village.donate.accepts." + name)).append("\n")
						.append(donatable > 0 ? Component.translatable("gui.eam.village.donate.have", amount(donatable)).append("\n")
								.append(Component.translatable("gui.eam.village.donate.hint"))
								: Component.translatable("gui.eam.village.donate.none"));
			}
		}
		return tooltip;
	}

	/** Villagers and beds, guards, golems; then a box with what the village lacks, or that it lacks nothing. */
	private void status(GuiGraphicsExtractor graphics) {
		VillageEconomyPayload.Status status = economy.status();
		int y = top + STATUS_TOP;
		int half = left + WIDTH / 2 + 4;
		graphics.text(font, Component.translatable("gui.eam.village.economy.beds", status.villagers(), status.beds()), left + PAD, y,
				status.villagers() < status.beds() ? TEXT : BAD, false);
		graphics.text(font, Component.translatable("gui.eam.village.economy.guards", status.guards(), status.guardsNeeded()), half, y,
				status.guards() < status.guardsNeeded() ? BAD : TEXT, false);
		graphics.text(font, Component.translatable("gui.eam.village.economy.golems", status.golems(), status.golemsAllowed()), half, y + 11,
				TEXT, false);
		List<Component> missing = missing(status);
		int boxTop = top + NEEDS_TOP;
		int lines = missing.isEmpty() ? 0 : missing.size();
		int boxBottom = boxTop + 14 + lines * 10;
		inset(graphics, left + PAD, boxTop, left + WIDTH - PAD, boxBottom, missing.isEmpty() ? GOOD_BOX : BAD_BOX);
		if (missing.isEmpty()) {
			graphics.text(font, Component.translatable("gui.eam.village.economy.all_good"), left + PAD + 5, boxTop + 3, GOOD, false);
			return;
		}
		graphics.text(font, Component.translatable("gui.eam.village.economy.missing"), left + PAD + 5, boxTop + 3, BAD, false);
		for (int i = 0; i < missing.size(); i++) {
			graphics.text(font, Component.literal("• ").append(missing.get(i)), left + PAD + 11, boxTop + 13 + i * 10, BAD, false);
		}
	}

	/** The orders as rows: the name, each resource it costs in a column of its own, red where the stock is short. */
	private void orders(GuiGraphicsExtractor graphics) {
		graphics.text(font, Component.translatable("gui.eam.village.economy.orders"), left + PAD, top + ORDERS_LABEL, TEXT, false);
		economy.goal().ifPresent(goal -> {
			Component text = Component.translatable("gui.eam.village.economy.goal", Component.translatable("gui.eam.village.order." + goal));
			graphics.text(font, text, left + WIDTH - PAD - font.width(text), top + ORDERS_LABEL, MUTED, false);
		});
		List<VillageEconomyPayload.OrderView> orders = economy.orders();
		for (int row = 0; row < rows && scroll + row < orders.size(); row++) {
			VillageEconomyPayload.OrderView order = orders.get(scroll + row);
			int x0 = left + 5;
			int x1 = left + WIDTH - 5;
			int rowY = top + ORDERS_TOP + row * ROW;
			graphics.fill(x0, rowY, x1, rowY + ROW, row % 2 == 0 ? ROW_LIGHT : ROW_DARK);
			if (isGoal(order)) frame(graphics, x0, rowY, x1, rowY + ROW, GOAL);
			String name = Component.translatable("gui.eam.village.order." + order.id()).getString();
			String shown = font.plainSubstrByWidth(name, NAME_WIDTH);
			graphics.text(font, shown.length() < name.length() ? font.plainSubstrByWidth(name, NAME_WIDTH - 6) + "…" : name,
					left + PAD, rowY + 8, order.available() ? TEXT : MUTED, false);
			int costs = 0;
			for (double cost : order.cost()) if (cost > 0) costs++;
			int column = costs == 0 ? 0 : Math.min(40, COSTS_WIDTH / costs);
			int x = left + COSTS_LEFT;
			for (int i = 0; i < order.cost().size(); i++) {
				double cost = order.cost().get(i);
				if (cost <= 0) continue;
				icon(graphics, i, x, rowY + 4);
				boolean enough = economy.stock().get(i) + 1.0E-6 >= cost;
				graphics.text(font, amount(cost), x + 18, rowY + 8, enough ? TEXT : BAD, false);
				x += column;
			}
		}
		if (orders.size() > rows) {
			int trackTop = top + ORDERS_TOP;
			int trackHeight = rows * ROW;
			int thumb = Math.max(12, trackHeight * rows / orders.size());
			int thumbTop = trackTop + (trackHeight - thumb) * scroll / maxScroll();
			graphics.fill(left + WIDTH - 4, trackTop, left + WIDTH - 3, trackTop + trackHeight, INSET_DARK);
			graphics.fill(left + WIDTH - 5, thumbTop, left + WIDTH - 2, thumbTop + thumb, SHADOW);
		}
	}

	private void icon(GuiGraphicsExtractor graphics, int resource, int x, int y) {
		graphics.item(icons[resource], x, y);
	}

	/** The resource card under the mouse, or -1. */
	private int card(double mouseX, double mouseY) {
		int y = top + RESOURCES_TOP;
		if (mouseY < y || mouseY >= y + CARD_HEIGHT) return -1;
		for (int i = 0; i < icons.length; i++) {
			int x = left + PAD + i * (CARD_WIDTH + CARD_GAP);
			if (mouseX >= x && mouseX < x + CARD_WIDTH) return i;
		}
		return -1;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		int card = card(event.x(), event.y());
		if (card >= 0 && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
			if (economy.donatable().get(card) > 0) {
				ClientPlayNetworking.send(new VillageDonatePayload(economy.elderId(), VillageResource.values()[card], event.hasShiftDown()));
				AbstractWidget.playButtonClickSound(Minecraft.getInstance().getSoundManager());
			}
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	/** A sunken box: dark top and left edges, light bottom and right. */
	private static void inset(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int fill) {
		graphics.fill(x0, y0, x1, y1, INSET_DARK);
		graphics.fill(x0 + 1, y0 + 1, x1, y1, LIGHT);
		graphics.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, fill);
	}

	private static void frame(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int color) {
		graphics.fill(x0, y0, x1, y0 + 1, color);
		graphics.fill(x0, y1 - 1, x1, y1, color);
		graphics.fill(x0, y0, x0 + 1, y1, color);
		graphics.fill(x1 - 1, y0, x1, y1, color);
	}

	/** What keeps the village from growing: beds, provisions for a child, provisions to live on. */
	private List<Component> missing(VillageEconomyPayload.Status status) {
		List<Component> missing = new ArrayList<>();
		if (status.villagers() >= status.beds()) missing.add(Component.translatable("gui.eam.village.economy.no_beds"));
		int provisions = VillageResource.PROVISIONS.ordinal();
		if (economy.stock().get(provisions) < status.breedCost()) {
			missing.add(Component.translatable("gui.eam.village.economy.no_food", amount(status.breedCost())));
		}
		if (economy.income().get(provisions) < 0) missing.add(Component.translatable("gui.eam.village.economy.hunger"));
		return missing;
	}

	/** Whole numbers as they are, else one decimal below ten and rounded above. */
	private static String amount(double value) {
		if (Math.abs(value - Math.rint(value)) < 0.05) return Long.toString(Math.round(value));
		return Math.abs(value) < 10 ? String.format(Locale.ROOT, "%.1f", value) : Long.toString(Math.round(value));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
