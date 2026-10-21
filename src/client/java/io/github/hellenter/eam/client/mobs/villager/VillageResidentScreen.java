package io.github.hellenter.eam.client.mobs.villager;

import io.github.hellenter.eam.mobs.vanilla.villager.VillageListPayload;
import io.github.hellenter.eam.mobs.vanilla.villager.VillageResidentPayload;
import io.github.hellenter.eam.mobs.vanilla.villager.VillageResidentRequestPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * A villager's page in the village menu, opened by a click on his row: his name, his figure, armor, hands (put-away gear too) and work item,
 * his level and experience, health, job site, bed and locked chest, his inventory and what is in his chest (for the
 * elder, the village store's nearest chest). It asks the server again every second, so it stays current; Escape and
 * the back button return to the list.
 */
public class VillageResidentScreen extends Screen {
	private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
	private static final int TEXT = 0xFF404040;
	private static final int MUTED = 0xFF707070;
	private static final int FIGURE_BACKGROUND = 0xFF000000;
	private static final int XP_BACK = 0xFF555555;
	private static final int XP_FILL = 0xFF7FB238;
	private static final int WIDTH = 304;
	private static final int SLOT = 18;
	private static final int GEAR_TOP = 18;
	private static final int ARMOR_X = 8;
	private static final int FIGURE_X0 = ARMOR_X + SLOT;
	private static final int FIGURE_X1 = FIGURE_X0 + 60;
	private static final int HANDS_Y = GEAR_TOP + 4 * SLOT + 2;
	private static final int INFO_X = FIGURE_X1 + 10;
	private static final int LINE = 11;
	private static final int INVENTORY_LABEL_Y = HANDS_Y + SLOT + 6;
	private static final int INVENTORY_Y = INVENTORY_LABEL_Y + 10;
	private static final int CHEST_LABEL_Y = INVENTORY_Y + SLOT + 6;
	private static final int CHEST_Y = CHEST_LABEL_Y + 10;
	private static final int CHEST_COLUMNS = 9;
	private static final int HEIGHT = CHEST_Y + 3 * SLOT + 8;
	private static final int REFRESH_TICKS = 20;

	private final VillageScreen list;
	private VillageResidentPayload page;
	private int left;
	private int top;
	private int ticks;
	private ItemStack hovered = ItemStack.EMPTY;

	VillageResidentScreen(VillageScreen list, VillageResidentPayload page) {
		super(VillageScreen.name(page.resident()));
		this.list = list;
		this.page = page;
	}

	/** Opens a villager's page when the server answers a click in the list, or refreshes the open one. */
	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(VillageResidentPayload.TYPE, (payload, context) -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.gui.screen() instanceof VillageResidentScreen screen && screen.page.elderId() == payload.elderId()) {
				if (screen.page.resident().id() == payload.resident().id()) {
					screen.page = payload;
				} else {
					minecraft.gui.setScreen(new VillageResidentScreen(screen.list, payload));
				}
			} else if (minecraft.gui.screen() instanceof VillageScreen screen && screen.elderId() == payload.elderId()) {
				minecraft.gui.setScreen(new VillageResidentScreen(screen, payload));
			}
		});
	}

	@Override
	protected void init() {
		left = (width - WIDTH) / 2;
		top = (height - HEIGHT) / 2;
		int x = left + ARMOR_X + CHEST_COLUMNS * SLOT + 8;
		addRenderableWidget(Button.builder(Component.translatable("gui.eam.village.back"), pressed -> onClose())
				.bounds(x, top + CHEST_Y + 3 * SLOT - 20, left + WIDTH - 8 - x, 20)
				.build());
	}

	@Override
	public void tick() {
		if (++ticks % REFRESH_TICKS == 0) {
			ClientPlayNetworking.send(new VillageResidentRequestPayload(page.elderId(), page.resident().id()));
		}
	}

	@Override
	public void onClose() {
		minecraft.gui.setScreen(list);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		VillageScreen.frame(graphics, left, top, left + WIDTH, top + HEIGHT);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		hovered = ItemStack.EMPTY;
		VillageListPayload.Resident resident = page.resident();
		graphics.text(font, VillageScreen.name(resident), left + 8, top + 6, TEXT, false);
		List<ItemStack> gear = resident.gear();
		for (int i = 0; i < 4; i++) slot(graphics, gear.get(i), left + ARMOR_X, top + GEAR_TOP + i * SLOT, mouseX, mouseY);
		slot(graphics, gear.get(4), left + ARMOR_X, top + HANDS_Y, mouseX, mouseY);
		slot(graphics, gear.get(5), left + ARMOR_X + SLOT, top + HANDS_Y, mouseX, mouseY);
		slot(graphics, page.workItem(), left + ARMOR_X + 3 * SLOT, top + HANDS_Y, mouseX, mouseY);
		int fx0 = left + FIGURE_X0;
		int fy0 = top + GEAR_TOP;
		int fx1 = left + FIGURE_X1;
		int fy1 = fy0 + 4 * SLOT;
		graphics.fill(fx0, fy0, fx1, fy1, FIGURE_BACKGROUND);
		Entity entity = minecraft.level == null ? null : minecraft.level.getEntity(resident.id());
		if (entity instanceof LivingEntity living) {
			InventoryScreen.extractEntityInInventoryFollowsMouse(graphics, fx0, fy0, fx1, fy1, 28, 0.0625F, mouseX, mouseY, living);
		}
		info(graphics, resident);
		graphics.text(font, Component.translatable("gui.eam.village.inventory"), left + 8, top + INVENTORY_LABEL_Y, TEXT, false);
		for (int i = 0; i < page.inventory().size(); i++) {
			slot(graphics, page.inventory().get(i), left + ARMOR_X + i * SLOT, top + INVENTORY_Y, mouseX, mouseY);
		}
		Component chestLabel = page.chest().isEmpty() ? Component.translatable("gui.eam.village.no_chest")
				: Component.translatable(resident.role() == VillageListPayload.Role.ELDER ? "gui.eam.village.store_items" : "gui.eam.village.chest_items");
		graphics.text(font, chestLabel, left + 8, top + CHEST_LABEL_Y, TEXT, false);
		for (int i = 0; i < 3 * CHEST_COLUMNS; i++) {
			ItemStack stack = i < page.chestItems().size() ? page.chestItems().get(i) : ItemStack.EMPTY;
			slot(graphics, stack, left + ARMOR_X + i % CHEST_COLUMNS * SLOT, top + CHEST_Y + i / CHEST_COLUMNS * SLOT, mouseX, mouseY);
		}
		super.extractRenderState(graphics, mouseX, mouseY, a);
		if (!hovered.isEmpty()) graphics.setTooltipForNextFrame(font, hovered, mouseX, mouseY);
	}

	/** Level, experience with its bar, health, job site, bed and chest, beside the figure. */
	private void info(GuiGraphicsExtractor graphics, VillageListPayload.Resident resident) {
		int x = left + INFO_X;
		int y = top + GEAR_TOP + 1;
		boolean child = resident.role() == VillageListPayload.Role.CHILD;
		if (!child) {
			int level = resident.level();
			graphics.text(font, Component.translatable("gui.eam.village.level", level, Component.translatable("merchant.level." + level)),
					x, y, TEXT, false);
			y += LINE;
			if (VillagerData.canLevelUp(level)) {
				int min = VillagerData.getMinXpPerLevel(level);
				int max = VillagerData.getMaxXpPerLevel(level);
				Component xp = Component.translatable("gui.eam.village.xp", page.xp(), max);
				graphics.text(font, xp, x, y, MUTED, false);
				int barX = x + font.width(xp) + 4;
				int barWidth = left + WIDTH - 8 - barX;
				graphics.fill(barX, y + 2, barX + barWidth, y + 6, XP_BACK);
				float progress = max > min ? Math.clamp((page.xp() - min) / (float) (max - min), 0.0F, 1.0F) : 1.0F;
				graphics.fill(barX, y + 2, barX + Math.round(barWidth * progress), y + 6, XP_FILL);
			} else {
				graphics.text(font, Component.translatable("gui.eam.village.xp_max", page.xp()), x, y, MUTED, false);
			}
			y += LINE;
		}
		VillageScreen.health(graphics, font, resident, x, y - 1);
		y += LINE + 4;
		if (!child) {
			place(graphics, "gui.eam.village.job_site", page.jobSite(), x, y);
			y += LINE;
		}
		place(graphics, "gui.eam.village.home", page.home(), x, y);
		y += LINE;
		if (!child) {
			place(graphics, resident.role() == VillageListPayload.Role.ELDER ? "gui.eam.village.store" : "gui.eam.village.chest",
					page.chest(), x, y);
		}
	}

	private void place(GuiGraphicsExtractor graphics, String key, Optional<BlockPos> pos, int x, int y) {
		Component where = pos.map(p -> Component.translatable("gui.eam.village.at", p.getX(), p.getY(), p.getZ()))
				.orElseGet(() -> Component.translatable("gui.eam.village.none"));
		graphics.text(font, Component.translatable(key, where), x, y, pos.isPresent() ? TEXT : MUTED, false);
	}

	private void slot(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y, int mouseX, int mouseY) {
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, x, y, SLOT, SLOT);
		if (stack.isEmpty()) return;
		graphics.item(stack, x + 1, y + 1);
		graphics.itemDecorations(font, stack, x + 1, y + 1);
		if (mouseX >= x && mouseX < x + SLOT && mouseY >= y && mouseY < y + SLOT) hovered = stack;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
