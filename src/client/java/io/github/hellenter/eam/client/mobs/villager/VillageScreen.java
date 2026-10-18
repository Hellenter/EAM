package io.github.hellenter.eam.client.mobs.villager;

import io.github.hellenter.eam.mobs.vanilla.villager.VillageGuardPayload;
import io.github.hellenter.eam.mobs.vanilla.villager.VillageListPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The village menu the elder opens: a window like a chest's listing every villager within his reach, a row each, with
 * his profession and level, his health, his armor and what he holds, and a button that makes a grown villager a guard
 * or a guard an ordinary villager again. The wheel scrolls the rows.
 */
public class VillageScreen extends Screen {
	private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
	private static final int OUTLINE = 0xFF000000;
	private static final int LIGHT = 0xFFFFFFFF;
	private static final int SHADOW = 0xFF555555;
	private static final int BODY = 0xFFC6C6C6;
	private static final int ROW_DARK = 0xFFB4B4B4;
	private static final int TEXT = 0xFF404040;
	private static final int MUTED = 0xFF707070;
	private static final int HURT = 0xFFA02020;
	private static final int WIDTH = 304;
	private static final int ROW = 22;
	private static final int ROWS = 8;
	private static final int LIST_TOP = 18;
	private static final int HEIGHT = LIST_TOP + ROWS * ROW + 8;
	private static final int TEXT_X = 8;
	private static final int GEAR_X = 122;
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
					.bounds(left + BUTTON_X, top + LIST_TOP + row * ROW + 1, BUTTON_WIDTH, 20)
					.build();
			button.active = village.guards();
			addRenderableWidget(button);
		}
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
		List<VillageListPayload.Resident> residents = village.villagers();
		Component heading = residents.size() > ROWS
				? Component.translatable("gui.eam.village.scroll", scroll + 1, Math.min(scroll + ROWS, residents.size()), residents.size())
				: Component.translatable("gui.eam.village.count", residents.size());
		graphics.text(font, title, left + TEXT_X, top + 6, TEXT, false);
		graphics.text(font, heading, left + WIDTH - 8 - font.width(heading), top + 6, MUTED, false);
		ItemStack hovered = ItemStack.EMPTY;
		for (int row = 0; row < ROWS && scroll + row < residents.size(); row++) {
			VillageListPayload.Resident resident = residents.get(scroll + row);
			int y = top + LIST_TOP + row * ROW;
			if (row % 2 == 1) graphics.fill(left + 5, y, left + WIDTH - 5, y + ROW, ROW_DARK);
			graphics.text(font, name(resident), left + TEXT_X, y + 2, TEXT, false);
			graphics.text(font, details(resident), left + TEXT_X, y + 12, resident.health() < resident.maxHealth() ? HURT : MUTED, false);
			for (int i = 0; i < resident.gear().size(); i++) {
				int x = left + GEAR_X + i * SLOT;
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, x, y + 2, SLOT, SLOT);
				ItemStack stack = resident.gear().get(i);
				if (stack.isEmpty()) continue;
				graphics.item(stack, x + 1, y + 3);
				graphics.itemDecorations(font, stack, x + 1, y + 3);
				if (mouseX >= x && mouseX < x + SLOT && mouseY >= y + 2 && mouseY < y + 2 + SLOT) hovered = stack;
			}
		}
		super.extractRenderState(graphics, mouseX, mouseY, a);
		if (!hovered.isEmpty()) graphics.setTooltipForNextFrame(font, hovered, mouseX, mouseY);
	}

	/** His profession, with his name if a player gave him one. */
	private static Component name(VillageListPayload.Resident resident) {
		Component role = switch (resident.role()) {
			case ELDER -> Component.translatable("gui.eam.village.elder");
			case CHILD -> Component.translatable("gui.eam.village.child");
			default -> resident.profession();
		};
		return resident.name().map(name -> (Component) Component.translatable("gui.eam.village.named", name, role)).orElse(role);
	}

	/** Level and health. */
	private static Component details(VillageListPayload.Resident resident) {
		String health = Math.round(resident.health()) + "/" + Math.round(resident.maxHealth());
		if (resident.role() == VillageListPayload.Role.CHILD) return Component.translatable("gui.eam.village.health", health);
		return Component.translatable("gui.eam.village.level_health", resident.level(), health);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
