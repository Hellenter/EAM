package io.github.hellenter.eam.client.mobs.villager;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.hellenter.eam.mobs.vanilla.villager.VillageEconomyOpenPayload;
import io.github.hellenter.eam.mobs.vanilla.villager.VillageGuardPayload;
import io.github.hellenter.eam.mobs.vanilla.villager.VillageListPayload;
import io.github.hellenter.eam.mobs.vanilla.villager.VillageResidentRequestPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The village menu the elder opens: a window like a chest's listing every villager within his reach, a row each, with
 * his name, profession and level, a bar of his health, his armor and what he holds, and a button that makes a grown
 * villager a guard or a guard an ordinary villager again. The wheel scrolls the rows; a click on a row opens that
 * villager's page with everything he has, {@link VillageResidentScreen}.
 */
public class VillageScreen extends Screen {
	private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
	private static final int OUTLINE = 0xFF000000;
	private static final int LIGHT = 0xFFFFFFFF;
	private static final int SHADOW = 0xFF555555;
	private static final int BODY = 0xFFC6C6C6;
	private static final int ROW_DARK = 0xFFB4B4B4;
	private static final int ROW_HOVER = 0xFFD8D8D8;
	private static final int TEXT = 0xFF404040;
	private static final int MUTED = 0xFF707070;
	private static final Identifier HEART_SPRITE = Identifier.withDefaultNamespace("hud/heart/full");
	private static final int BAR_BACK = 0xFF3A1A1A;
	private static final int BAR_HIGH = 0xFF4E9A2E;
	private static final int BAR_MID = 0xFFC8961E;
	private static final int BAR_LOW = 0xFFB82828;
	static final int HEALTH_WIDTH = 9 + 2 + 60;
	private static final int WIDTH = 440;
	private static final int ROW = 24;
	private static final int ROWS = 8;
	private static final int LIST_TOP = 18;
	private static final int HINT_Y = LIST_TOP + ROWS * ROW + 4;
	private static final int HEIGHT = HINT_Y + 14;
	private static final int TEXT_X = 8;
	private static final int TEXT_WIDTH = 170;
	private static final int HEALTH_X = TEXT_X + TEXT_WIDTH + 4;
	private static final int GEAR_X = HEALTH_X + HEALTH_WIDTH + 6;
	private static final int SLOT = 18;
	private static final int BUTTON_X = GEAR_X + 6 * SLOT + 4;
	private static final int BUTTON_WIDTH = WIDTH - BUTTON_X - 7;

	private VillageListPayload village;
	private int scroll;
	private int left;
	private int top;

	public VillageScreen(VillageListPayload village) {
		super(Component.translatable("gui.eam.village.title"));
		this.village = village;
	}

	/** Opens the menu when the server sends the village, or refreshes it when it is open on the same elder. */
	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(VillageListPayload.TYPE, (payload, context) -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.gui.screen() instanceof VillageScreen screen && screen.village.elderId() == payload.elderId()) {
				screen.village = payload;
				screen.rebuildWidgets();
			} else {
				minecraft.gui.setScreen(new VillageScreen(payload));
			}
		});
	}

	@Override
	protected void init() {
		left = (width - WIDTH) / 2;
		top = (height - HEIGHT) / 2;
		scroll = Mth.clamp(scroll, 0, maxScroll());
		addUpgradesButton();
		List<VillageListPayload.Resident> residents = village.villagers();
		for (int row = 0; row < ROWS && scroll + row < residents.size(); row++) {
			VillageListPayload.Resident resident = residents.get(scroll + row);
			boolean guard = resident.role() == VillageListPayload.Role.GUARD;
			if (resident.role() == VillageListPayload.Role.CHILD || resident.role() == VillageListPayload.Role.ELDER) continue;
			Component label = Component.translatable(guard ? "gui.eam.village.dismiss_guard" : "gui.eam.village.make_guard");
			Button button = Button.builder(label, pressed -> {
						ClientPlayNetworking.send(new VillageGuardPayload(village.elderId(), resident.id(), !guard));
						pressed.active = false;
					})
					.bounds(left + BUTTON_X, top + LIST_TOP + row * ROW + 2, BUTTON_WIDTH, 20)
					.build();
			button.active = village.guards();
			addRenderableWidget(button);
		}
	}

	/** The upgrades button under the window: the elder's resources and orders. */
	private void addUpgradesButton() {
		addRenderableWidget(Button.builder(Component.translatable("gui.eam.village.upgrades"),
						pressed -> ClientPlayNetworking.send(new VillageEconomyOpenPayload(village.elderId())))
				.bounds(left + WIDTH - 90, top + HEIGHT + 2, 90, 20)
				.build());
	}

	int elderId() {
		return village.elderId();
	}

	/** A left or right click on a row, off its button, asks the server for that villager's page. */
	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) return true;
		int index = rowAt(event.x(), event.y());
		boolean button = event.button() == InputConstants.MOUSE_BUTTON_LEFT || event.button() == InputConstants.MOUSE_BUTTON_RIGHT;
		if (!button || index < 0) return false;
		ClientPlayNetworking.send(new VillageResidentRequestPayload(village.elderId(), village.villagers().get(index).id()));
		return true;
	}

	/** The index of the villager whose row is under the mouse, or -1. */
	private int rowAt(double x, double y) {
		if (x < left + 5 || x >= left + WIDTH - 5 || y < top + LIST_TOP) return -1;
		int row = (int) (y - top - LIST_TOP) / ROW;
		int index = scroll + row;
		return row < ROWS && index < village.villagers().size() ? index : -1;
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

	private int maxScroll() {
		return Math.max(0, village.villagers().size() - ROWS);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		frame(graphics, left, top, left + WIDTH, top + HEIGHT);
	}

	/** A window like a chest's, from {@code x0, y0} to {@code x1, y1}. */
	static void frame(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1) {
		graphics.fill(x0 + 1, y0, x1 - 1, y1, OUTLINE);
		graphics.fill(x0, y0 + 1, x1, y1 - 1, OUTLINE);
		graphics.fill(x0 + 1, y0 + 1, x1 - 2, y1 - 2, LIGHT);
		graphics.fill(x0 + 3, y0 + 3, x1 - 1, y1 - 1, SHADOW);
		graphics.fill(x0 + 3, y0 + 3, x1 - 3, y1 - 3, BODY);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		List<VillageListPayload.Resident> residents = village.villagers();
		Component heading = residents.size() > ROWS
				? Component.translatable("gui.eam.village.scroll", scroll + 1, Math.min(scroll + ROWS, residents.size()), residents.size())
				: Component.translatable("gui.eam.village.count", residents.size());
		graphics.text(font, title, left + TEXT_X, top + 6, TEXT, false);
		graphics.text(font, heading, left + WIDTH - 8 - font.width(heading), top + 6, MUTED, false);
		graphics.text(font, Component.translatable("gui.eam.village.hint"), left + TEXT_X, top + HINT_Y, MUTED, false);
		ItemStack hovered = ItemStack.EMPTY;
		int hoveredRow = rowAt(mouseX, mouseY);
		for (int row = 0; row < ROWS && scroll + row < residents.size(); row++) {
			VillageListPayload.Resident resident = residents.get(scroll + row);
			int y = top + LIST_TOP + row * ROW;
			if (scroll + row == hoveredRow) {
				graphics.fill(left + 5, y, left + WIDTH - 5, y + ROW, ROW_HOVER);
			} else if (row % 2 == 1) {
				graphics.fill(left + 5, y, left + WIDTH - 5, y + ROW, ROW_DARK);
			}
			graphics.text(font, fit(font, resident.name(), TEXT_WIDTH), left + TEXT_X, y + 3, TEXT, false);
			graphics.text(font, fit(font, occupation(resident), TEXT_WIDTH), left + TEXT_X, y + 13, MUTED, false);
			health(graphics, font, resident, left + HEALTH_X, y + 7);
			for (int i = 0; i < resident.gear().size(); i++) {
				int x = left + GEAR_X + i * SLOT;
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, x, y + 3, SLOT, SLOT);
				ItemStack stack = resident.gear().get(i);
				if (stack.isEmpty()) continue;
				graphics.item(stack, x + 1, y + 4);
				graphics.itemDecorations(font, stack, x + 1, y + 4);
				if (mouseX >= x && mouseX < x + SLOT && mouseY >= y + 3 && mouseY < y + 3 + SLOT) hovered = stack;
			}
		}
		super.extractRenderState(graphics, mouseX, mouseY, a);
		if (!hovered.isEmpty()) graphics.setTooltipForNextFrame(font, hovered, mouseX, mouseY);
	}

	/** His name and what he is, for the title of his page. */
	static Component name(VillageListPayload.Resident resident) {
		return Component.translatable("gui.eam.village.named", resident.name(), role(resident));
	}

	/** The elder, a child, or his profession. */
	private static Component role(VillageListPayload.Resident resident) {
		return switch (resident.role()) {
			case ELDER -> Component.translatable("gui.eam.village.elder");
			case CHILD -> Component.translatable("gui.eam.village.child");
			default -> resident.profession();
		};
	}

	/** What he is and his trade level with its name; a child has no level. */
	private static Component occupation(VillageListPayload.Resident resident) {
		if (resident.role() == VillageListPayload.Role.CHILD) return role(resident);
		int level = resident.level();
		return Component.translatable("gui.eam.village.occupation", role(resident), level, Component.translatable("merchant.level." + level));
	}

	/** The text cut to {@code width} pixels, with an ellipsis when it does not fit. */
	private static String fit(Font font, Component text, int width) {
		String plain = text.getString();
		if (font.width(plain) <= width) return plain;
		return font.plainSubstrByWidth(plain, width - font.width("...")) + "...";
	}

	/**
	 * A heart and a bar of his health, {@link #HEALTH_WIDTH} wide, green while he is well, yellow, then red, with his
	 * health and full health written on it.
	 */
	static void health(GuiGraphicsExtractor graphics, Font font, VillageListPayload.Resident resident, int x, int y) {
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HEART_SPRITE, x, y, 9, 9);
		int x0 = x + 11;
		int x1 = x + HEALTH_WIDTH;
		float fraction = resident.maxHealth() > 0 ? Math.clamp(resident.health() / resident.maxHealth(), 0.0F, 1.0F) : 0.0F;
		graphics.fill(x0, y, x1, y + 9, OUTLINE);
		graphics.fill(x0 + 1, y + 1, x1 - 1, y + 8, BAR_BACK);
		int color = fraction > 0.6F ? BAR_HIGH : fraction > 0.3F ? BAR_MID : BAR_LOW;
		graphics.fill(x0 + 1, y + 1, x0 + 1 + Math.round((x1 - x0 - 2) * fraction), y + 8, color);
		String text = Mth.ceil(resident.health()) + "/" + Mth.ceil(resident.maxHealth());
		graphics.text(font, text, (x0 + x1 - font.width(text)) / 2, y + 1, LIGHT, true);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
