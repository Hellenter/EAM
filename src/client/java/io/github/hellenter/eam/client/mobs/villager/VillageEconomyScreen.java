package io.github.hellenter.eam.client.mobs.villager;

import io.github.hellenter.eam.mobs.vanilla.villager.VillageEconomyPayload;
import io.github.hellenter.eam.mobs.vanilla.villager.VillageOrderPayload;
import io.github.hellenter.eam.mobs.vanilla.villager.VillageResource;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
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
	private static final int ROW_DARK = 0xFFB4B4B4;
	private static final int TEXT = 0xFF404040;
	private static final int MUTED = 0xFF707070;
	private static final int GOOD = 0xFF2E7D32;
	private static final int BAD = 0xFFA02020;
	private static final int WIDTH = 304;
	private static final int RESOURCES_TOP = 20;
	private static final int STATUS_TOP = 58;
	private static final int ORDERS_TOP = 128;
	private static final int ROW = 22;
	private static final int ROWS = 6;
	private static final int HEIGHT = ORDERS_TOP + ROWS * ROW + 8;
	private static final int COLUMN = 58;
	private static final int BUTTON_WIDTH = 64;
	/** Item stacks are made with the screen, never when the class loads: components are not bound that early. */
	private final ItemStack[] icons = {
			new ItemStack(Items.OAK_LOG), new ItemStack(Items.IRON_INGOT), new ItemStack(Items.COBBLESTONE),
			new ItemStack(Items.EMERALD), new ItemStack(Items.BREAD)};

	private VillageEconomyPayload economy;
	private int scroll;
	private int left;
	private int top;

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
		left = (width - WIDTH) / 2;
		top = (height - HEIGHT) / 2;
		List<VillageEconomyPayload.OrderView> orders = economy.orders();
		scroll = Mth.clamp(scroll, 0, Math.max(0, orders.size() - ROWS));
		for (int row = 0; row < ROWS && scroll + row < orders.size(); row++) {
			VillageEconomyPayload.OrderView order = orders.get(scroll + row);
			boolean goal = economy.goal().filter(order.id()::equals).isPresent();
			boolean affordable = affords(order);
			boolean take = order.ready() > 0;
			String label = take ? "take" : goal ? "cancel" : !order.available() ? "unavailable" : affordable ? "order" : "save_up";
			Component text = take ? Component.translatable("gui.eam.village.economy.take", order.ready())
					: Component.translatable("gui.eam.village.economy." + label);
			Button button = Button.builder(text, pressed -> {
						ClientPlayNetworking.send(new VillageOrderPayload(economy.elderId(), goal && !take ? "" : order.id(), take));
						pressed.active = false;
					})
					.bounds(left + WIDTH - 8 - BUTTON_WIDTH, top + ORDERS_TOP + row * ROW + 1, BUTTON_WIDTH, 20)
					.build();
			button.active = take || goal || order.available();
			addRenderableWidget(button);
		}
	}

	private boolean affords(VillageEconomyPayload.OrderView order) {
		for (int i = 0; i < order.cost().size(); i++) {
			if (economy.stock().get(i) + 1.0E-6 < order.cost().get(i)) return false;
		}
		return true;
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		int next = Mth.clamp(scroll - (int) Math.signum(scrollY), 0, Math.max(0, economy.orders().size() - ROWS));
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
		int y1 = top + HEIGHT;
		graphics.fill(x0 + 1, y0, x1 - 1, y1, OUTLINE);
		graphics.fill(x0, y0 + 1, x1, y1 - 1, OUTLINE);
		graphics.fill(x0 + 1, y0 + 1, x1 - 2, y1 - 2, LIGHT);
		graphics.fill(x0 + 3, y0 + 3, x1 - 1, y1 - 1, SHADOW);
		graphics.fill(x0 + 3, y0 + 3, x1 - 3, y1 - 3, BODY);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		graphics.text(font, title, left + 8, top + 6, TEXT, false);
		Component tooltip = null;
		VillageResource[] resources = VillageResource.values();
		for (int i = 0; i < resources.length; i++) {
			int x = left + 8 + i * COLUMN;
			int y = top + RESOURCES_TOP;
			graphics.item(icons[i], x, y);
			graphics.text(font, amount(economy.stock().get(i)), x + 19, y + 4, TEXT, false);
			double income = economy.income().get(i);
			String signed = (income > 0 ? "+" : "") + amount(income);
			graphics.text(font, signed, x, y + 20, income < 0 ? BAD : income > 0 ? GOOD : MUTED, false);
			if (mouseX >= x && mouseX < x + COLUMN - 2 && mouseY >= y && mouseY < y + 30) {
				tooltip = Component.translatable("gui.eam.village.resource." + resources[i].getSerializedName()).append("\n")
						.append(Component.translatable("gui.eam.village.economy.income", signed));
			}
		}
		VillageEconomyPayload.Status status = economy.status();
		int y = top + STATUS_TOP;
		graphics.text(font, Component.translatable("gui.eam.village.economy.beds", status.villagers(), status.beds()), left + 8, y, TEXT, false);
		graphics.text(font, Component.translatable("gui.eam.village.economy.guards", status.guards(), status.guardsNeeded()), left + 160, y, TEXT, false);
		graphics.text(font, Component.translatable("gui.eam.village.economy.golems", status.golems(), status.golemsAllowed()), left + 160, y + 11, TEXT, false);
		List<Component> missing = missing(status);
		if (missing.isEmpty()) {
			graphics.text(font, Component.translatable("gui.eam.village.economy.all_good"), left + 8, y + 11, GOOD, false);
		} else {
			graphics.text(font, Component.translatable("gui.eam.village.economy.missing"), left + 8, y + 11, BAD, false);
			for (int i = 0; i < missing.size(); i++) graphics.text(font, missing.get(i), left + 14, y + 22 + i * 10, BAD, false);
		}
		economy.goal().ifPresent(goal -> graphics.text(font, Component.translatable("gui.eam.village.economy.goal",
				Component.translatable("gui.eam.village.order." + goal)), left + 8, top + ORDERS_TOP - 12, MUTED, false));
		List<VillageEconomyPayload.OrderView> orders = economy.orders();
		for (int row = 0; row < ROWS && scroll + row < orders.size(); row++) {
			VillageEconomyPayload.OrderView order = orders.get(scroll + row);
			int rowY = top + ORDERS_TOP + row * ROW;
			if (row % 2 == 1) graphics.fill(left + 5, rowY, left + WIDTH - 5, rowY + ROW, ROW_DARK);
			graphics.text(font, Component.translatable("gui.eam.village.order." + order.id()), left + 8, rowY + 7, TEXT, false);
			int x = left + 104;
			for (int i = 0; i < order.cost().size(); i++) {
				double cost = order.cost().get(i);
				if (cost <= 0) continue;
				graphics.item(icons[i], x, rowY + 3);
				boolean enough = economy.stock().get(i) + 1.0E-6 >= cost;
				String text = amount(cost);
				graphics.text(font, text, x + 17, rowY + 7, enough ? TEXT : BAD, false);
				x += 19 + font.width(text) + 4;
			}
		}
		super.extractRenderState(graphics, mouseX, mouseY, a);
		if (tooltip != null) graphics.setTooltipForNextFrame(font, font.split(tooltip, 200), mouseX, mouseY);
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

	/** One decimal below ten, whole numbers above. */
	private static String amount(double value) {
		return Math.abs(value) < 10 ? String.format(Locale.ROOT, "%.1f", value) : Long.toString(Math.round(value));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
